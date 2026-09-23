import { TestBed } from '@angular/core/testing';
import { DEFAULT_TEMPLATES } from '../data/default-templates';
import { TemplateDialogComponent } from './template-dialog.component';

describe('TemplateDialogComponent', () => {
  it('offers Root and existing folders and emits a safe destination', async () => {
    await TestBed.configureTestingModule({ imports: [TemplateDialogComponent] }).compileComponents();
    const fixture = TestBed.createComponent(TemplateDialogComponent);
    fixture.componentRef.setInput('template', DEFAULT_TEMPLATES[0]);
    fixture.componentRef.setInput('tree', [{ name: 'Notes', path: 'Notes', type: 'folder', revision: 'a', updatedAt: '', children: [] }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('option').length).toBe(2);
    const created = vi.fn(); fixture.componentInstance.create.subscribe(created);
    fixture.componentInstance.submit();
    expect(created).toHaveBeenCalledWith({ folder: '', name: 'okf-v02-knowledge-concept.md' });
    fixture.componentInstance.name.set('../bad.md'); fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('.primary') as HTMLButtonElement).disabled).toBe(true);
  });
});
