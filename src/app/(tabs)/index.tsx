import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, Image, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { api, TMDBResult, img } from '../../lib/tmdb';
import { MediaRow } from '../../components/MediaRow';
import { SafeAreaView } from 'react-native-safe-area-context';

const HERO_H = 460;

export default function HomeScreen() {
  const router = useRouter();
  const [trending, setTrending] = useState<TMDBResult[]>([]);
  const [popularMovies, setPopularMovies] = useState<TMDBResult[]>([]);
  const [popularTv, setPopularTv] = useState<TMDBResult[]>([]);
  const [topMovies, setTopMovies] = useState<TMDBResult[]>([]);
  const [topTv, setTopTv] = useState<TMDBResult[]>([]);
  const [nowPlaying, setNowPlaying] = useState<TMDBResult[]>([]);
  const [upcoming, setUpcoming] = useState<TMDBResult[]>([]);
  const [airing, setAiring] = useState<TMDBResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [heroIndex, setHeroIndex] = useState(0);

  useEffect(() => {
    Promise.all([
      api.trending(), api.popularMovies(), api.popularTv(), api.topRatedMovies(),
      api.topRatedTv(), api.nowPlaying(), api.upcoming(), api.airingToday(),
    ]).then(([t, pm, pt, tm, tt, np, up, ar]) => {
      setTrending(t); setPopularMovies(pm); setPopularTv(pt); setTopMovies(tm);
      setTopTv(tt); setNowPlaying(np); setUpcoming(up); setAiring(ar);
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const heroItems = useMemo(() => trending.filter(i => i.backdrop_path).slice(0, 6), [trending]);
  useEffect(() => {
    if (heroItems.length < 2) return;
    const t = setInterval(() => setHeroIndex(i => (i + 1) % heroItems.length), 7000);
    return () => clearInterval(t);
  }, [heroItems.length]);

  const hero = heroItems[heroIndex];

  const openDetails = (item: TMDBResult) => {
    const type = item.media_type === 'tv' ? 'tv' : 'movie';
    router.push(`/details/${type}/${item.id}`);
  };

  const playNow = (item: TMDBResult) => {
    const type = item.media_type === 'tv' ? 'tv' : 'movie';
    router.push({ pathname: '/player', params: { type, id: String(item.id), season: '1', episode: '1' } });
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <View style={styles.logoBox}>
          <Ionicons name="film" size={16} color="#fff" />
        </View>
        <Text style={styles.logo}>STREAMSXAINY</Text>
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 110 }}>
        {/* Hero */}
        {hero && (
          <View style={styles.heroWrap}>
            <Image key={hero.id} source={{ uri: img.backdrop(hero.backdrop_path, 'w1280')! }} style={styles.heroImg} resizeMode="cover" />
            <LinearGradient colors={['rgba(0,0,0,0.15)', 'transparent', '#0a0a0b']} style={StyleSheet.absoluteFill} />
            <View style={styles.heroBottom}>
              <View style={styles.heroMeta}>
                <Text style={styles.heroKicker}>TRENDING #{heroIndex + 1}</Text>
                <Text style={styles.heroTitle} numberOfLines={2}>{hero.title || hero.name}</Text>
                <Text style={styles.heroSub}>
                  {hero.media_type === 'tv' ? 'SERIES' : 'MOVIE'} · ★ {hero.vote_average?.toFixed(1)} · {(hero.release_date || hero.first_air_date || '').slice(0, 4)}
                </Text>
              </View>
              <View style={styles.heroBtns}>
                <TouchableOpacity style={styles.btnPrimary} onPress={() => openDetails(hero)}>
                  <Ionicons name="information-circle" size={18} color="#0a0a0b" />
                  <Text style={styles.btnPrimaryText}>Details</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.btnAccent} onPress={() => playNow(hero)}>
                  <Ionicons name="play" size={18} color="#fff" />
                  <Text style={styles.btnAccentText}>Watch Now</Text>
                </TouchableOpacity>
              </View>
              <View style={styles.dots}>
                {heroItems.map((_, i) => (
                  <View key={i} style={[styles.dot, i === heroIndex && styles.dotActive]} />
                ))}
              </View>
            </View>
          </View>
        )}

        {loading ? (
          <ActivityIndicator color="#7c3aed" style={{ marginTop: 40 }} size="large" />
        ) : (
          <>
            <MediaRow title="Trending This Week" items={trending} onItemPress={openDetails} />
            <MediaRow title="Now Playing in Cinemas" items={nowPlaying} onItemPress={openDetails} />
            <MediaRow title="Popular Movies" items={popularMovies} onItemPress={openDetails} />
            <MediaRow title="Popular Series" items={popularTv} onItemPress={openDetails} />
            <MediaRow title="Top Rated Movies" items={topMovies} onItemPress={openDetails} />
            <MediaRow title="Top Rated Series" items={topTv} onItemPress={openDetails} />
            <MediaRow title="Coming Soon" items={upcoming} onItemPress={openDetails} />
            <MediaRow title="Airing Today" items={airing} onItemPress={openDetails} />
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#0a0a0b' },
  header: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingVertical: 10 },
  logoBox: { width: 26, height: 26, borderRadius: 7, backgroundColor: '#7c3aed', alignItems: 'center', justifyContent: 'center' },
  logo: { color: '#fff', fontWeight: '900', fontSize: 14, letterSpacing: 1 },
  heroWrap: { height: HERO_H, marginHorizontal: 12, borderRadius: 20, overflow: 'hidden', marginBottom: 14 },
  heroImg: { width: '100%', height: '100%' },
  heroBottom: { position: 'absolute', bottom: 0, left: 0, right: 0, padding: 16 },
  heroMeta: { marginBottom: 12 },
  heroKicker: { color: '#a78bfa', fontSize: 11, fontWeight: '800', letterSpacing: 1.5, marginBottom: 6 },
  heroTitle: { color: '#fff', fontSize: 30, fontWeight: '900', letterSpacing: -0.5 },
  heroSub: { color: 'rgba(255,255,255,0.65)', fontSize: 12, fontWeight: '600', marginTop: 6, letterSpacing: 0.5 },
  heroBtns: { flexDirection: 'row', gap: 10, marginBottom: 12 },
  btnPrimary: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: '#fff', paddingHorizontal: 18, paddingVertical: 11, borderRadius: 24 },
  btnPrimaryText: { color: '#0a0a0b', fontWeight: '800', fontSize: 14 },
  btnAccent: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: '#7c3aed', paddingHorizontal: 18, paddingVertical: 11, borderRadius: 24 },
  btnAccentText: { color: '#fff', fontWeight: '800', fontSize: 14 },
  dots: { flexDirection: 'row', gap: 5 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.35)' },
  dotActive: { width: 20, backgroundColor: '#fff' },
});
