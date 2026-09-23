import { parseDocument } from 'yaml';
import { markdownBody, parseOkf, splitFrontmatter, updateOkf } from './okf';

const completeFrontmatter = [
  '---', '# Keep human-authored comments.', 'type: note', 'version: 1', 'title: Original', 'name: Example',
  'description: Description text', 'status: draft', 'author: Nikhil', 'tags:', '  - one', '  - two', 'sources:',
  '  - https://example.test', 'resource: vault://item', 'stale_after: 2030-01-01', 'generated:',
  '  by: MarkCraft', '  model: model-a', '  custom_generated: keep-generated', 'verified:',
  '  by: Nikhil', '  at: 2026-09-23', '  custom_verified: keep-verified', 'custom:',
  '  keep: [one, two]', '---', '# Body stays exactly here.', '',
].join('\n');

describe('OKF metadata', () => {
  it('parses schema fields and the preexisting version/author fields and excludes frontmatter from Markdown', () => {
    const metadata = parseOkf(completeFrontmatter);
    expect(metadata.valid).toBe(true);
    expect(metadata).toMatchObject({
      type: 'note', version: '1', title: 'Original', name: 'Example', description: 'Description text', status: 'draft', author: 'Nikhil',
      tags: ['one', 'two'], sources: ['https://example.test'], resource: 'vault://item', stale_after: '2030-01-01',
      generated: { by: 'MarkCraft', model: 'model-a', custom_generated: 'keep-generated' },
      verified: { by: 'Nikhil', at: '2026-09-23', custom_verified: 'keep-verified' },
    });
    expect(markdownBody(completeFrontmatter)).toBe('# Body stays exactly here.\n');
    expect(splitFrontmatter(completeFrontmatter).hasFrontmatter).toBe(true);
  });

  it('requires a nonempty string type', () => {
    expect(parseOkf('---\ntitle: X\n---\nBody').errors).toContain('Required type must be a nonempty string.');
    expect(parseOkf('---\ntype: 7\n---\nBody').valid).toBe(false);
    expect(parseOkf('---\ntype: "  "\n---\nBody').valid).toBe(false);
  });

  it('reports malformed YAML and duplicate keys as invalid without allowing conform', () => {
    const malformed = '---\ntype: note\ntags: [not-closed\n---\n# body';
    const parsed = parseOkf(malformed);
    expect(parsed.valid).toBe(false);
    expect(parsed.yamlValid).toBe(false);
    expect(parsed.errors[0]).toContain('Malformed YAML');
    expect(updateOkf(malformed, { ...parsed, type: 'concept', yamlValid: true })).toBe(malformed);
    expect(parseOkf('---\ntype: note\ntype: idea\n---').yamlValid).toBe(false);
  });

  it('reports schema field shape errors', () => {
    const parsed = parseOkf('---\ntype: note\ntags: one\ngenerated: agent\n---');
    expect(parsed.valid).toBe(false);
    expect(parsed.errors).toContain('tags must be a list of strings.');
    expect(parsed.errors).toContain('generated must be a mapping.');
  });

  it('updates metadata while retaining unknown fields, nested extras, comments, and body', () => {
    const current = parseOkf(completeFrontmatter);
    const result = updateOkf(completeFrontmatter, { ...current, title: 'Updated title', tags: ['two', 'three'] });
    const parsed = parseOkf(result);
    const yaml = parseDocument(splitFrontmatter(result).raw).toJS() as Record<string, unknown>;
    expect(parsed.title).toBe('Updated title');
    expect(parsed.tags).toEqual(['two', 'three']);
    expect(yaml['custom']).toEqual({ keep: ['one', 'two'] });
    expect(yaml['generated']).toMatchObject({ custom_generated: 'keep-generated' });
    expect(yaml['verified']).toMatchObject({ custom_verified: 'keep-verified' });
    expect(result).toContain('# Keep human-authored comments.');
    expect(markdownBody(result)).toBe('# Body stays exactly here.\n');
  });

  it('conforms missing type without dropping unknown YAML keys', () => {
    const source = '---\ntitle: X\ncustom: keep-me\n---\nBody';
    const parsed = parseOkf(source);
    expect(parsed.yamlValid).toBe(true);
    const result = updateOkf(source, { ...parsed, type: 'concept', valid: true });
    expect(result).toContain('custom: keep-me');
    expect(parseOkf(result).valid).toBe(true);
  });

  it('adds frontmatter for a document with none and blocks an unclosed delimiter', () => {
    const body = '# Plain Markdown\n';
    const missing = parseOkf(body);
    const conformed = updateOkf(body, { ...missing, type: 'concept' });
    expect(conformed).toContain('---\ntype: concept\n---');
    expect(markdownBody(conformed)).toBe(body);
    expect(parseOkf('---\ntype: note').yamlValid).toBe(false);
  });
});
