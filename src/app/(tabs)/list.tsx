import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, ScrollView, TouchableOpacity, Image, StyleSheet, ActivityIndicator } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { SafeAreaView } from 'react-native-safe-area-context';
import { getWatchlist, getProgress, removeProgress, ProgressItem } from '../../lib/store';
import { TMDBResult, img } from '../../lib/tmdb';
import { PosterCard } from '../../components/MediaRow';

function ProgressCard({ item, onPress, onRemove }: { item: ProgressItem; onPress: () => void; onRemove: () => void }) {
  const pct = Math.min(100, Math.max(0, (item.position / Math.max(item.duration, 1)) * 100));
  const uri = img.poster(item.poster_path, 'w342');
  return (
    <View style={styles.pCard}>
      <TouchableOpacity style={styles.pPoster} onPress={onPress} activeOpacity={0.85}>
        {uri ? <Image source={{ uri }} style={styles.pImg} resizeMode="cover" /> : <View style={[styles.pImg, { backgroundColor: '#1c1c1f' }]} />}
        <LinearGradient colors={['transparent', 'rgba(0,0,0,0.9)']} style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }} />
        <View style={styles.pPlay}>
          <Ionicons name="play" size={22} color="#fff" />
        </View>
        {item.season ? (
          <View style={styles.pBadge}><Text style={styles.pBadgeText}>S{item.season}E{item.episode}</Text></View>
        ) : null}
        <View style={styles.pBar}><View style={[styles.pFill, { width: `${pct}%` }]} /></View>
        <Text style={styles.pTitle} numberOfLines={1}>{item.title}</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.pRemove} onPress={onRemove} hitSlop={8}>
        <Ionicons name="close" size={14} color="rgba(255,255,255,0.7)" />
      </TouchableOpacity>
    </View>
  );
}

export default function ListScreen() {
  const router = useRouter();
  const [watchlist, setWatchlist] = useState<TMDBResult[]>([]);
  const [progress, setProgress] = useState<ProgressItem[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const [w, p] = await Promise.all([getWatchlist(), getProgress()]);
    setWatchlist(w);
    setProgress(p.sort((a, b) => b.updatedAt - a.updatedAt).slice(0, 20));
    setLoading(false);
  }, []);

  useFocusEffect(useCallback(() => { void refresh(); }, [refresh]));

  const openProgress = (item: ProgressItem) => {
    if (item.media_type === 'tv') {
      router.push({ pathname: '/player', params: { type: 'tv', id: String(item.id), season: String(item.season || 1), episode: String(item.episode || 1), position: String(Math.floor(item.position)) } });
    } else {
      router.push({ pathname: '/player', params: { type: 'movie', id: String(item.id), position: String(Math.floor(item.position)) } });
    }
  };

  const openItem = (item: TMDBResult) => {
    router.push(`/details/${item.media_type === 'tv' ? 'tv' : 'movie'}/${item.id}`);
  };

  if (loading) {
    return <SafeAreaView style={styles.safe}><ActivityIndicator color="#7c3aed" style={{ marginTop: 40 }} /></SafeAreaView>;
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <Text style={styles.title}>My List</Text>
      <FlatList
        data={watchlist}
        keyExtractor={i => `${i.media_type}-${i.id}`}
        numColumns={3}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: 120 }}
        columnWrapperStyle={styles.gridRow}
        renderItem={({ item }) => <View style={{ flex: 1 }}><PosterCard item={item} onPress={() => openItem(item)} /></View>}
        ListHeaderComponent={
          <View>
            {progress.length > 0 && (
              <View style={{ marginBottom: 26 }}>
                <Text style={[styles.section, { paddingHorizontal: 16 }]}>Continue Watching</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.progressWrap}>
                  {progress.map(item => (
                    <ProgressCard
                      key={`${item.media_type}-${item.id}-${item.season}-${item.episode}`}
                      item={item}
                      onPress={() => openProgress(item)}
                      onRemove={async () => { await removeProgress(item.id, item.media_type); void refresh(); }}
                    />
                  ))}
                </ScrollView>
              </View>
            )}
            <Text style={[styles.section, { paddingHorizontal: 16 }]}>Watchlist</Text>
          </View>
        }
        ListEmptyComponent={
          <View style={styles.empty}>
            <Ionicons name="bookmark-outline" size={36} color="rgba(255,255,255,0.2)" />
            <Text style={styles.emptyText}>{"Nothing saved yet — tap the bookmark on a title's page."}</Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#0a0a0b' },
  title: { color: '#fff', fontSize: 26, fontWeight: '900', paddingHorizontal: 16, paddingTop: 8, paddingBottom: 14, letterSpacing: -0.5 },
  section: { color: '#fff', fontSize: 16, fontWeight: '800', marginBottom: 10 },
  progressWrap: { flexDirection: 'row', gap: 12, paddingHorizontal: 16 },
  gridRow: { paddingHorizontal: 12, gap: 10, marginBottom: 14 },
  pCard: { width: 130 },
  pPoster: { width: 130, height: 195, borderRadius: 14, overflow: 'hidden', backgroundColor: '#1c1c1f' },
  pImg: { width: '100%', height: '100%' },
  pPlay: { position: 'absolute', top: '40%', alignSelf: 'center', width: 44, height: 44, borderRadius: 22, backgroundColor: 'rgba(124,58,237,0.9)', alignItems: 'center', justifyContent: 'center' },
  pBadge: { position: 'absolute', top: 8, left: 8, backgroundColor: 'rgba(0,0,0,0.7)', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 6 },
  pBadgeText: { color: '#fff', fontSize: 9, fontWeight: '800' },
  pBar: { position: 'absolute', bottom: 30, left: 8, right: 8, height: 3, borderRadius: 2, backgroundColor: 'rgba(255,255,255,0.2)' },
  pFill: { height: '100%', borderRadius: 2, backgroundColor: '#a78bfa' },
  pTitle: { position: 'absolute', bottom: 8, left: 8, right: 8, color: '#fff', fontSize: 11, fontWeight: '700' },
  pRemove: { position: 'absolute', top: 6, right: 6, backgroundColor: 'rgba(0,0,0,0.6)', borderRadius: 10, padding: 3 },
  empty: { alignItems: 'center', paddingTop: 80, gap: 14 },
  emptyText: { color: 'rgba(255,255,255,0.45)', fontSize: 14, textAlign: 'center', paddingHorizontal: 40 },
});
