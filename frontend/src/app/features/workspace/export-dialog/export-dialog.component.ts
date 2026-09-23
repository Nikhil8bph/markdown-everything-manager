import { CdkTrapFocus } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({ selector: 'app-export-dialog', standalone: true, imports: [CdkTrapFocus], templateUrl: './export-dialog.component.html', styleUrl: './export-dialog.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class ExportDialogComponent {
  readonly name = input.required<string>();
  readonly exportAction = output<'md' | 'html' | 'copy' | 'print'>();
  readonly dismiss = output<void>();
  choose(kind: 'md' | 'html' | 'copy' | 'print'): void { this.exportAction.emit(kind); }
  keydown(event: KeyboardEvent): void { if (event.key === 'Escape') { event.preventDefault(); this.dismiss.emit(); } }
}
