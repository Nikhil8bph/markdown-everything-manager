import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExplorerComponent } from './explorer.component';
import { VaultNode } from '../models/vault.model';

describe('ExplorerComponent', () => {
  let fixture: ComponentFixture<ExplorerComponent>;
  const tree: VaultNode[] = [{ name: 'Notes', path: 'Notes', type: 'folder', updatedAt: '', revision: 'a', children: [
    { name: 'today.md', path: 'Notes/today.md', type: 'file', updatedAt: '', revision: 'b', size: 12 },
  ] }];
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ExplorerComponent] }).compileComponents();
    fixture = TestBed.createComponent(ExplorerComponent);
    fixture.componentRef.setInput('tree', tree);
    fixture.detectChanges();
  });
  it('keeps matching descendants under their parents and emits file selection', () => {
    fixture.nativeElement.querySelector('input').value = 'today';
    fixture.nativeElement.querySelector('input').dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('[role="treeitem"]').length).toBe(2);
    const selected = vi.fn(); fixture.componentInstance.openFile.subscribe(selected);
    (fixture.nativeElement.querySelectorAll('[role="treeitem"]')[1] as HTMLElement).click();
    expect(selected).toHaveBeenCalledWith('Notes/today.md');
  });
  it('supports arrow and Enter keyboard navigation', () => {
    const folder = fixture.nativeElement.querySelector('[role="treeitem"]') as HTMLElement;
    folder.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('[role="treeitem"]').length).toBe(2);
    folder.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    expect(document.activeElement).toBe(fixture.nativeElement.querySelectorAll('[role="treeitem"]')[1]);
  });
  it('shows source templates and emits a selected template', () => {
    fixture.componentInstance.activeTab.set('templates'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.template-card').length).toBe(4);
    const selected = vi.fn(); fixture.componentInstance.selectTemplate.subscribe(selected);
    (fixture.nativeElement.querySelector('.template-card') as HTMLButtonElement).click();
    expect(selected.mock.calls[0][0].id).toBe('okf-concept');
  });
});
