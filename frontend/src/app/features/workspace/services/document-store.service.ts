import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Subject, take } from 'rxjs';
import { VaultDocument, VaultNode } from '../models/vault.model';
import { VaultReadService } from './vault-read.service';

export interface DocumentDraft {
  readonly document: VaultDocument;
  readonly content: string;
  readonly generation: number;
  readonly savedGeneration: number;
  readonly savingGeneration: number | null;
  readonly errorCode: string | null;
  readonly errorMessage: string | null;
  readonly serverCopy: VaultDocument | null;
}

@Injectable({ providedIn: 'root' })
export class DocumentStoreService {
  private readonly vault = inject(VaultReadService);
  private readonly drafts = signal<Record<string, DocumentDraft>>({});
  private readonly pendingReads = new Set<string>();
  private readonly pendingWrites = new Set<string>();
  private readonly queuedWrites = new Set<string>();
  private readonly timers = new Map<string, ReturnType<typeof setTimeout>>();
  readonly selectedPath = signal<string | null>(null);
  readonly mode = signal<'edit' | 'view'>('edit');
  readonly readError = signal<string | null>(null);
  readonly loading = signal(false);
  readonly activeDraft = computed(() => this.selectedPath() ? this.drafts()[this.selectedPath()!] ?? null : null);
  readonly saveStatus = computed(() => {
    const draft = this.activeDraft();
    if (!draft) return '';
    if (draft.errorCode === 'REVISION_CONFLICT') return 'Conflict';
    if (draft.errorCode) return 'Save failed';
    if (draft.generation > draft.savedGeneration) return draft.savingGeneration === draft.generation ? 'Saving…' : 'Unsaved';
    return 'Saved';
  });
  readonly saved = new Subject<string>();

  open(path: string): void {
    this.selectedPath.set(path);
    this.readError.set(null);
    if (this.drafts()[path]) { this.loading.set(false); return; }
    if (this.pendingReads.has(path)) { this.loading.set(true); return; }
    this.pendingReads.add(path);
    this.loading.set(true);
    this.vault.document(path).pipe(take(1)).subscribe({
      next: document => {
        this.pendingReads.delete(path);
        if (!this.drafts()[path]) this.update(path, {
          document, content: document.content, generation: 0, savedGeneration: 0,
          savingGeneration: null, errorCode: null, errorMessage: null, serverCopy: null,
        });
        if (this.selectedPath() === path) this.loading.set(false);
      },
      error: () => {
        this.pendingReads.delete(path);
        if (this.selectedPath() === path) {
          this.loading.set(false);
          this.readError.set('Could not open this file. Retry the read.');
        }
      },
    });
  }

  selectDashboard(): void { this.selectedPath.set(null); this.readError.set(null); this.loading.set(false); }
  setMode(mode: 'edit' | 'view'): void { this.mode.set(mode); }
  hasUnsavedDraft(path: string): boolean {
    const draft = this.drafts()[path];
    return !!draft && (draft.generation !== draft.savedGeneration || this.pendingWrites.has(path));
  }
  acceptCreatedDocument(document: VaultDocument): boolean {
    if (this.hasUnsavedDraft(document.path)) return false;
    this.clearTimer(document.path);
    this.update(document.path, { document, content: document.content, generation: 0, savedGeneration: 0,
      savingGeneration: null, errorCode: null, errorMessage: null, serverCopy: null });
    this.selectedPath.set(document.path);
    this.readError.set(null);
    this.loading.set(false);
    this.mode.set('edit');
    return true;
  }

  acceptMovedItem(from: string, moved: VaultNode): void {
    const updates = Object.entries(this.drafts()).filter(([path]) => path === from || (moved.type === 'folder' && path.startsWith(`${from}/`)));
    if (!updates.length) return;
    const selected = this.selectedPath();
    const nextSelected = selected === from ? moved.path : selected?.startsWith(`${from}/`) ? `${moved.path}${selected.slice(from.length)}` : selected;
    this.drafts.update(all => {
      const next = { ...all };
      for (const [oldPath, draft] of updates) {
        this.clearTimer(oldPath);
        delete next[oldPath];
        const newPath = oldPath === from ? moved.path : `${moved.path}${oldPath.slice(from.length)}`;
        const name = newPath.split('/').at(-1)!;
        const document: VaultDocument = { ...draft.document, name, path: newPath,
          revision: oldPath === from && moved.type === 'file' ? moved.revision : draft.document.revision,
          updatedAt: moved.updatedAt };
        const changed = { ...draft, document, generation: draft.generation + 1,
          savedGeneration: draft.savedGeneration + (draft.generation === draft.savedGeneration ? 1 : 0), errorCode: null, errorMessage: null, serverCopy: null };
        next[newPath] = changed;
      }
      return next;
    });
    if (nextSelected !== selected) this.selectedPath.set(nextSelected);
    for (const [oldPath] of updates) {
      const newPath = oldPath === from ? moved.path : `${moved.path}${oldPath.slice(from.length)}`;
      const draft = this.drafts()[newPath];
      if (draft && draft.generation !== draft.savedGeneration) this.schedule(newPath);
    }
  }

  edit(content: string): void {
    const path = this.selectedPath();
    const draft = path ? this.drafts()[path] : undefined;
    if (!path || !draft || draft.content === content) return;
    this.update(path, { ...draft, content, generation: draft.generation + 1 });
    this.schedule(path);
  }

  save(path = this.selectedPath()): void {
    if (!path) return;
    const draft = this.drafts()[path];
    if (!draft || draft.generation === draft.savedGeneration || draft.errorCode === 'REVISION_CONFLICT') return;
    this.clearTimer(path);
    if (this.pendingWrites.has(path)) { this.queuedWrites.add(path); return; }
    const generation = draft.generation;
    const content = draft.content;
    const revision = draft.document.revision;
    this.pendingWrites.add(path);
    this.update(path, { ...draft, savingGeneration: generation, errorCode: null, errorMessage: null });
    this.vault.saveDocument(path, content, revision).pipe(take(1)).subscribe({
      next: document => {
        this.pendingWrites.delete(path);
        const current = this.drafts()[path];
        if (!current) return;
        const hasNewerEdit = current.generation !== generation;
        this.update(path, {
          ...current, document,
          content: hasNewerEdit ? current.content : document.content,
          savedGeneration: generation, savingGeneration: null,
          errorCode: null, errorMessage: null, serverCopy: null,
        });
        this.saved.next(path);
        const queued = this.queuedWrites.delete(path);
        if (queued && hasNewerEdit) this.save(path);
      },
      error: (error: unknown) => {
        this.pendingWrites.delete(path);
        this.queuedWrites.delete(path);
        const current = this.drafts()[path];
        if (!current) return;
        const code = error instanceof HttpErrorResponse ? error.error?.error?.code ?? 'NETWORK_FAILURE' : 'NETWORK_FAILURE';
        const message = this.messageFor(code);
        this.update(path, { ...current, savingGeneration: null, errorCode: code, errorMessage: message });
      },
    });
  }

  reviewLatest(): void {
    const path = this.selectedPath();
    const draft = path ? this.drafts()[path] : undefined;
    if (!path || !draft || draft.errorCode !== 'REVISION_CONFLICT') return;
    this.vault.document(path).pipe(take(1)).subscribe({
      next: document => {
        const current = this.drafts()[path];
        if (current?.errorCode === 'REVISION_CONFLICT') this.update(path, { ...current, serverCopy: document });
      },
      error: () => {
        const current = this.drafts()[path];
        if (current) this.update(path, { ...current, errorMessage: 'Could not read the latest copy. Retry review.' });
      },
    });
  }

  useServerCopy(): void {
    const path = this.selectedPath();
    const draft = path ? this.drafts()[path] : undefined;
    if (!path || !draft?.serverCopy) return;
    const generation = draft.generation + 1;
    this.clearTimer(path);
    this.update(path, { ...draft, document: draft.serverCopy, content: draft.serverCopy.content,
      generation, savedGeneration: generation, savingGeneration: null,
      serverCopy: null, errorCode: null, errorMessage: null });
  }

  replaceServerCopy(): void {
    const path = this.selectedPath();
    const draft = path ? this.drafts()[path] : undefined;
    if (!path || !draft?.serverCopy) return;
    this.update(path, { ...draft, document: draft.serverCopy, serverCopy: null, errorCode: null, errorMessage: null });
    this.save(path);
  }

  private schedule(path: string): void {
    this.clearTimer(path);
    if (this.drafts()[path]?.errorCode === 'REVISION_CONFLICT') return;
    this.timers.set(path, setTimeout(() => { this.timers.delete(path); this.save(path); }, 1000));
  }
  private clearTimer(path: string): void {
    const timer = this.timers.get(path);
    if (timer) clearTimeout(timer);
    this.timers.delete(path);
  }
  private update(path: string, draft: DocumentDraft): void { this.drafts.update(all => ({ ...all, [path]: draft })); }
  private messageFor(code: string): string {
    switch (code) {
      case 'REVISION_CONFLICT': return 'This file changed on disk. Your unsaved edits are kept here.';
      case 'NOT_FOUND': return 'The file is missing. Your edits are kept here. Refresh the vault to locate it.';
      case 'INVALID_FRONTMATTER': return 'Fix malformed YAML frontmatter, then retry. Your edits are kept here.';
      case 'PAYLOAD_TOO_LARGE': return 'Reduce this document below 25 MB, then retry. Your edits are kept here.';
      default: return 'Save failed — edits kept here. Check the connection or storage, then retry.';
    }
  }
}
