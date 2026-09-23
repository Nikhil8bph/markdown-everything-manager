import { marked } from 'marked';
import { markdownBody } from './okf';

export function renderedHtmlDocument(markdown: string, title: string): string {
  const safeTitle = escapeHtml(title);
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${safeTitle}</title></head><body>${renderedHtmlBody(markdown)}</body></html>`;
}

export function renderedHtmlBody(markdown: string): string {
  const parsed = marked.parse(markdownBody(markdown), { async: false, gfm: true }) as string;
  const root = document.createElement('div'); root.innerHTML = parsed;
  root.querySelectorAll('script,iframe,object,embed,form,svg,math,base,meta[http-equiv]').forEach(element => element.remove());
  const headingCounts = new Map<string, number>();
  root.querySelectorAll('h1,h2,h3,h4,h5,h6').forEach(heading => {
    const base = slug(heading.textContent ?? '') || 'section'; const count = headingCounts.get(base) ?? 0;
    headingCounts.set(base, count + 1); heading.id = count ? `${base}-${count + 1}` : base;
  });
  root.querySelectorAll<HTMLAnchorElement>('a[href]').forEach(link => {
    if (/^https?:\/\//i.test(link.href)) { link.target = '_blank'; link.rel = 'noopener noreferrer'; }
  });
  root.querySelectorAll<HTMLElement>('*').forEach(element => {
    for (const attribute of Array.from(element.attributes)) {
      const name = attribute.name.toLowerCase();
      if (name.startsWith('on') || name === 'srcdoc' || name === 'style') element.removeAttribute(attribute.name);
      if ((name === 'href' || name === 'src') && !/^(https?:|mailto:|#|\/)/i.test(attribute.value)) element.removeAttribute(attribute.name);
    }
  });
  return root.innerHTML;
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]!);
}

function slug(value: string): string { return value.toLowerCase().trim().replace(/[^\p{L}\p{N}\s-]/gu, '').replace(/[\s-]+/g, '-'); }
