import React, { useEffect, useState } from 'react';
import { View, TextInput, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api, TMDBResult } from '../../lib/tmdb';
import { GridList } from '../../components/GridList';

export default function SearchScreen() {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<TMDBResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState('');

  useEffect(() => {
    const t = setTimeout(async () => {
      const q = query.trim();
      if (q.length < 2) { setResults([]); setSearched(''); return; }
      setLoading(true);
      try {
        const r = await api.search(q);
        setResults(r);
        setSearched(q);
      } catch {} finally { setLoading(false); }
    }, 350);
    return () => clearTimeout(t);
  }, [query]);

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.searchWrap}>
          <Ionicons name="search" size={18} color="rgba(255,255,255,0.4)" />
          <TextInput
            style={styles.input}
            placeholder="Search movies & series..."
            placeholderTextColor="rgba(255,255,255,0.3)"
            value={query}
            onChangeText={setQuery}
            autoCapitalize="none"
            autoCorrect={false}
            returnKeyType="search"
          />
          {query.length > 0 && (
            <Ionicons name="close-circle" size={18} color="rgba(255,255,255,0.4)" onPress={() => setQuery('')} />
          )}
        </View>
        <GridList
          items={results}
          loading={loading}
          emptyText={searched ? `No results for "${searched}"` : 'Type to search movies and series'}
          onItemPress={item => router.push(`/details/${item.media_type === 'tv' ? 'tv' : 'movie'}/${item.id}`)}
        />
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#0a0a0b' },
  searchWrap: {
    flexDirection: 'row', alignItems: 'center', gap: 10,
    margin: 16, paddingHorizontal: 16, height: 50,
    backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: 16,
    borderWidth: 1, borderColor: 'rgba(255,255,255,0.08)',
  },
  input: { flex: 1, color: '#fff', fontSize: 15 },
});
