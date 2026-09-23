export interface Preferences { theme: 'dark' | 'light' | 'system'; fontSize: number; wordWrap: boolean; sidebar: boolean }
export const PREFERENCES_KEY = 'markcraft.preferences.v1';
export const DEFAULT_PREFERENCES: Preferences = { theme: 'dark', fontSize: 14, wordWrap: true, sidebar: true };
const FONT_SIZES = [12, 14, 16, 18] as const;

export function normalizePreferences(value: unknown): Preferences {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return { ...DEFAULT_PREFERENCES };
  const candidate = value as Record<string, unknown>;
  return {
    theme: candidate['theme'] === 'light' || candidate['theme'] === 'system' ? candidate['theme'] : 'dark',
    fontSize: FONT_SIZES.includes(candidate['fontSize'] as 12 | 14 | 16 | 18) ? candidate['fontSize'] as number : 14,
    wordWrap: typeof candidate['wordWrap'] === 'boolean' ? candidate['wordWrap'] : true,
    sidebar: typeof candidate['sidebar'] === 'boolean' ? candidate['sidebar'] : true,
  };
}

export function readPreferences(): Preferences {
  try {
    const raw = localStorage.getItem(PREFERENCES_KEY);
    return raw ? normalizePreferences(JSON.parse(raw)) : { ...DEFAULT_PREFERENCES };
  } catch { return { ...DEFAULT_PREFERENCES }; }
}
