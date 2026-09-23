import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { WorkspaceComponent } from './workspace.component';
import { VaultReadService } from '../services/vault-read.service';
import { DocumentStoreService } from '../services/document-store.service';
import { DEFAULT_TEMPLATES } from '../data/default-templates';

describe('WorkspaceComponent', () => {
  it('loads an empty vault and reports a retryable tree error', async () => {
    const vault = { tree: vi.fn().mockReturnValueOnce(of([])).mockReturnValueOnce(throwError(() => new Error('offline'))), document: vi.fn() };
    await TestBed.configureTestingModule({ imports: [WorkspaceComponent], providers: [{ provide: VaultReadService, useValue: vault }] }).compileComponents();
    const fixture = TestBed.createComponent(WorkspaceComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Folders (0)');
    fixture.componentInstance.loadTree(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Could not load the vault');
    expect(fixture.nativeElement.querySelector('button[aria-label="Refresh tree"]')).not.toBeNull();
  });
  it('creates a document from a template without losing the current draft', async () => {
    const created = { path: 'new.md', name: 'new.md', content: DEFAULT_TEMPLATES[0].content, updatedAt: '', size: 1, revision: 'a'.repeat(64) };
    const vault = { tree: vi.fn().mockReturnValueOnce(of([])).mockReturnValueOnce(of([{ name: 'new.md', path: 'new.md', type: 'file', revision: created.revision, updatedAt: '', size: 1 }])),
      document: vi.fn(), createDocument: vi.fn().mockReturnValue(of(created)), saveDocument: vi.fn() };
    await TestBed.configureTestingModule({ imports: [WorkspaceComponent], providers: [{ provide: VaultReadService, useValue: vault }] }).compileComponents();
    const fixture = TestBed.createComponent(WorkspaceComponent); fixture.detectChanges();
    const workspace = fixture.componentInstance;
    workspace.openTemplate(DEFAULT_TEMPLATES[0]); workspace.createTemplate({ folder: '', name: 'new.md' }); fixture.detectChanges();
    expect(vault.createDocument).toHaveBeenCalledWith('new.md', DEFAULT_TEMPLATES[0].content);
    expect(TestBed.inject(DocumentStoreService).selectedPath()).toBe('new.md');
    expect(workspace.selectedTemplate()).toBeNull();
    expect(vault.tree).toHaveBeenCalledTimes(2);
  });
  it('asks for a fresh copy before overwriting a colliding template destination', async () => {
    const old = { path: 'existing.md', name: 'existing.md', content: '# Old', updatedAt: '', size: 5, revision: 'a'.repeat(64) };
    const replaced = { ...old, content: DEFAULT_TEMPLATES[0].content, revision: 'b'.repeat(64) };
    const vault = { tree: vi.fn().mockReturnValueOnce(of([{ name: 'existing.md', path: 'existing.md', type: 'file', revision: old.revision, updatedAt: '', size: 5 }]))
        .mockReturnValueOnce(of([{ name: 'existing.md', path: 'existing.md', type: 'file', revision: replaced.revision, updatedAt: '', size: 5 }])),
      document: vi.fn().mockReturnValue(of(old)),
      createDocument: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({ status: 412, error: { error: { code: 'PATH_EXISTS' } } }))),
      saveDocument: vi.fn().mockReturnValue(of(replaced)) };
    await TestBed.configureTestingModule({ imports: [WorkspaceComponent], providers: [{ provide: VaultReadService, useValue: vault }] }).compileComponents();
    const fixture = TestBed.createComponent(WorkspaceComponent); fixture.detectChanges();
    const workspace = fixture.componentInstance;
    const destination = { folder: '', name: 'existing.md' };
    workspace.openTemplate(DEFAULT_TEMPLATES[0]); workspace.createTemplate(destination);
    expect(workspace.templateCollision()).toBe(true);
    expect(vault.saveDocument).not.toHaveBeenCalled();
    workspace.reviewTemplateExisting(destination);
    expect(workspace.templateCurrentCopy()?.content).toBe('# Old');
    workspace.overwriteTemplate(destination);
    expect(vault.saveDocument).toHaveBeenCalledWith('existing.md', DEFAULT_TEMPLATES[0].content, old.revision);
    expect(TestBed.inject(DocumentStoreService).activeDraft()?.content).toBe(DEFAULT_TEMPLATES[0].content);
  });
});
