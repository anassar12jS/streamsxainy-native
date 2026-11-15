import React from 'react';
import { View, Text, StyleSheet, ActivityIndicator, FlatList } from 'react-native';
import { TMDBResult } from '../lib/tmdb';
import { PosterCard } from './MediaRow';

export function GridList({ items, loading, onItemPress, emptyText = 'Nothing here yet' }: {
  items: TMDBResult[];
  loading?: boolean;
  onItemPress: (item: TMDBResult) => void;
  emptyText?: string;
}) {
  if (loading) {
    return <View style={styles.center}><ActivityIndicator color="#7c3aed" size="large" /></View>;
  }
  if (!items.length) {
    return <View style={styles.center}><Text style={styles.empty}>{emptyText}</Text></View>;
  }
  return (
    <FlatList
      data={items}
      keyExtractor={i => `${i.media_type}-${i.id}`}
      numColumns={3}
      contentContainerStyle={styles.grid}
      columnWrapperStyle={styles.gridRow}
      renderItem={({ item }) => <PosterCard item={item} onPress={() => onItemPress(item)} />}
      showsVerticalScrollIndicator={false}
    />
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingVertical: 60 },
  empty: { color: 'rgba(255,255,255,0.4)', fontSize: 14 },
  grid: { padding: 12, paddingBottom: 120 },
  gridRow: { justifyContent: 'flex-start', gap: 10, marginBottom: 14 },
});
