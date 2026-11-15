import React, { useCallback, useEffect, useState } from 'react';
import {
  View, Text, Image, ScrollView, TouchableOpacity, StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api, img, TitleDetail, Episode, MediaType, TMDBResult } from '../../../lib/tmdb';
import { isInWatchlist, toggleWatchlist } from '../../../lib/store';

export default function DetailsScreen() {
  const { type, id } = useLocalSearchParams<{ type: string; id: string }>();
  const mediaType: MediaType = type === 'tv' ? 'tv' : 'movie';
  const numericId = Number(id);
  const router = useRouter();

  const [detail, setDetail] = useState<TitleDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [inList, setInList] = useState(false);
  const [seasonNum, setSeasonNum] = useState<number | null>(null);
  const [episodes, setEpisodes] = useState<Episode[] | null>(null);
  const [epsLoading, setEpsLoading] = useState(false);

  const loadEpisodes = useCallback(async (season: number) => {
    setEpsLoading(true);
    try {
      const d = await api.season(numericId, season);
      setEpisodes(d.episodes || []);
    } catch { setEpisodes([]); } finally { setEpsLoading(false); }
  }, [numericId]);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const d = await api.details(numericId, mediaType);
        if (!alive) return;
        setDetail(d);
        setInList(await isInWatchlist(d.id));
        if (mediaType === 'tv' && d.seasons?.length) {
          const s = d.seasons.find(s => s.season_number > 0) || d.seasons[0];
          setSeasonNum(s.season_number);
          void loadEpisodes(s.season_number);
        }
      } catch {} finally { if (alive) setLoading(false); }
    })();
    return () => { alive = false; };
  }, [numericId, mediaType, loadEpisodes]);

  const play = (season = 1, episode = 1) => {
    router.push({ pathname: '/player', params: { type: mediaType, id: String(numericId), season: String(season), episode: String(episode) } });
  };

  const onToggleList = async () => {
    if (!detail) return;
    const now = await toggleWatchlist(detail as TMDBResult);
    setInList(now);
  };

  if (loading) {
    return <SafeAreaView style={styles.safe}><ActivityIndicator color="#7c3aed" style={{ marginTop: 80 }} size="large" /></SafeAreaView>;
  }
  if (!detail) {
    return (
      <SafeAreaView style={styles.safe}>
        <View style={styles.center}><Text style={styles.muted}>{"Couldn't load this title."}</Text></View>
      </SafeAreaView>
    );
  }

  const title = detail.title || detail.name || '';
  const year = (detail.release_date || detail.first_air_date || '').slice(0, 4);
  const runtime = detail.runtime || detail.episode_run_time?.[0];

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 60 }}>
        {/* Backdrop hero */}
        <View style={styles.hero}>
          {img.backdrop(detail.backdrop_path, 'w1280') && (
            <Image source={{ uri: img.backdrop(detail.backdrop_path, 'w1280')! }} style={StyleSheet.absoluteFill} resizeMode="cover" />
          )}
          <LinearGradient colors={['rgba(10,10,11,0.55)', 'transparent', '#0a0a0b']} style={StyleSheet.absoluteFill} />
          <TouchableOpacity style={styles.backBtn} onPress={() => router.back()}>
            <Ionicons name="chevron-back" size={22} color="#fff" />
          </TouchableOpacity>
        </View>

        {/* Poster + info */}
        <View style={styles.infoRow}>
          <View style={styles.posterWrap}>
            {img.poster(detail.poster_path, 'w342') ? (
              <Image source={{ uri: img.poster(detail.poster_path, 'w342')! }} style={styles.poster} resizeMode="cover" />
            ) : <View style={[styles.poster, { backgroundColor: '#1c1c1f' }]} />}
          </View>
          <View style={styles.info}>
            <Text style={styles.title}>{title}</Text>
            <Text style={styles.meta}>
              {year}{runtime ? ` · ${runtime}m` : ''}{detail.vote_average ? ` · ★ ${detail.vote_average.toFixed(1)}` : ''}
            </Text>
            {!!detail.genres?.length && (
              <Text style={styles.genres}>{detail.genres.slice(0, 3).map(g => g.name).join(' · ')}</Text>
            )}
            <View style={styles.actions}>
              <TouchableOpacity style={styles.playBtn} onPress={() => play()}>
                <Ionicons name="play" size={18} color="#0a0a0b" />
                <Text style={styles.playBtnText}>{mediaType === 'tv' ? 'Play S1E1' : 'Play'}</Text>
              </TouchableOpacity>
              <TouchableOpacity style={[styles.iconBtn, inList && styles.iconBtnActive]} onPress={onToggleList}>
                <Ionicons name={inList ? 'checkmark' : 'add'} size={20} color="#fff" />
              </TouchableOpacity>
              <TouchableOpacity style={styles.iconBtn} onPress={() => router.push({ pathname: '/player', params: { type: mediaType, id: String(numericId), season: '1', episode: '1' } })}>
                <Ionicons name="download-outline" size={20} color="#fff" />
              </TouchableOpacity>
            </View>
          </View>
        </View>

        {/* Overview */}
        {!!detail.overview && (
          <View style={styles.section}>
            <Text style={styles.overview}>{detail.overview}</Text>
          </View>
        )}

        {/* Seasons & episodes for TV */}
        {mediaType === 'tv' && !!detail.seasons?.length && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Episodes</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 8 }}>
              {detail.seasons.filter(s => s.season_number > 0).map(s => (
                <TouchableOpacity
                  key={s.season_number}
                  style={[styles.seasonChip, seasonNum === s.season_number && styles.seasonChipActive]}
                  onPress={() => { setSeasonNum(s.season_number); void loadEpisodes(s.season_number); }}
                >
                  <Text style={[styles.seasonChipText, seasonNum === s.season_number && { color: '#fff' }]}>
                    {s.name || `Season ${s.season_number}`}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
            {epsLoading ? (
              <ActivityIndicator color="#7c3aed" style={{ marginVertical: 20 }} />
            ) : (
              (episodes || []).map(ep => (
                <TouchableOpacity key={ep.episode_number} style={styles.epRow} onPress={() => play(seasonNum || 1, ep.episode_number)}>
                  <View style={styles.epNum}>
                    <Text style={styles.epNumText}>{ep.episode_number}</Text>
                  </View>
                  <View style={styles.epInfo}>
                    <Text style={styles.epName} numberOfLines={1}>{ep.name || `Episode ${ep.episode_number}`}</Text>
                    <Text style={styles.epMeta} numberOfLines={1}>
                      {ep.air_date?.slice(0, 10) || ''}{ep.runtime ? ` · ${ep.runtime}m` : ''}{ep.vote_average ? ` · ★ ${ep.vote_average.toFixed(1)}` : ''}
                    </Text>
                  </View>
                  <Ionicons name="play-circle" size={26} color="#a78bfa" />
                </TouchableOpacity>
              ))
            )}
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#0a0a0b' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  muted: { color: 'rgba(255,255,255,0.4)' },
  hero: { height: 240, width: '100%', backgroundColor: '#141416' },
  backBtn: { position: 'absolute', top: 12, left: 12, width: 36, height: 36, borderRadius: 18, backgroundColor: 'rgba(0,0,0,0.55)', alignItems: 'center', justifyContent: 'center' },
  infoRow: { flexDirection: 'row', paddingHorizontal: 16, marginTop: -70, gap: 14 },
  posterWrap: { shadowColor: '#000', shadowOpacity: 0.6, shadowRadius: 12, shadowOffset: { width: 0, height: 6 }, elevation: 8 },
  poster: { width: 110, height: 165, borderRadius: 14, borderWidth: 1, borderColor: 'rgba(255,255,255,0.12)' },
  info: { flex: 1, paddingTop: 74 },
  title: { color: '#fff', fontSize: 24, fontWeight: '900', letterSpacing: -0.5 },
  meta: { color: 'rgba(255,255,255,0.55)', fontSize: 12, fontWeight: '600', marginTop: 4 },
  genres: { color: '#a78bfa', fontSize: 11, fontWeight: '700', marginTop: 5 },
  actions: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 14 },
  playBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: '#fff', paddingHorizontal: 18, paddingVertical: 11, borderRadius: 24 },
  playBtnText: { color: '#0a0a0b', fontWeight: '800', fontSize: 14 },
  iconBtn: { width: 42, height: 42, borderRadius: 21, backgroundColor: 'rgba(255,255,255,0.1)', alignItems: 'center', justifyContent: 'center' },
  iconBtnActive: { backgroundColor: '#7c3aed' },
  section: { paddingHorizontal: 16, marginTop: 24 },
  overview: { color: 'rgba(255,255,255,0.75)', fontSize: 13.5, lineHeight: 21 },
  sectionTitle: { color: '#fff', fontSize: 17, fontWeight: '800', marginBottom: 12 },
  seasonChip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 18, backgroundColor: 'rgba(255,255,255,0.07)', borderWidth: 1, borderColor: 'rgba(255,255,255,0.08)' },
  seasonChipActive: { backgroundColor: '#7c3aed', borderColor: '#7c3aed' },
  seasonChipText: { color: 'rgba(255,255,255,0.7)', fontSize: 12, fontWeight: '700' },
  epRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: 'rgba(255,255,255,0.05)' },
  epNum: { width: 30, height: 30, borderRadius: 15, backgroundColor: 'rgba(255,255,255,0.07)', alignItems: 'center', justifyContent: 'center' },
  epNumText: { color: '#fff', fontSize: 12, fontWeight: '800' },
  epInfo: { flex: 1 },
  epName: { color: '#fff', fontSize: 13, fontWeight: '700' },
  epMeta: { color: 'rgba(255,255,255,0.45)', fontSize: 11, marginTop: 2 },
});
