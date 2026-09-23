import { TestBed } from '@angular/core/testing';
import { FormatToolbarComponent } from './format-toolbar.component';

describe('FormatToolbarComponent', () => {
  it('exposes source actions with accessible names and emits selected action', async () => {
    await TestBed.configureTestingModule({ imports: [FormatToolbarComponent] }).compileComponents();
    const fixture = TestBed.createComponent(FormatToolbarComponent); fixture.detectChanges();
    const action = vi.fn(); fixture.componentInstance.format.subscribe(action);
    (fixture.nativeElement.querySelector('[aria-label="Bold (Ctrl+B)"]') as HTMLButtonElement).click();
    expect(action).toHaveBeenCalledWith('bold');
    expect(fixture.nativeElement.querySelector('[aria-label="Insert table"]')).not.toBeNull();
  });
});
