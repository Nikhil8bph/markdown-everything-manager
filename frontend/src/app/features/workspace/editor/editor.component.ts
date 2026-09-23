import { ChangeDetectionStrategy, Component, computed, effect, ElementRef, input, output, signal, viewChild } from '@angular/core';
import { DocumentDraft } from '../services/document-store.service';
import { findMatches, FormatAction, formatMarkdown, indentMarkdown, replaceAllLiteral, TextEdit } from '../utils/markdown-edit';

@Component({ selector: 'app-editor', standalone: true, templateUrl: './editor.component.html',
  styleUrl: './editor.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class EditorComponent {
  readonly draft = input.required<DocumentDraft>();
  readonly mode = input.required<'edit' | 'view'>();
  readonly saveStatus = input.required<string>();
  readonly fontSize = input(14);
  readonly wordWrap = input(true);
  readonly theme = input<'dark' | 'light'>('dark');
  readonly contentChange = output<string>();
  readonly cursorChange = output<{ start: number; end: number }>();
  readonly retrySave = output<void>();
  readonly reviewLatest = output<void>();
  readonly useServerCopy = output<void>();
  readonly replaceServerCopy = output<void>();
  readonly refreshTree = output<void>();
  readonly editor = viewChild<ElementRef<HTMLTextAreaElement>>('editor');
  readonly notice = viewChild<ElementRef<HTMLElement>>('notice');
  readonly findInput = viewChild<ElementRef<HTMLInputElement>>('findInput');
  readonly searchOpen = signal(false);
  readonly query = signal('');
  readonly replacement = signal('');
  readonly matchCase = signal(false);
  readonly currentMatchIndex = signal(0);
  readonly matches = computed(() => findMatches(this.draft().content, this.query(), this.matchCase()));
  readonly matchFeedback = computed(() => this.query() ? this.matches().length ? `${Math.min(this.currentMatchIndex() + 1, this.matches().length)} of ${this.matches().length}` : '0 matches' : '');
  private lastPath: string | null = null;
  private lastError: string | null = null;

  constructor() {
    effect(() => {
      const draft = this.draft();
      if (draft.document.path !== this.lastPath) {
        this.lastPath = draft.document.path;
        this.currentMatchIndex.set(0);
        setTimeout(() => { this.editor()?.nativeElement.focus(); this.trackCursor(); });
      }
      if (draft.errorCode && draft.errorCode !== this.lastError) {
        setTimeout(() => this.notice()?.nativeElement.focus());
      }
      this.lastError = draft.errorCode;
    });
  }

  onInput(event: Event): void { this.contentChange.emit((event.target as HTMLTextAreaElement).value); this.trackCursor(event.target as HTMLTextAreaElement); }
  trackCursor(element = this.editor()?.nativeElement): void { if (element) this.cursorChange.emit({ start: element.selectionStart, end: element.selectionEnd }); }
  isEditorFocused(): boolean { return this.editor()?.nativeElement === document.activeElement; }
  captureSelection(): { start: number; end: number; path: string } | null {
    const el = this.editor()?.nativeElement;
    return el ? { start: el.selectionStart, end: el.selectionEnd, path: this.draft().document.path } : null;
  }
  applyFormat(action: FormatAction, range?: { start: number; end: number; path: string }, customPayload?: string): void {
    const el = this.editor()?.nativeElement;
    if (!el || this.mode() !== 'edit' || (range && range.path !== this.draft().document.path)) return;
    const start = range?.start ?? el.selectionStart;
    const end = range?.end ?? el.selectionEnd;
    this.applyTextEdit(formatMarkdown(el.value, start, end, action, customPayload));
  }
  onEditorKeydown(event: KeyboardEvent): void {
    if (event.key === 'Tab') {
      event.preventDefault();
      const el = this.editor()?.nativeElement;
      if (el) this.applyTextEdit(indentMarkdown(el.value, el.selectionStart, el.selectionEnd, event.shiftKey));
    } else if (event.key === 'Escape' && this.searchOpen()) this.closeSearch();
  }
  openSearch(): void { if (this.mode() !== 'edit') return; this.searchOpen.set(true); setTimeout(() => this.findInput()?.nativeElement.focus()); }
  closeSearch(): void { this.searchOpen.set(false); this.editor()?.nativeElement.focus(); }
  onFindInput(event: Event): void { this.query.set((event.target as HTMLInputElement).value); this.currentMatchIndex.set(0); }
  onReplaceInput(event: Event): void { this.replacement.set((event.target as HTMLInputElement).value); }
  toggleMatchCase(): void { this.matchCase.update(value => !value); this.currentMatchIndex.set(0); }
  onFindKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') { event.preventDefault(); if (event.shiftKey) this.findPrevious(); else this.findNext(); }
    if (event.key === 'Escape') { event.preventDefault(); this.closeSearch(); }
  }
  findNext(): void { this.navigateMatch(1); }
  findPrevious(): void { this.navigateMatch(-1); }
  replaceCurrent(): void {
    const position = this.matches()[this.currentMatchIndex()];
    if (position === undefined) return;
    const content = this.draft().content;
    const value = content.slice(0, position) + this.replacement() + content.slice(position + this.query().length);
    this.applyTextEdit({ content: value, cursor: position + this.replacement().length });
    this.currentMatchIndex.set(Math.min(this.currentMatchIndex(), Math.max(0, findMatches(value, this.query(), this.matchCase()).length - 1)));
  }
  replaceAll(): void {
    if (!this.matches().length) return;
    const value = replaceAllLiteral(this.draft().content, this.query(), this.replacement(), this.matchCase());
    this.applyTextEdit({ content: value, cursor: 0 });
    this.currentMatchIndex.set(0);
  }
  private navigateMatch(direction: number): void {
    const matches = this.matches();
    if (!matches.length) return;
    const index = (this.currentMatchIndex() + direction + matches.length) % matches.length;
    this.currentMatchIndex.set(index);
    const el = this.editor()?.nativeElement;
    if (el) { el.focus(); el.setSelectionRange(matches[index], matches[index] + this.query().length); }
  }
  private applyTextEdit(edit: TextEdit): void {
    const el = this.editor()?.nativeElement;
    if (!el) return;
    el.value = edit.content;
    this.contentChange.emit(edit.content);
    setTimeout(() => {
      el.focus();
      el.setSelectionRange(edit.selectionStart ?? edit.cursor, edit.selectionEnd ?? edit.cursor);
      this.trackCursor(el);
    });
  }
  lineCount(): number { return this.draft().content.split('\n').length; }
  useLatest(): void {
    if (window.confirm('Replace your unsaved edits with the latest copy from disk? This cannot be undone.')) this.useServerCopy.emit();
  }
  replaceDisk(): void {
    if (window.confirm('Overwrite the latest disk copy with your current unsaved edits?')) this.replaceServerCopy.emit();
  }
}
