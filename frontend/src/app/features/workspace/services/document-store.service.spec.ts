import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { VaultDocument, VaultNode } from '../models/vault.model';
import { DocumentStoreService } from './document-store.service';
import { VaultReadService } from './vault-read.service';

const document = (path = 'Notes/a.md', content = '# Original', revision = 'a'.repeat(64)): VaultDocument => ({
  path, name: path.split('/').at(-1)!, content, revision, size: content.length, updatedAt: '2026-09-23T00:00:00Z',
});

describe('DocumentStoreService', () => {
  let store: DocumentStoreService;
  let vault: { document: ReturnType<typeof vi.fn>; saveDocument: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    vi.useFakeTimers();
    vault = { document: vi.fn().mockReturnValue(of(document())), saveDocument: vi.fn() };
    TestBed.configureTestingModule({ providers: [{ provide: VaultReadService, useValue: vault }] });
    store = TestBed.inject(DocumentStoreService);
    store.open('Notes/a.md');
  });
  afterEach(() => { vi.clearAllTimers(); vi.useRealTimers(); });

  it('debounces edits for one second and saves with the persisted revision', () => {
    vault.saveDocument.mockReturnValue(of(document('Notes/a.md', '---\ntype: concept\n---\n\n# New', 'b'.repeat(64))));
    store.edit('# N'); vi.advanceTimersByTime(700); store.edit('# New');
    vi.advanceTimersByTime(999);
    expect(vault.saveDocument).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(vault.saveDocument).toHaveBeenCalledWith('Notes/a.md', '# New', 'a'.repeat(64));
    expect(store.saveStatus()).toBe('Saved');
    expect(store.activeDraft()?.content).toContain('type: concept');
    expect(store.activeDraft()?.document.revision).toBe('b'.repeat(64));
  });

  it('serializes overlapping saves and never marks a newer edit saved by an older response', () => {
    const first = new Subject<VaultDocument>();
    const second = new Subject<VaultDocument>();
    vault.saveDocument.mockReturnValueOnce(first).mockReturnValueOnce(second);
    store.edit('# First'); store.save();
    expect(store.saveStatus()).toBe('Saving…');
    store.edit('# Second'); store.save();
    expect(vault.saveDocument).toHaveBeenCalledTimes(1);
    expect(store.saveStatus()).toBe('Unsaved');
    first.next(document('Notes/a.md', '# First', 'b'.repeat(64))); first.complete();
    expect(store.activeDraft()?.content).toBe('# Second');
    expect(store.saveStatus()).toBe('Saving…');
    expect(vault.saveDocument).toHaveBeenNthCalledWith(2, 'Notes/a.md', '# Second', 'b'.repeat(64));
    second.next(document('Notes/a.md', '# Second', 'c'.repeat(64))); second.complete();
    expect(store.saveStatus()).toBe('Saved');
  });

  it('keeps a failed buffer editable and retries with the same revision', () => {
    vault.saveDocument.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500, error: { error: { code: 'STORAGE_FAILURE' } } })))
      .mockReturnValueOnce(of(document('Notes/a.md', '# Mine', 'b'.repeat(64))));
    store.edit('# Mine'); store.save();
    expect(store.activeDraft()?.content).toBe('# Mine');
    expect(store.saveStatus()).toBe('Save failed');
    store.save();
    expect(vault.saveDocument).toHaveBeenNthCalledWith(2, 'Notes/a.md', '# Mine', 'a'.repeat(64));
    expect(store.saveStatus()).toBe('Saved');
  });

  it('requires a fresh read and explicit replacement after a stale revision', () => {
    vault.saveDocument.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 412, error: { error: { code: 'REVISION_CONFLICT' } } })))
      .mockReturnValueOnce(of(document('Notes/a.md', '# Mine', 'c'.repeat(64))));
    store.edit('# Mine'); store.save();
    store.save();
    expect(vault.saveDocument).toHaveBeenCalledTimes(1);
    expect(store.activeDraft()?.content).toBe('# Mine');
    expect(store.saveStatus()).toBe('Conflict');
    vault.document.mockReturnValueOnce(of(document('Notes/a.md', '# Theirs', 'b'.repeat(64))));
    store.reviewLatest();
    expect(store.activeDraft()?.serverCopy?.content).toBe('# Theirs');
    expect(store.activeDraft()?.content).toBe('# Mine');
    store.replaceServerCopy();
    expect(vault.saveDocument).toHaveBeenNthCalledWith(2, 'Notes/a.md', '# Mine', 'b'.repeat(64));
    expect(store.saveStatus()).toBe('Saved');
  });

  it('keeps unsaved drafts through file, dashboard, and mode switches', () => {
    store.edit('# Pending');
    store.setMode('view');
    store.open('Notes/b.md');
    store.selectDashboard();
    store.open('Notes/a.md');
    expect(store.activeDraft()?.content).toBe('# Pending');
    expect(store.mode()).toBe('view');
    expect(store.saveStatus()).toBe('Unsaved');
    expect(vault.document).toHaveBeenCalledTimes(2);
  });

  it('does not let an older file read replace a newer selection', () => {
    const pending = new Subject<VaultDocument>();
    vault.document.mockReturnValueOnce(pending).mockReturnValueOnce(of(document('Notes/c.md', '# C')));
    store.open('Notes/b.md'); store.open('Notes/c.md');
    pending.next(document('Notes/b.md', '# B')); pending.complete();
    expect(store.selectedPath()).toBe('Notes/c.md');
    expect(store.activeDraft()?.content).toBe('# C');
  });

  it('keeps the selected file stable when another file finishes saving', () => {
    const pending = new Subject<VaultDocument>();
    vault.saveDocument.mockReturnValue(pending);
    vault.document.mockImplementation((path: string) => of(document(path)));
    store.edit('# A changed'); store.save();
    store.open('Notes/b.md');
    pending.next(document('Notes/a.md', '# A changed', 'b'.repeat(64))); pending.complete();
    expect(store.selectedPath()).toBe('Notes/b.md');
    expect(store.activeDraft()?.document.path).toBe('Notes/b.md');
    store.open('Notes/a.md');
    expect(store.activeDraft()?.content).toBe('# A changed');
    expect(store.saveStatus()).toBe('Saved');
  });

  it('keeps a dirty open document selected and saveable after renaming its file', () => {
    store.edit('# Pending rename');
    const moved: VaultNode = { name: 'renamed.md', path: 'Notes/renamed.md', type: 'file', updatedAt: 'now', revision: 'b'.repeat(64) };
    store.acceptMovedItem('Notes/a.md', moved);
    expect(store.selectedPath()).toBe('Notes/renamed.md');
    expect(store.activeDraft()?.content).toBe('# Pending rename');
    expect(store.activeDraft()?.document.revision).toBe('b'.repeat(64));
    expect(store.saveStatus()).toBe('Unsaved');
  });

  it('updates selected descendant paths after renaming a folder', () => {
    store.acceptMovedItem('Notes', { name: 'Archive', path: 'Archive', type: 'folder', updatedAt: 'now', revision: 'b'.repeat(64) });
    expect(store.selectedPath()).toBe('Archive/a.md');
    expect(store.activeDraft()?.document.path).toBe('Archive/a.md');
  });

  it('uses a reviewed server copy only after the owner chooses it', () => {
    vault.saveDocument.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 412, error: { error: { code: 'REVISION_CONFLICT' } } })));
    store.edit('# Mine'); store.save();
    vault.document.mockReturnValueOnce(of(document('Notes/a.md', '# Theirs', 'b'.repeat(64))));
    store.reviewLatest();
    expect(store.activeDraft()?.content).toBe('# Mine');
    store.useServerCopy();
    expect(store.activeDraft()?.content).toBe('# Theirs');
    expect(store.activeDraft()?.document.revision).toBe('b'.repeat(64));
    expect(store.saveStatus()).toBe('Saved');
  });
});
