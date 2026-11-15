const API_KEY = '15d2ea6d0dc1d476efbca3eba2b9bbfb';
const BASE = 'https://api.themoviedb.org/3';

export type MediaType = 'movie' | 'tv';

export interface TMDBResult {
  id: number;
  media_type?: MediaType;
  title?: string;
  name?: string;
  poster_path: string | null;
  backdrop_path?: string | null;
  release_date?: string;
  first_air_date?: string;
  vote_average?: number;
  overview?: string;
}

export interface Genre { id: number; name: string }

export interface Episode {
  episode_number: number;
  name: string;
  overview: string;
  still_path: string | null;
  air_date: string | null;
  runtime: number | null;
  vote_average: number;
}

export interface Season {
  season_number: number;
  name: string;
  episode_count: number;
  poster_path: string | null;
  air_date: string | null;
}

export interface TitleDetail extends TMDBResult {
  genres?: Genre[];
  tagline?: string;
  status?: string;
  number_of_seasons?: number;
  number_of_episodes?: number;
  seasons?: Season[];
  runtime?: number;
  episode_run_time?: number[];
  original_language?: string;
  imdb_id?: string | null;
  belongs_to_collection?: { id: number; name: string; poster_path: string | null } | null;
}

export const img = {
  poster: (p: string | null | undefined, size: 'w185' | 'w342' | 'original' = 'w342') =>
    p ? `https://image.tmdb.org/t/p/${size}${p}` : null,
  backdrop: (p: string | null | undefined, size: 'w780' | 'w1280' | 'original' = 'w1280') =>
    p ? `https://image.tmdb.org/t/p/${size}${p}` : null,
};

async function get<T>(path: string, params: Record<string, string> = {}): Promise<T> {
  const qs = new URLSearchParams({ api_key: API_KEY, ...params });
  const res = await fetch(`${BASE}${path}?${qs}`);
  if (!res.ok) throw new Error(`TMDB ${res.status}`);
  return res.json() as Promise<T>;
}

const mapResults = (d: any): TMDBResult[] =>
  (d.results || []).map((r: any) => ({ ...r, media_type: r.media_type === 'tv' ? 'tv' : 'movie' }));

export const api = {
  trending: () => get<any>('/trending/all/week').then(mapResults),
  popularMovies: () => get<any>('/movie/popular').then(d => mapResults({ results: d.results.map((r: any) => ({ ...r, media_type: 'movie' })) })),
  popularTv: () => get<any>('/tv/popular').then(d => mapResults({ results: d.results.map((r: any) => ({ ...r, media_type: 'tv' })) })),
  topRatedMovies: () => get<any>('/movie/top_rated').then(d => mapResults({ results: d.results.map((r: any) => ({ ...r, media_type: 'movie' })) })),
  topRatedTv: () => get<any>('/tv/top_rated').then(d => mapResults({ results: d.results.map((r: any) => ({ ...r, media_type: 'tv' })) })),
  nowPlaying: () => get<any>('/movie/now_playing').then(d => mapResults({ results: d.results.map((r: any) => ({ ...r, media_type: 'movie' })) })),
  upcoming: () => get<any>('/movie/upcoming').then(d => mapResults({ results: d.results.map((r: any) => ({ ...r, media_type: 'movie' })) })),
  airingToday: () => get<any>('/tv/airing_today').then(d => mapResults({ results: d.results.map((r: any) => ({ ...r, media_type: 'tv' })) })),
  search: (q: string) =>
    get<any>('/search/multi', { query: q, include_adult: 'false' })
      .then(d => mapResults(d).filter(r => r.media_type === 'movie' || r.media_type === 'tv')),
  details: (id: number, type: MediaType) => get<TitleDetail>(`/${type}/${id}`),
  season: (id: number, season: number) => get<{ episodes: Episode[] }>(`/tv/${id}/season/${season}`),
};
