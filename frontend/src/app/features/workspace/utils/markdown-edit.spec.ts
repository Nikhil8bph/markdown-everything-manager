import { buildMarkdownTable, findMatches, formatMarkdown, indentMarkdown, replaceAllLiteral } from './markdown-edit';

describe('Markdown editing utilities', () => {
  it('wraps selection and preserves surrounding text', () => {
    expect(formatMarkdown('before text after', 7, 11, 'bold')).toEqual({ content: 'before **text** after', cursor: 15 });
    expect(formatMarkdown('a\nb', 0, 3, 'quote').content).toBe('> a\n> b');
  });
  it('finds literal case-sensitive matches and replaces all literally', () => {
    expect(findMatches('A.a A.a', 'a', false)).toEqual([0, 2, 4, 6]);
    expect(findMatches('A.a A.a', 'a', true)).toEqual([2, 6]);
    expect(replaceAllLiteral('a.b a.b', 'a.b', '$&', false)).toBe('$& $&');
  });
  it('indents and outdents selected lines', () => {
    const indented = indentMarkdown('one\ntwo', 0, 7, false);
    expect(indented.content).toBe('  one\n  two');
    expect(indentMarkdown(indented.content, 0, indented.content.length, true).content).toBe('one\ntwo');
  });
  it('builds bounded aligned tables', () => {
    const table = buildMarkdownTable(2, 1, 'center');
    expect(table).toContain('| :---: | :---: |');
    expect(table.split('\n')).toHaveLength(3);
    expect(buildMarkdownTable(50, 50, 'right').split('\n')).toHaveLength(22);
  });
});
