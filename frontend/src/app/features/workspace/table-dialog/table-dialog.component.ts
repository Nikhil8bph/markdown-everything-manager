import { CdkTrapFocus } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, computed, output, signal } from '@angular/core';
import { Alignment, buildMarkdownTable } from '../utils/markdown-edit';

@Component({ selector: 'app-table-dialog', standalone: true, imports: [CdkTrapFocus],
  templateUrl: './table-dialog.component.html', styleUrl: './table-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush })
export class TableDialogComponent {
  readonly insert = output<string>();
  readonly dismiss = output<void>();
  readonly columns = signal(3);
  readonly rows = signal(3);
  readonly alignment = signal<Alignment>('left');
  readonly valid = computed(() => Number.isInteger(this.columns()) && this.columns() >= 1 && this.columns() <= 10
    && Number.isInteger(this.rows()) && this.rows() >= 1 && this.rows() <= 20);
  readonly preview = computed(() => this.valid() ? buildMarkdownTable(this.columns(), this.rows(), this.alignment()) : '');
  setColumns(event: Event): void { this.columns.set(Number((event.target as HTMLInputElement).value)); }
  setRows(event: Event): void { this.rows.set(Number((event.target as HTMLInputElement).value)); }
  confirm(): void { if (this.valid()) this.insert.emit(this.preview()); }
  onKeydown(event: KeyboardEvent): void { if (event.key === 'Escape') { event.preventDefault(); this.dismiss.emit(); } }
}
