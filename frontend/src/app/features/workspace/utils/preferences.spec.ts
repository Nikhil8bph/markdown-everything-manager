import { describe, expect, it } from 'vitest';
import { normalizePreferences } from './preferences';

describe('normalizePreferences', () => {
  it('keeps valid preferences', () => expect(normalizePreferences({ theme: 'light', fontSize: 18, wordWrap: false, sidebar: false }))
    .toEqual({ theme: 'light', fontSize: 18, wordWrap: false, sidebar: false }));
  it('falls back invalid values individually', () => expect(normalizePreferences({ theme: 'neon', fontSize: 999, wordWrap: 'false', sidebar: null }))
    .toEqual({ theme: 'dark', fontSize: 14, wordWrap: true, sidebar: true }));
  it('returns defaults for malformed shapes', () => expect(normalizePreferences(null))
    .toEqual({ theme: 'dark', fontSize: 14, wordWrap: true, sidebar: true }));
});
