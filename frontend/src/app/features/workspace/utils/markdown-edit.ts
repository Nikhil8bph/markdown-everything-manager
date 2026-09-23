export type FormatAction = 'h1' | 'h2' | 'h3' | 'bold' | 'italic' | 'strike' | 'inline_code' | 'quote'
  | 'ul' | 'ol' | 'task' | 'link' | 'image' | 'codeblock' | 'hr' | 'table';
export type Alignment = 'left' | 'center' | 'right';
export interface TextEdit { content: string; cursor: number; selectionStart?: number; selectionEnd?: number }

export function formatMarkdown(value: string, start: number, end: number, action: FormatAction, customPayload?: string): TextEdit {
  const selected = value.slice(start, end);
  let replacement = '';
  let cursorOffset = 0;
  switch (action) {
    case 'bold': replacement = selected ? `**${selected}**` : '**bold text**'; cursorOffset = selected ? replacement.length : 2; break;
    case 'italic': replacement = selected ? `*${selected}*` : '*italic text*'; cursorOffset = selected ? replacement.length : 1; break;
    case 'strike': replacement = selected ? `~~${selected}~~` : '~~strikethrough~~'; cursorOffset = selected ? replacement.length : 2; break;
    case 'inline_code': replacement = selected ? `\`${selected}\`` : '`code`'; cursorOffset = selected ? replacement.length : 1; break;
    case 'quote': replacement = selected ? selected.split('\n').map(line => `> ${line}`).join('\n') : '> Quote here'; break;
    case 'h1': replacement = `\n# ${selected || 'Heading 1'}\n`; break;
    case 'h2': replacement = `\n## ${selected || 'Heading 2'}\n`; break;
    case 'h3': replacement = `\n### ${selected || 'Heading 3'}\n`; break;
    case 'ul': replacement = selected ? selected.split('\n').map(line => `- ${line}`).join('\n') : '\n- Item 1\n- Item 2\n- Item 3\n'; break;
    case 'ol': replacement = selected ? selected.split('\n').map((line, index) => `${index + 1}. ${line}`).join('\n') : '\n1. First item\n2. Second item\n3. Third item\n'; break;
    case 'task': replacement = selected ? selected.split('\n').map(line => `- [ ] ${line}`).join('\n') : '\n- [ ] Pending task\n- [x] Completed task\n'; break;
    case 'link': replacement = selected ? `[${selected}](https://example.com)` : '[link text](https://example.com)'; break;
    case 'image': replacement = '\n![Alt text](https://picsum.photos/seed/markdown/600/300)\n'; break;
    case 'codeblock': replacement = `\n\`\`\`typescript\n${selected || '// Insert code here'}\n\`\`\`\n`; break;
    case 'hr': replacement = '\n\n---\n\n'; break;
    case 'table': replacement = customPayload || '\n| Column 1 | Column 2 |\n| :--- | :--- |\n| Value 1 | Value 2 |\n'; break;
  }
  if (!cursorOffset) cursorOffset = replacement.length;
  return { content: value.slice(0, start) + replacement + value.slice(end), cursor: start + cursorOffset };
}

export function findMatches(content: string, query: string, matchCase: boolean): number[] {
  if (!query) return [];
  const haystack = matchCase ? content : content.toLowerCase();
  const needle = matchCase ? query : query.toLowerCase();
  const matches: number[] = [];
  let index = 0;
  while ((index = haystack.indexOf(needle, index)) !== -1) { matches.push(index); index += needle.length; }
  return matches;
}

export function replaceAllLiteral(content: string, query: string, replacement: string, matchCase: boolean): string {
  if (!query) return content;
  const positions = findMatches(content, query, matchCase);
  let result = content;
  for (const position of positions.reverse()) result = result.slice(0, position) + replacement + result.slice(position + query.length);
  return result;
}

export function indentMarkdown(value: string, start: number, end: number, outdent: boolean): TextEdit {
  if (start === end && !outdent) return { content: value.slice(0, start) + '  ' + value.slice(end), cursor: start + 2 };
  const lineStart = value.lastIndexOf('\n', start - 1) + 1;
  const lineEnd = value.indexOf('\n', end);
  const blockEnd = lineEnd === -1 ? value.length : lineEnd;
  const transformed = value.slice(lineStart, blockEnd).split('\n').map(line => outdent
    ? line.startsWith('  ') ? line.slice(2) : line.startsWith(' ') ? line.slice(1) : line
    : `  ${line}`).join('\n');
  return { content: value.slice(0, lineStart) + transformed + value.slice(blockEnd), cursor: lineStart + transformed.length,
    selectionStart: lineStart, selectionEnd: lineStart + transformed.length };
}

export function buildMarkdownTable(columns: number, rows: number, alignment: Alignment): string {
  const cols = Math.max(1, Math.min(10, Number.isFinite(columns) ? Math.trunc(columns) : 3));
  const count = Math.max(1, Math.min(20, Number.isFinite(rows) ? Math.trunc(rows) : 3));
  const header = `| ${Array.from({ length: cols }, (_, i) => `Header ${i + 1}`).join(' | ')} |`;
  const marker = alignment === 'center' ? ':---:' : alignment === 'right' ? '---:' : ':---';
  const separator = `| ${Array.from({ length: cols }, () => marker).join(' | ')} |`;
  const body = Array.from({ length: count }, (_, row) => `| ${Array.from({ length: cols }, (_, col) => `Item ${row + 1},${col + 1}`).join(' | ')} |`);
  return [header, separator, ...body].join('\n');
}
