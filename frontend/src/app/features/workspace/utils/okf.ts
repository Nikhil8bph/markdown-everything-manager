import { parseDocument } from 'yaml';

export interface OkfNestedMetadata {
  by?: string;
  model?: string;
  at?: string;
  [key: string]: unknown;
}

export interface OkfMetadata {
  type: string;
  version?: string;
  title?: string;
  name?: string;
  description?: string;
  status?: string;
  author?: string;
  tags?: string[];
  sources?: string[];
  resource?: string;
  stale_after?: string;
  generated?: OkfNestedMetadata;
  verified?: OkfNestedMetadata;
  valid: boolean;
  yamlValid: boolean;
  errors: string[];
  raw: string;
}

interface FrontmatterParts {
  raw: string;
  body: string;
  hasFrontmatter: boolean;
}

const STRING_FIELDS = ['version', 'title', 'name', 'description', 'status', 'author', 'resource', 'stale_after'] as const;
const SCHEMA_STRING_FIELDS = ['title', 'name', 'description', 'status', 'resource', 'stale_after'] as const;
const ARRAY_FIELDS = ['tags', 'sources'] as const;

export function splitFrontmatter(content: string): FrontmatterParts {
  const match = content.match(/^---[ \t]*\r?\n([\s\S]*?)\r?\n---[ \t]*(?:\r?\n|$)/);
  if (!match) return { raw: '', body: content, hasFrontmatter: false };
  return { raw: match[1], body: content.slice(match[0].length), hasFrontmatter: true };
}

export function markdownBody(content: string): string {
  return splitFrontmatter(content).body;
}

export function parseOkf(content: string): OkfMetadata {
  const parts = splitFrontmatter(content);
  const errors: string[] = [];
  if (!parts.hasFrontmatter) {
    if (/^---[ \t]*(?:\r?\n|$)/.test(content)) {
      errors.push('Malformed frontmatter: closing delimiter is missing.');
      return emptyMetadata(parts.raw, errors, false);
    }
    errors.push('OKF frontmatter is missing.');
    return emptyMetadata(parts.raw, errors, true);
  }

  const yaml = parseDocument(parts.raw, { uniqueKeys: true, prettyErrors: false });
  if (yaml.errors.length) {
    const first = yaml.errors[0];
    const line = first.linePos?.[0]?.line;
    errors.push(`Malformed YAML${line ? ` near line ${line}` : ''}: ${first.message.split('\n')[0]}`);
    return emptyMetadata(parts.raw, errors, false);
  }

  const value: unknown = yaml.toJS({ mapAsMap: false });
  if (!isRecord(value)) {
    errors.push('Frontmatter must be a YAML mapping.');
    return emptyMetadata(parts.raw, errors, true);
  }

  const type = value['type'];
  if (typeof type !== 'string' || !type.trim()) errors.push('Required type must be a nonempty string.');
  for (const key of SCHEMA_STRING_FIELDS) {
    if (value[key] !== undefined && typeof value[key] !== 'string') errors.push(`${key} must be a string.`);
  }
  for (const key of ARRAY_FIELDS) {
    if (value[key] !== undefined && (!Array.isArray(value[key]) || !value[key].every(item => typeof item === 'string'))) {
      errors.push(`${key} must be a list of strings.`);
    }
  }
  validateNested(value, 'generated', ['by', 'model'], errors);
  validateNested(value, 'verified', ['by', 'at'], errors);

  return {
    type: typeof type === 'string' ? type : '',
    version: asLegacyString(value['version']),
    title: asOptionalString(value['title']),
    name: asOptionalString(value['name']),
    description: asOptionalString(value['description']),
    status: asOptionalString(value['status']),
    author: asOptionalString(value['author']),
    tags: asStringArray(value['tags']),
    sources: asStringArray(value['sources']),
    resource: asOptionalString(value['resource']),
    stale_after: asOptionalString(value['stale_after']),
    generated: asRecord(value['generated']) as OkfNestedMetadata | undefined,
    verified: asRecord(value['verified']) as OkfNestedMetadata | undefined,
    valid: errors.length === 0,
    yamlValid: true,
    errors,
    raw: parts.raw,
  };
}

export function updateOkf(content: string, metadata: OkfMetadata): string {
  const parts = splitFrontmatter(content);
  if (!metadata.yamlValid) return content;

  const yaml = parseDocument(parts.raw, { uniqueKeys: true, prettyErrors: false });
  if (yaml.errors.length) return content;
  if (parts.hasFrontmatter && !isRecord(yaml.toJS({ mapAsMap: false }))) return content;
  if (!metadata.type.trim()) return content;

  yaml.set('type', metadata.type.trim());
  for (const key of STRING_FIELDS) setOptional(yaml, key, metadata[key]);
  for (const key of ARRAY_FIELDS) {
    const entries = metadata[key]?.map(value => value.trim()).filter(Boolean) ?? [];
    if (entries.length) yaml.set(key, entries);
    else yaml.delete(key);
  }
  setNested(yaml, 'generated', metadata.generated, ['by', 'model']);
  setNested(yaml, 'verified', metadata.verified, ['by', 'at']);

  const serialized = yaml.toString({ lineWidth: 0 }).replace(/\n*$/, '\n');
  if (!parts.hasFrontmatter) return `---\n${serialized}---\n${content}`;
  const original = content.match(/^---[ \t]*\r?\n[\s\S]*?\r?\n---[ \t]*(?:\r?\n|$)/)?.[0];
  return original ? content.replace(original, `---\n${serialized}---\n`) : content;
}

function emptyMetadata(raw: string, errors: string[], yamlValid: boolean): OkfMetadata {
  return { type: '', valid: false, yamlValid, errors, raw };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function asRecord(value: unknown): Record<string, unknown> | undefined {
  return isRecord(value) ? value : undefined;
}

function asOptionalString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

function asLegacyString(value: unknown): string | undefined {
  return typeof value === 'string' || typeof value === 'number' ? String(value) : undefined;
}

function asStringArray(value: unknown): string[] | undefined {
  return Array.isArray(value) && value.every(item => typeof item === 'string') ? value : undefined;
}

function validateNested(value: Record<string, unknown>, field: 'generated' | 'verified', stringKeys: string[], errors: string[]): void {
  const nested = value[field];
  if (nested === undefined) return;
  if (!isRecord(nested)) {
    errors.push(`${field} must be a mapping.`);
    return;
  }
  for (const key of stringKeys) {
    if (nested[key] !== undefined && typeof nested[key] !== 'string') errors.push(`${field}.${key} must be a string.`);
  }
}

function setOptional(yaml: ReturnType<typeof parseDocument>, key: typeof STRING_FIELDS[number], value: string | undefined): void {
  const normalized = value?.trim();
  if (normalized) yaml.set(key, normalized);
  else yaml.delete(key);
}

function setNested(yaml: ReturnType<typeof parseDocument>, field: 'generated' | 'verified', metadata: OkfNestedMetadata | undefined, supportedKeys: string[]): void {
  const node = yaml.get(field, true) as { set?: (key: string, value: unknown) => void; delete?: (key: string) => boolean; toJSON?: () => unknown } | undefined;
  const current = asRecord(node?.toJSON?.());
  const next: Record<string, unknown> = { ...current };
  for (const key of supportedKeys) {
    const value = metadata?.[key];
    if (typeof value === 'string' && value.trim()) next[key] = value.trim();
    else delete next[key];
  }
  if (!Object.keys(next).length) { yaml.delete(field); return; }
  if (node?.set && node.delete && node.toJSON && current) {
    for (const key of supportedKeys) {
      if (Object.prototype.hasOwnProperty.call(next, key)) node.set(key, next[key]);
      else node.delete(key);
    }
  } else yaml.set(field, next);
}
