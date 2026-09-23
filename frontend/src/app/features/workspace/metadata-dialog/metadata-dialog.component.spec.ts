import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OkfMetadata } from '../utils/okf';
import { MetadataDialogComponent } from './metadata-dialog.component';

describe('MetadataDialogComponent', () => {
  let fixture: ComponentFixture<MetadataDialogComponent>;
  const metadata: OkfMetadata = {
    type: 'note', version: '1', title: 'Title', name: 'Name', description: 'Description', status: 'draft', author: 'Nikhil', tags: ['a', 'b'],
    sources: ['source'], resource: 'resource', stale_after: '2030-01-01',
    generated: { by: 'bot', model: 'model', custom: 'preserve' }, verified: { by: 'person', at: 'today', extra: true },
    valid: true, yamlValid: true, errors: [], raw: 'custom: keep',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [MetadataDialogComponent] }).compileComponents();
    fixture = TestBed.createComponent(MetadataDialogComponent);
    fixture.componentRef.setInput('metadata', metadata);
    fixture.detectChanges();
  });

  it('exposes controls for every supported frontmatter field', () => {
    const ids = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>('input,textarea')).map(control => control.id);
    expect(ids).toEqual([
      'okf-type', 'okf-version', 'okf-title', 'okf-name', 'okf-description', 'okf-status', 'okf-author', 'okf-tags', 'okf-sources',
      'okf-resource', 'okf-stale-after', 'okf-generated-by', 'okf-generated-model', 'okf-verified-by', 'okf-verified-at',
    ]);
  });

  it('blocks conform for malformed YAML and explains how to recover', () => {
    fixture.componentRef.setInput('metadata', { ...metadata, valid: false, yamlValid: false, errors: ['Malformed YAML near line 3.'] });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button:last-child').disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('correct the raw frontmatter in Edit mode');
  });

  it('applies supported changes and carries unknown fields through the returned metadata', () => {
    let applied: OkfMetadata | undefined;
    fixture.componentInstance.apply.subscribe(value => applied = value);
    setValue(fixture.nativeElement, 'okf-title', 'Changed');
    setValue(fixture.nativeElement, 'okf-tags', 'one\nthree');
    setValue(fixture.nativeElement, 'okf-generated-by', 'new bot');
    fixture.componentInstance.submit();
    expect(applied).toMatchObject({
      title: 'Changed', tags: ['one', 'three'], generated: { by: 'new bot', custom: 'preserve' },
      verified: { extra: true }, valid: true,
    });
  });

  it('allows recovery of syntactically valid frontmatter that is missing type', () => {
    fixture.componentRef.setInput('metadata', { ...metadata, type: '', valid: false, errors: ['Required type is missing.'] });
    fixture.detectChanges();
    setValue(fixture.nativeElement, 'okf-type', 'concept');
    let applied: OkfMetadata | undefined;
    fixture.componentInstance.apply.subscribe(value => applied = value);
    fixture.componentInstance.submit();
    expect(applied?.type).toBe('concept');
  });

  it('emits dismiss for Escape', () => {
    let dismissed = false;
    fixture.componentInstance.dismiss.subscribe(() => dismissed = true);
    fixture.componentInstance.keydown(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(dismissed).toBe(true);
  });
});

function setValue(root: HTMLElement, id: string, value: string): void {
  const control = root.querySelector<HTMLInputElement | HTMLTextAreaElement>(`#${id}`)!;
  control.value = value;
  control.dispatchEvent(new Event('input', { bubbles: true }));
}
