import { describe, expect, it } from 'vitest';
import { renderedHtmlBody, renderedHtmlDocument } from './rendered-export';

describe('renderedHtmlDocument', () => {
  it('omits frontmatter and sanitizes raw active content', () => {
    const html = renderedHtmlDocument('---\ntype: note\ntitle: sample\n---\n# Hello\n\n<script>window.bad=1</script><a href="javascript:alert(1)" onclick="bad()">bad</a>', 'a<&');
    expect(html).toContain('<title>a&lt;&amp;</title>');
    expect(html).toContain('<h1 id="hello">Hello</h1>');
    expect(html).not.toContain('<script>');
    expect(html).not.toContain('javascript:');
    expect(html).not.toContain('onclick');
    expect(html).not.toContain('type: note');
  });
  it('prepares sanitized rendered body for printing without document chrome', () => {
    const body = renderedHtmlBody('---\ntype: note\n---\n# Print me\n\n<script>bad()</script><a href="https://example.com">source</a>');
    expect(body).toContain('<h1 id="print-me">Print me</h1>');
    expect(body).toContain('target="_blank"');
    expect(body).toContain('rel="noopener noreferrer"');
    expect(body).not.toContain('type: note');
    expect(body).not.toContain('<script>');
  });
});
