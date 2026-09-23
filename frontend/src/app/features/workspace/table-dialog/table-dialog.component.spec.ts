import { TestBed } from '@angular/core/testing';
import { TableDialogComponent } from './table-dialog.component';

describe('TableDialogComponent', () => {
  it('generates the selected table and prevents invalid insertion', async () => {
    await TestBed.configureTestingModule({ imports: [TableDialogComponent] }).compileComponents();
    const fixture = TestBed.createComponent(TableDialogComponent); fixture.detectChanges();
    const inserted = vi.fn(); fixture.componentInstance.insert.subscribe(inserted);
    fixture.componentInstance.columns.set(2); fixture.componentInstance.rows.set(1); fixture.componentInstance.alignment.set('center'); fixture.detectChanges();
    (fixture.nativeElement.querySelector('.primary') as HTMLButtonElement).click();
    expect(inserted).toHaveBeenCalledWith('| Header 1 | Header 2 |\n| :---: | :---: |\n| Item 1,1 | Item 1,2 |');
    fixture.componentInstance.columns.set(0); fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('.primary') as HTMLButtonElement).disabled).toBe(true);
  });
});
