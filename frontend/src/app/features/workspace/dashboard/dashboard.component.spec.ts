import { TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  it('searches file and folder names across the flattened vault', async () => {
    await TestBed.configureTestingModule({ imports: [DashboardComponent] }).compileComponents();
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.componentRef.setInput('tree', [{ name: 'Projects', path: 'Projects', type: 'folder', updatedAt: '', revision: '', children: [
      { name: 'Alpha.md', path: 'Projects/Alpha.md', type: 'file', updatedAt: '', revision: '', size: 10 },
      { name: 'Beta.md', path: 'Projects/Beta.md', type: 'file', updatedAt: '', revision: '', size: 12 },
    ] }]);
    fixture.detectChanges();
    fixture.componentInstance.search.set('Projects');
    expect(fixture.componentInstance.matches().map(file => file.path)).toEqual(['Projects/Alpha.md', 'Projects/Beta.md']);
    fixture.componentInstance.search.set('Beta');
    expect(fixture.componentInstance.matches().map(file => file.path)).toEqual(['Projects/Beta.md']);
    fixture.componentInstance.search.set('body-only phrase');
    expect(fixture.componentInstance.matches()).toEqual([]);
  });

  it('shows real zero counts for an empty vault', async () => {
    await TestBed.configureTestingModule({ imports: [DashboardComponent] }).compileComponents();
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.componentRef.setInput('tree', []);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Folders (0)');
    expect(fixture.nativeElement.textContent).toContain('Recent .md Files (0)');
  });
});
