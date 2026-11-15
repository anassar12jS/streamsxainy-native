import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ActivityIndicator, ScrollView, StatusBar, useWindowDimensions } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { WebView as RNWebView } from 'react-native-webview';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import * as ScreenOrientation from 'expo-screen-orientation';
import * as NavigationBar from 'expo-navigation-bar';
import { saveProgress } from '../lib/store';

// react-native-webview's bundled types don't resolve cleanly under this RN
// version's generated types; the component works fine at runtime.
const WebView = RNWebView as unknown as React.ComponentType<any>;

interface Provider { id: string; name: string; build: (p: P) => string }
interface P { type: string; id: string; season: number; episode: number; position: number }

// These embeds rotate ad domains constantly, so a blacklist can't keep up.
// Instead we whitelist: top-frame navigation is only allowed on the provider's
// own domain (+ subdomains). Any redirect to an ad/bookmaker/other site is
// cancelled before it loads. Subresource requests (video CDNs, trackers inside
// iframes) are untouched.
const hostOf = (url: string) => {
  try { return new URL(url).hostname.toLowerCase(); } catch { return ''; }
};

const allowedFor = (embedUrl: string) => {
  const host = hostOf(embedUrl);
  return (url: string) => {
    const h = hostOf(url);
    if (!h) return true; // about:blank, data:, etc.
    if (!host) return true; // couldn't parse embed url — don't break playback
    return h === host || h.endsWith(`.${host}`);
  };
};

const onShouldStartLoad = (allow: (url: string) => boolean) =>
  (req: { url: string; isTopFrame?: boolean }) => {
    if (req.isTopFrame === false) return true;
    return allow(req.url);
  };

// Runs in every frame before page scripts. window.open is how fullscreen-redirect
// ads escape the player; nulling it keeps taps inside the WebView.
// cinemaos.tech/player is a full page — video on top, then its own source
// lists/episode panels below. In a landscape phone WebView that means a tiny
// video with junk underneath. This strips every top-level element that doesn't
// contain the <video>, leaving a clean cinema surface. Retries while the SPA
// hydrates and lazy-mounts the player.
const CLEAN_UI_JS = `
  (function () {
    var tries = 0;
    var timer = setInterval(function () {
      tries++;
      // The real player may be a <video> OR an <iframe> (cross-origin — we
      // can't reach inside, but we CAN resize the frame). Ad iframes are tiny,
      // so pick the LARGEST candidate, and keep polling — players lazy-load.
      var best = null, bestArea = 0, counts = { v: 0, f: 0, c: 0 }, bestSrc = '';
      var pick = function (list, label) {
        for (var i = 0; i < list.length; i++) {
          var el = list[i];
          var a = el.clientWidth * el.clientHeight;
          if (a > bestArea) { bestArea = a; best = el; bestSrc = label; }
        }
      };
      var vids = document.querySelectorAll('video');
      counts.v = vids.length; pick(vids, 'video');
      var frames = document.querySelectorAll('iframe');
      counts.f = frames.length;
      for (var j = 0; j < frames.length; j++) {
        var f = frames[j];
        var fa = f.clientWidth * f.clientHeight;
        if (fa > bestArea) { bestArea = fa; best = f; bestSrc = (f.src || 'iframe').split('/')[2] || 'iframe'; }
      }
      // Custom players (hls.js/shaka) often render into <canvas>, not <video>.
      var canv = document.querySelectorAll('canvas');
      counts.c = canv.length; pick(canv, 'canvas');
      if (window.ReactNativeWebView) {
        var r = best ? best.getBoundingClientRect() : { width: 0, height: 0 };
        var src = '';
        try { src = (best.currentSrc || best.src || '').split('/').slice(0, 4).join('/').slice(0, 60); } catch (_) {}
        // What's actually under the center of the screen? If taps are being
        // eaten, this names the culprit element.
        var hit = '';
        try {
          var h = document.elementFromPoint(window.innerWidth / 2, window.innerHeight / 2);
          if (h) hit = (h.tagName || '') + '.' + String(h.className && h.className.baseVal !== undefined ? h.className.baseVal : h.className || '').slice(0, 40);
        } catch (_) {}
        window.ReactNativeWebView.postMessage(JSON.stringify({
          type: 'dominfo', tag: bestSrc, w: Math.round(r.width), h: Math.round(r.height),
          vw: window.innerWidth, vh: window.innerHeight,
          v: counts.v, f: counts.f, c: counts.c, src: src, hit: hit,
        }));
      }
      if (!best || bestArea < 20000) { // < ~140x140 — not a real player yet
        if (tries > 120) clearInterval(timer);
        return;
      }

      // Re-apply every tick — the host page's framework can wipe inline styles
      // on re-render, so we keep enforcing rather than apply-once.
      // Find the top-level element (direct child of <body>) that contains the
      // player and force it to fill the viewport. We deliberately do NOT prune
      // siblings — the player's own controls/overlays are siblings of the
      // video element, and hiding them kills tap-to-show-controls.
      var el = best;
      while (el.parentElement && el.parentElement !== document.body) {
        el = el.parentElement;
      }
      var top = el.parentElement === document.body ? el : document.body.firstElementChild;

      // Hide only true page-level siblings of the app root (rare), then pin
      // the surviving root to the full viewport.
      if (top && top.parentElement === document.body) {
        for (var k = 0; k < document.body.children.length; k++) {
          var c = document.body.children[k];
          if (c !== top) c.style.display = 'none';
        }
        top.style.cssText += ';position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:2147483647;background:#000;overflow:hidden;';
      }
      best.style.cssText += ';width:100%!important;height:100%!important;object-fit:contain!important;';
      var a2 = best.parentElement;
      while (a2 && a2 !== document.body) {
        a2.style.setProperty('height', '100%', 'important');
        a2.style.setProperty('width', '100%', 'important');
        a2 = a2.parentElement;
      }
      document.body.style.overflow = 'hidden';
      document.documentElement.style.overflow = 'hidden';
    }, 1000);
  })();
  true;
`;

const ANTI_AD_JS = `
  (function () {
    try {
      window.open = function () { return null; };
      // The embed's own fullscreen handler requests orientation changes that
      // fight our native landscape lock — make them no-ops.
      try {
        if (screen.orientation && screen.orientation.lock) {
          screen.orientation.lock = function () { return Promise.resolve(); };
        }
      } catch (_) {}
      var host = location.hostname;
      document.addEventListener('click', function (e) {
        var a = e.target && e.target.closest ? e.target.closest('a[target="_blank"]') : null;
        if (!a) return;
        try {
          var h = new URL(a.href, location.href).hostname;
          // Only block links that leave the provider's own domain — same-site
          // _blank links (like a legit fullscreen player page) still work.
          if (h !== host && !h.endsWith('.' + host)) {
            e.preventDefault(); e.stopPropagation();
          }
        } catch (_) { e.preventDefault(); e.stopPropagation(); }
      }, true);
    } catch (_) {}
  })();
  true;
`;

const PROVIDERS: Provider[] = [
  {
    id: 'cinemaos', name: 'CinemaOS',
    build: p => `https://cinemaos.tech/player/${p.id}${p.type === 'tv' ? `/${p.season}/${p.episode}` : ''}?theme=7c3aed${p.position ? `&progress=${p.position}` : ''}`,
  },
  {
    id: 'vidapi', name: 'VidAPI',
    build: p => `https://vidapi.xyz/embed/${p.type}/${p.id}${p.type === 'tv' ? `/${p.season}/${p.episode}` : ''}`,
  },
  {
    id: 'ishowstream', name: 'iShowStream',
    build: p => {
      const q = new URLSearchParams({ branding: 'streamsxainy', loading: '3', subtitle: 'English', color: '7c3aed' });
      if (p.position) q.set('progress', String(p.position));
      return `https://ishowstream.cc/embed/${p.type}/${p.id}${p.type === 'tv' ? `/${p.season}/${p.episode}` : ''}?${q}`;
    },
  },
  {
    id: 'vidy', name: 'Vidy Prime',
    build: p => {
      const q = new URLSearchParams({ color: '7c3aed', autoplay: 'true' });
      if (p.type === 'tv') { q.set('nextEpisode', 'true'); q.set('episodeSelector', 'true'); q.set('autoplayNextEpisode', 'true'); }
      if (p.position) q.set('progress', String(p.position));
      return `https://vidy.st/${p.type}/${p.id}${p.type === 'tv' ? `/${p.season}/${p.episode}` : ''}?${q}`;
    },
  },
  {
    id: 'vidlink', name: 'VidLink Pro',
    build: p => {
      const q = new URLSearchParams({ primaryColor: '7c3aed', secondaryColor: '0a0a0b', autoplay: 'true' });
      if (p.type === 'tv') q.set('nextbutton', 'true');
      return `https://vidlink.pro/${p.type}/${p.id}${p.type === 'tv' ? `/${p.season}/${p.episode}` : ''}?${q}`;
    },
  },
  {
    id: 'vidsrc-sh', name: 'VidSrc',
    build: p => {
      const q = new URLSearchParams({ autoplay: '1' });
      if (p.position) q.set('startAt', String(p.position));
      if (p.type === 'tv') q.set('autonext', '1');
      const imdb = p.id; // vidapi/ishowstream use tmdb id; vidsrc.sh wants imdb — fallback to tmdb anyway
      return `https://vidsrc.sh/embed/${p.type}/${imdb}${p.type === 'tv' ? `/${p.season}/${p.episode}` : ''}?${q}`;
    },
  },
  {
    id: 'videasy', name: 'Videasy',
    build: p => `https://videasy.xyz/embed/${p.type}/${p.id}${p.type === 'tv' ? `/${p.season}/${p.episode}` : ''}`,
  },
  {
    id: 'vidfast', name: 'VidFast',
    build: p => `https://vidfast.pro/${p.type}/${p.id}${p.type === 'tv' ? `/${p.season}/${p.episode}` : ''}?autoPlay=true`,
  },
];

export default function PlayerScreen() {
  const params = useLocalSearchParams<{ type: string; id: string; season?: string; episode?: string; position?: string; title?: string; poster?: string }>();
  const router = useRouter();
  const [providerIdx, setProviderIdx] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [domInfo, setDomInfo] = useState('');
  const [wrapSize, setWrapSize] = useState('');
  const [showServers, setShowServers] = useState(false);
  const insets = useSafeAreaInsets();
  const { width: winW, height: winH } = useWindowDimensions();
  const webRef = useRef<any>(null);
  const lastSaved = useRef(0);

  // Cinema-style playback: lock to landscape the moment the player opens,
  // restore portrait when leaving. The embed players have proper landscape
  // layouts — this gives them the whole screen like the CinemaOS app.
  useEffect(() => {
    void ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.LANDSCAPE);
    // Immersive mode — hide system bars so the player owns the whole screen.
    // This is the "fullscreen" the embeds' own buttons can't reliably trigger
    // inside a WebView (they require a real user gesture).
    void NavigationBar.setVisibilityAsync('hidden');
    StatusBar.setHidden(true);
    return () => {
      void ScreenOrientation.unlockAsync();
      void NavigationBar.setVisibilityAsync('visible');
      StatusBar.setHidden(false);
    };
  }, []);

  const p: P = useMemo(() => ({
    type: params.type === 'tv' ? 'tv' : 'movie',
    id: params.id || '0',
    season: Number(params.season) || 1,
    episode: Number(params.episode) || 1,
    position: Number(params.position) || 0,
  }), [params]);

  const provider = PROVIDERS[providerIdx];
  const uri = useMemo(() => provider.build(p), [provider, p]);
  const allowNav = useMemo(() => allowedFor(uri), [uri]);
  const navGuard = useMemo(() => onShouldStartLoad(allowNav), [allowNav]);

  const switchProvider = (dir: 1 | -1) => {
    setLoading(true);
    setLoadError(false);
    setProviderIdx(i => (i + dir + PROVIDERS.length) % PROVIDERS.length);
  };

  // Track playback position via the embed's postMessage timeupdate events
  const onMessage = (e: any) => {
    try {
      const data = JSON.parse(e.nativeEvent.data);
      if (data?.type === 'dominfo') { setDomInfo(`${data.tag || ''} ${data.w || ''}x${data.h || ''} hit:${data.hit || ''}`); return; }
      const ct = data?.data?.currentTime ?? data?.currentTime ?? data?.time;
      const dur = data?.data?.duration ?? data?.duration;
      if (typeof ct === 'number' && typeof dur === 'number' && dur > 0) {
        const now = Date.now();
        if (now - lastSaved.current > 15000) {
          lastSaved.current = now;
          void saveProgress({
            id: Number(p.id), media_type: p.type as 'movie' | 'tv',
            title: params.title || '', poster_path: params.poster || null,
            season: p.type === 'tv' ? p.season : undefined,
            episode: p.type === 'tv' ? p.episode : undefined,
            position: Math.floor(ct), duration: Math.floor(dur),
          });
        }
      }
    } catch {}
  };

  return (
    <View style={styles.safe}>
      <View style={styles.playerWrap} onLayout={e => setWrapSize(`${Math.round(e.nativeEvent.layout.width)}x${Math.round(e.nativeEvent.layout.height)}`)}>
        <WebView
          ref={webRef}
          key={uri}
          source={{ uri }}
          style={styles.webview}
          allowsFullscreenVideo
          allowsInlineMediaPlayback
          mediaPlaybackRequiresUserAction={false}
          javaScriptEnabled
          domStorageEnabled
          javaScriptCanOpenWindowsAutomatically={false}
          setSupportMultipleWindows={false}
          injectedJavaScriptBeforeContentLoaded={ANTI_AD_JS}
          injectedJavaScript={CLEAN_UI_JS}
          onShouldStartLoadWithRequest={navGuard}
          startInLoadingState
          onLoadStart={() => { setLoading(true); setLoadError(false); }}
          onLoadEnd={() => setLoading(false)}
          onError={() => { setLoading(false); setLoadError(true); }}
          onHttpError={() => { setLoading(false); setLoadError(true); }}
          onMessage={onMessage}
          mixedContentMode="compatibility"
        />
        {loading && (
          <View style={styles.overlay}>
            <ActivityIndicator color="#a78bfa" size="large" />
            <Text style={styles.overlayText}>Loading {provider.name}…</Text>
          </View>
        )}
        {loadError && !loading && (
          <View style={styles.overlay}>
            <Ionicons name="cloud-offline" size={40} color="rgba(255,255,255,0.4)" />
            <Text style={styles.overlayText}>{provider.name} failed to load</Text>
            <View style={styles.errBtns}>
              <TouchableOpacity style={styles.errBtn} onPress={() => { setLoading(true); setLoadError(false); webRef.current?.reload(); }}>
                <Text style={styles.errBtnText}>Retry</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.errBtnAccent} onPress={() => switchProvider(1)}>
                <Text style={styles.errBtnAccentText}>Next Server</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      </View>

      {/* Minimal floating chrome — CinemaOS's own controls own the screen.
          Back + server-switch buttons only; provider pills appear on demand. */}
      <TouchableOpacity onPress={() => router.back()} style={[styles.fab, { top: insets.top + 10, left: 10 }]}>
        <Ionicons name="chevron-down" size={22} color="#fff" />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => setShowServers(v => !v)} style={[styles.fab, { top: insets.top + 10, right: 10 }]}>
        <Ionicons name="swap-horizontal" size={20} color="#fff" />
      </TouchableOpacity>
      <Text style={styles.nowPlaying} numberOfLines={1}>{params.title || 'Now Playing'}</Text>

      {domInfo || wrapSize ? <Text style={styles.debug}>{domInfo} | wrap {wrapSize} win {winW}x{winH}</Text> : null}

      {showServers && (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={[styles.providerRow, { paddingBottom: insets.bottom + 10 }]}>
          {PROVIDERS.map((pr, i) => (
            <TouchableOpacity
              key={pr.id}
              style={[styles.providerChip, i === providerIdx && styles.providerChipActive]}
              onPress={() => { if (i !== providerIdx) { setLoading(true); setLoadError(false); setProviderIdx(i); } setShowServers(false); }}
            >
              <Text style={[styles.providerChipText, i === providerIdx && { color: '#fff' }]}>{pr.name}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#000' },
  playerWrap: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: '#000' },
  webview: { flex: 1, backgroundColor: '#000' },
  fab: { position: 'absolute', width: 38, height: 38, borderRadius: 19, backgroundColor: 'rgba(0,0,0,0.55)', alignItems: 'center', justifyContent: 'center', zIndex: 10 },
  nowPlaying: { position: 'absolute', top: 14, alignSelf: 'center', color: 'rgba(255,255,255,0.85)', fontSize: 13, fontWeight: '700', zIndex: 10, textShadowColor: 'rgba(0,0,0,0.8)', textShadowRadius: 4 },
  overlay: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.92)', gap: 12 },
  overlayText: { color: 'rgba(255,255,255,0.6)', fontSize: 13, fontWeight: '600' },
  errBtns: { flexDirection: 'row', gap: 10, marginTop: 6 },
  errBtn: { paddingHorizontal: 18, paddingVertical: 10, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.12)' },
  errBtnText: { color: '#fff', fontWeight: '700', fontSize: 13 },
  errBtnAccent: { paddingHorizontal: 18, paddingVertical: 10, borderRadius: 20, backgroundColor: '#7c3aed' },
  errBtnAccentText: { color: '#fff', fontWeight: '700', fontSize: 13 },
  debug: { position: 'absolute', top: 64, alignSelf: 'center', color: '#fbbf24', fontSize: 10, fontFamily: 'monospace', backgroundColor: 'rgba(0,0,0,0.7)', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 6, overflow: 'hidden' },
  providerRow: { position: 'absolute', bottom: 0, left: 0, right: 0, paddingHorizontal: 12, paddingVertical: 10, gap: 8, flexGrow: 1, justifyContent: 'center' },
  providerChip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 18, backgroundColor: 'rgba(255,255,255,0.07)', borderWidth: 1, borderColor: 'rgba(255,255,255,0.08)' },
  providerChipActive: { backgroundColor: '#7c3aed', borderColor: '#7c3aed' },
  providerChipText: { color: 'rgba(255,255,255,0.65)', fontSize: 12, fontWeight: '700' },
});
