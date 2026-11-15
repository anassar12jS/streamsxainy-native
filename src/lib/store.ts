import AsyncStorage from '@react-native-async-storage/async-storage';
import { TMDBResult } from './tmdb';

export interface ProgressItem {
  id: number;
  media_type: 'movie' | 'tv';
  title: string;
  poster_path: string | null;
  season?: number;
  episode?: number;
  position: number; // seconds
  duration: number;
  updatedAt: number;
}

const WATCHLIST_KEY = 'sx_watchlist';
const PROGRESS_KEY = 'sx_progress';

export async function getWatchlist(): Promise<TMDBResult[]> {
  try { return JSON.parse((await AsyncStorage.getItem(WATCHLIST_KEY)) || '[]'); } catch { return []; }
}
export async function toggleWatchlist(item: TMDBResult): Promise<boolean> {
  const list = await getWatchlist();
  const idx = list.findIndex(i => i.id === item.id);
  if (idx >= 0) { list.splice(idx, 1); await AsyncStorage.setItem(WATCHLIST_KEY, JSON.stringify(list)); return false; }
  list.unshift({ ...item, media_type: item.media_type || (item.title ? 'movie' : 'tv') });
  await AsyncStorage.setItem(WATCHLIST_KEY, JSON.stringify(list.slice(0, 200)));
  return true;
}
export async function isInWatchlist(id: number): Promise<boolean> {
  return (await getWatchlist()).some(i => i.id === id);
}

export async function getProgress(): Promise<ProgressItem[]> {
  try { return JSON.parse((await AsyncStorage.getItem(PROGRESS_KEY)) || '[]'); } catch { return []; }
}
export async function saveProgress(p: Omit<ProgressItem, 'updatedAt'>): Promise<void> {
  const list = await getProgress();
  const idx = list.findIndex(i => i.id === p.id && i.media_type === p.media_type && i.season === p.season && i.episode === p.episode);
  const item = { ...p, updatedAt: Date.now() };
  if (idx >= 0) list[idx] = item; else list.unshift(item);
  await AsyncStorage.setItem(PROGRESS_KEY, JSON.stringify(list.slice(0, 100)));
}
export async function removeProgress(id: number, mediaType: string): Promise<void> {
  const list = (await getProgress()).filter(i => !(i.id === id && i.media_type === mediaType));
  await AsyncStorage.setItem(PROGRESS_KEY, JSON.stringify(list));
}
