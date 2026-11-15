import React from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet } from 'react-native';
import { TMDBResult, img } from '../lib/tmdb';
import { LinearGradient } from 'expo-linear-gradient';

const CARD_W = 120;
const CARD_H = 180;

export function PosterCard({ item, onPress }: { item: TMDBResult; onPress: () => void }) {
  const uri = img.poster(item.poster_path, 'w342');
  const title = item.title || item.name || '';
  const year = (item.release_date || item.first_air_date || '').slice(0, 4);
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.85}>
      <View style={styles.posterWrap}>
        {uri ? (
          <Image source={{ uri }} style={styles.poster} resizeMode="cover" />
        ) : (
          <View style={[styles.poster, styles.posterFallback]} />
        )}
        <LinearGradient colors={['transparent', 'rgba(0,0,0,0.85)']} style={StyleSheet.absoluteFill} />
        {typeof item.vote_average === 'number' && item.vote_average > 0 && (
          <View style={styles.rating}>
            <Text style={styles.ratingText}>★ {item.vote_average.toFixed(1)}</Text>
          </View>
        )}
        <View style={styles.meta}>
          <Text style={styles.title} numberOfLines={1}>{title}</Text>
          <Text style={styles.sub}>{year}{item.media_type ? ` · ${item.media_type === 'tv' ? 'Series' : 'Movie'}` : ''}</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
}

export function MediaRow({ title, items, onItemPress, onSeeAll }: {
  title: string;
  items: TMDBResult[];
  onItemPress: (item: TMDBResult) => void;
  onSeeAll?: () => void;
}) {
  if (!items?.length) return null;
  return (
    <View style={styles.row}>
      <View style={styles.rowHeader}>
        <View style={styles.rowTitleWrap}>
          <View style={styles.accentBar} />
          <Text style={styles.rowTitle}>{title}</Text>
        </View>
        {onSeeAll && (
          <TouchableOpacity onPress={onSeeAll}><Text style={styles.seeAll}>See all</Text></TouchableOpacity>
        )}
      </View>
      <View style={styles.rowScroll}>
        {items.map(item => (
          <PosterCard key={`${item.media_type}-${item.id}`} item={item} onPress={() => onItemPress(item)} />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { marginBottom: 22 },
  rowHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, marginBottom: 10 },
  rowTitleWrap: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  accentBar: { width: 4, height: 18, borderRadius: 2, backgroundColor: '#7c3aed' },
  rowTitle: { color: '#fff', fontSize: 17, fontWeight: '800', letterSpacing: -0.3 },
  seeAll: { color: '#a78bfa', fontSize: 13, fontWeight: '600' },
  rowScroll: { flexDirection: 'row', paddingHorizontal: 16, gap: 12 },
  card: { width: CARD_W },
  posterWrap: { width: CARD_W, height: CARD_H, borderRadius: 14, overflow: 'hidden', backgroundColor: '#1c1c1f' },
  poster: { width: '100%', height: '100%' },
  posterFallback: { backgroundColor: '#26262b' },
  rating: { position: 'absolute', top: 8, right: 8, backgroundColor: 'rgba(0,0,0,0.7)', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 6 },
  ratingText: { color: '#fbbf24', fontSize: 10, fontWeight: '700' },
  meta: { position: 'absolute', left: 8, right: 8, bottom: 8 },
  title: { color: '#fff', fontSize: 12, fontWeight: '700' },
  sub: { color: 'rgba(255,255,255,0.6)', fontSize: 10, marginTop: 1 },
});
