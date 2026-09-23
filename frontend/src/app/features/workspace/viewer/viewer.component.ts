import { AfterViewChecked, ChangeDetectionStrategy, Component, computed, ElementRef, input, output, signal, viewChild } from '@angular/core';
import * as Prism from 'prismjs';
import 'prismjs/components/prism-bash';
import 'prismjs/components/prism-css';
import 'prismjs/components/prism-java';
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-json';
import 'prismjs/components/prism-markup';
import 'prismjs/components/prism-markdown';
import 'prismjs/components/prism-python';
import 'prismjs/components/prism-sql';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-yaml';
import { marked } from 'marked';
import { VaultDocument } from '../models/vault.model';
import { markdownBody, parseOkf, OkfMetadata, updateOkf } from '../utils/okf';

interface HeadingItem { text: string; level: number; id: string }

@Component({
  selector: 'app-viewer', standalone: true, templateUrl: './viewer.component.html', styleUrl: './viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ViewerComponent implements AfterViewChecked {
  readonly document = input.required<VaultDocument>();
  readonly contentChange = output<string>();
  readonly openInspector = output<void>();
  readonly announce = output<string>();
  readonly contentRoot = viewChild<ElementRef<HTMLElement>>('contentRoot');
  readonly metadata = computed(() => parseOkf(this.document().content));
  readonly html = computed(() => this.sanitize(marked.parse(markdownBody(this.document().content), { async: false, gfm: true }) as string));
  readonly headings = computed<HeadingItem[]>(() => {
    const div = document.createElement('div');
    div.innerHTML = this.html();
    return Array.from(div.querySelectorAll<HTMLHeadingElement>('h1,h2,h3,h4,h5,h6'))
      .filter(heading => !!heading.textContent?.trim())
      .map(heading => ({ text: heading.textContent?.trim() ?? '', level: Number(heading.tagName.slice(1)), id: heading.id }));
  });
  readonly tocOpen = signal(true);
  private lastEnhancedHtml = '';

  ngAfterViewChecked(): void {
    const html = this.html();
    const root = this.contentRoot()?.nativeElement;
    if (!root || html === this.lastEnhancedHtml) return;
    this.lastEnhancedHtml = html;
    this.assignHeadingIds(root);
    this.configureLinks(root);
    this.configureTaskControls(root);
    this.addCodeCopyButtons(root);
  }

  scrollTo(id: string): void {
    const target = Array.from(this.contentRoot()?.nativeElement.querySelectorAll<HTMLElement>('[id]') ?? [])
      .find(element => element.id === id);
    if (!target) return;
    target.setAttribute('tabindex', '-1');
    target.scrollIntoView({ block: 'start' });
    target.focus({ preventScroll: true });
  }

  toggleToc(): void { this.tocOpen.update(open => !open); }

  toggleTask(event: Event): void {
    const checkbox = event.target as HTMLInputElement;
    if (!checkbox.matches('input[type="checkbox"][data-task-index]')) return;
    const index = Number(checkbox.dataset['taskIndex']);
    const lines = this.document().content.split('\n');
    let seen = 0;
    const next = lines.map(line => {
      if (/^\s*(?:(?:[-*+])|(?:\d+[.)]))\s+\[[ xX]\]/.test(line)) {
        if (seen++ === index) return line.replace(/\[[ xX]\]/, checkbox.checked ? '[x]' : '[ ]');
      }
      return line;
    }).join('\n');
    if (next === this.document().content) return;
    this.contentChange.emit(next);
    this.announce.emit(checkbox.checked ? 'Task completed. Draft marked unsaved.' : 'Task reopened. Draft marked unsaved.');
  }

  async copyCode(event: Event): Promise<void> {
    const target = event.target as HTMLElement;
    const button = target.closest<HTMLButtonElement>('button.code-copy');
    if (!button) return;
    const code = button.closest('pre')?.querySelector('code')?.textContent ?? '';
    try {
      if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable');
      await navigator.clipboard.writeText(code);
      this.announce.emit('Code copied.');
    } catch {
      this.announce.emit('Could not copy code. Select and copy it manually.');
    }
  }

  renderedKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('button.code-copy') || (event.key !== 'Enter' && event.key !== ' ')) return;
    event.preventDefault();
    void this.copyCode(event);
  }

  applyMetadata(metadata: OkfMetadata): void {
    this.contentChange.emit(updateOkf(this.document().content, metadata));
    this.announce.emit('Metadata updated in the draft.');
  }

  private sanitize(html: string): string {
    const root = document.createElement('div');
    root.innerHTML = html;
    root.querySelectorAll('script,iframe,object,embed,form,svg,math').forEach(element => element.remove());
    root.querySelectorAll<HTMLElement>('*').forEach(element => {
      for (const attribute of Array.from(element.attributes)) {
        const name = attribute.name.toLowerCase();
        if (name.startsWith('on') || name === 'srcdoc' || name === 'style') element.removeAttribute(attribute.name);
        if ((name === 'href' || name === 'src') && !/^(https?:|mailto:|#|\/)/i.test(attribute.value)) element.removeAttribute(attribute.name);
      }
    });
    this.assignHeadingIds(root);
    this.configureLinks(root);
    this.prepareTaskPlaceholders(root);
    this.highlightAndAddCopy(root);
    return root.innerHTML;
  }

  private assignHeadingIds(root: HTMLElement): void {
    const counts = new Map<string, number>();
    root.querySelectorAll('h1,h2,h3,h4,h5,h6').forEach(heading => {
      const base = slug(heading.textContent ?? '') || 'section';
      const count = counts.get(base) ?? 0;
      counts.set(base, count + 1);
      heading.id = count ? `${base}-${count + 1}` : base;
    });
  }

  private configureLinks(root: HTMLElement): void {
    root.querySelectorAll<HTMLAnchorElement>('a[href]').forEach(link => {
      if (/^https?:\/\//i.test(link.href)) {
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
        link.setAttribute('aria-label', `${link.textContent?.trim() || link.href} (opens in a new tab)`);
        if (!link.querySelector('.external-link-indicator')) {
          const indicator = document.createElement('span');
          indicator.className = 'external-link-indicator';
          indicator.setAttribute('aria-hidden', 'true');
          indicator.textContent = ' ↗';
          link.append(indicator);
        }
      }
    });
  }

  private configureTaskControls(root: HTMLElement): void {
    root.querySelectorAll<HTMLSpanElement>('span.markcraft-task-placeholder').forEach(placeholder => {
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.checked = placeholder.classList.contains('markcraft-task-placeholder--checked');
      const taskIndexClass = Array.from(placeholder.classList).find(name => /^markcraft-task-placeholder--\d+$/.test(name));
      checkbox.dataset['taskIndex'] = taskIndexClass?.split('--').at(-1) ?? placeholder.dataset['taskIndex'] ?? '0';
      const taskLabel = placeholder.parentElement?.textContent?.trim() || `task ${Number(checkbox.dataset['taskIndex']) + 1}`;
      checkbox.setAttribute('aria-label', `Complete task: ${taskLabel}`);
      placeholder.replaceWith(checkbox);
    });
    let index = 0;
    root.querySelectorAll<HTMLInputElement>('input[type="checkbox"]').forEach(checkbox => {
      if (!checkbox.hasAttribute('data-task-index')) checkbox.dataset['taskIndex'] = String(index);
      index++;
      checkbox.removeAttribute('disabled');
      checkbox.setAttribute('aria-label', checkbox.getAttribute('aria-label') ?? `Complete task ${index}`);
    });
  }

  private prepareTaskPlaceholders(root: HTMLElement): void {
    let index = 0;
    root.querySelectorAll<HTMLInputElement>('input[type="checkbox"][disabled]').forEach(checkbox => {
      const marker = document.createElement('span');
      marker.className = `markcraft-task-placeholder markcraft-task-placeholder--${index}${checkbox.checked ? ' markcraft-task-placeholder--checked' : ''}`;
      marker.dataset['taskIndex'] = String(index++);
      checkbox.replaceWith(marker);
    });
  }

  private highlightAndAddCopy(root: HTMLElement): void {
    root.querySelectorAll<HTMLElement>('pre').forEach(pre => {
      const code = pre.querySelector<HTMLElement>('code');
      if (!code) return;
      const languageClass = Array.from(code.classList).find(name => name.startsWith('language-'));
      const language = languageClass?.slice('language-'.length).toLowerCase() ?? '';
      const grammar = Prism.languages[language] ?? Prism.languages[language === 'js' ? 'javascript' : language === 'ts' ? 'typescript' : ''];
      if (grammar && language) code.innerHTML = Prism.highlight(code.textContent ?? '', grammar, language);
    });
  }

  private addCodeCopyButtons(root: HTMLElement): void {
    root.querySelectorAll<HTMLElement>('pre').forEach(pre => {
      if (!pre.querySelector('code') || pre.querySelector('button.code-copy')) return;
      const code = pre.querySelector('code');
      const languageClass = code && Array.from(code.classList).find(name => name.startsWith('language-'));
      const language = languageClass?.slice('language-'.length) ?? 'plain text';
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'code-copy';
      button.setAttribute('aria-label', `Copy ${language} code`);
      button.textContent = 'Copy code';
      pre.prepend(button);
    });
  }
}

function slug(text: string): string {
  return text.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '');
}
