import { TestBed } from '@angular/core/testing';
import { EditorComponent } from './editor.component';
import { DocumentDraft } from '../services/document-store.service';

const base: DocumentDraft = {
  document: { path: 'a.md', name: 'a.md', content: '# A', updatedAt: '', size: 3, revision: 'a'.repeat(64) },
  content: '# A', generation: 0, savedGeneration: 0, savingGeneration: null,
  errorCode: null, errorMessage: null, serverCopy: null,
};

describe('EditorComponent', () => {
  it('emits raw Markdown edits without modifying the supplied draft', async () => {
    await TestBed.configureTestingModule({ imports: [EditorComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EditorComponent);
    fixture.componentRef.setInput('draft', base);
    fixture.componentRef.setInput('mode', 'edit');
    fixture.componentRef.setInput('saveStatus', 'Saved');
    fixture.detectChanges();
    const changed = vi.fn(); fixture.componentInstance.contentChange.subscribe(changed);
    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    textarea.value = '# Changed'; textarea.dispatchEvent(new Event('input'));
    expect(changed).toHaveBeenCalledWith('# Changed');
    expect(base.content).toBe('# A');
  });
  it('shows an actionable conflict while retaining the editable buffer', async () => {
    await TestBed.configureTestingModule({ imports: [EditorComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EditorComponent);
    fixture.componentRef.setInput('draft', { ...base, content: '# Mine', errorCode: 'REVISION_CONFLICT', errorMessage: 'This file changed on disk.', serverCopy: { ...base.document, content: '# Theirs' } });
    fixture.componentRef.setInput('mode', 'edit'); fixture.componentRef.setInput('saveStatus', 'Conflict');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('textarea').value).toBe('# Mine');
    expect(fixture.nativeElement.textContent).toContain('Review latest copy');
    expect(fixture.nativeElement.textContent).toContain('Overwrite disk copy');
  });
  it('formats the selected text through the same content-change output', async () => {
    await TestBed.configureTestingModule({ imports: [EditorComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EditorComponent);
    fixture.componentRef.setInput('draft', { ...base, content: 'before text after' });
    fixture.componentRef.setInput('mode', 'edit'); fixture.componentRef.setInput('saveStatus', 'Saved'); fixture.detectChanges();
    const changed = vi.fn(); fixture.componentInstance.contentChange.subscribe(changed);
    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    textarea.setSelectionRange(7, 11);
    fixture.componentInstance.applyFormat('bold');
    expect(changed).toHaveBeenCalledWith('before **text** after');
    expect(textarea.value).toBe('before **text** after');
  });
  it('replaces one match or all matches and honors Match Case', async () => {
    await TestBed.configureTestingModule({ imports: [EditorComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EditorComponent);
    fixture.componentRef.setInput('draft', { ...base, content: 'Alpha alpha Alpha' });
    fixture.componentRef.setInput('mode', 'edit'); fixture.componentRef.setInput('saveStatus', 'Saved'); fixture.detectChanges();
    const changed = vi.fn(); fixture.componentInstance.contentChange.subscribe(changed);
    const component = fixture.componentInstance;
    component.query.set('Alpha'); component.replacement.set('$&');
    expect(component.matches()).toEqual([0, 6, 12]);
    component.toggleMatchCase();
    expect(component.matches()).toEqual([0, 12]);
    component.replaceCurrent();
    expect(changed).toHaveBeenLastCalledWith('$& alpha Alpha');
    fixture.componentRef.setInput('draft', { ...base, content: '$& alpha Alpha' }); fixture.detectChanges();
    component.matchCase.set(false);
    component.replaceAll();
    expect(changed).toHaveBeenLastCalledWith('$& $& $&');
  });
});

  it('emits cursor offsets when the editor selection moves', async () => {
    await TestBed.configureTestingModule({ imports: [EditorComponent] }).compileComponents();
    const fixture = TestBed.createComponent(EditorComponent);
    fixture.componentRef.setInput('draft', { ...base, content: 'one\ntwo' });
    fixture.componentRef.setInput('mode', 'edit'); fixture.componentRef.setInput('saveStatus', 'Saved'); fixture.detectChanges();
    const cursor = vi.fn(); fixture.componentInstance.cursorChange.subscribe(cursor);
    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    textarea.setSelectionRange(5, 5); fixture.componentInstance.trackCursor();
    expect(cursor).toHaveBeenCalledWith({ start: 5, end: 5 });
  });
