import { TestBed } from '@angular/core/testing';
import { HelpDialogComponent } from './help-dialog.component';

describe('HelpDialogComponent', () => {
  it('shows working shortcuts and switches to the cheat sheet', async () => {
    await TestBed.configureTestingModule({ imports: [HelpDialogComponent] }).compileComponents();
    const fixture = TestBed.createComponent(HelpDialogComponent); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Ctrl + F');
    fixture.componentInstance.tab.set('cheatsheet'); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Markdown Syntax');
    expect(fixture.nativeElement.textContent).toContain('Strikethrough');
  });
});
