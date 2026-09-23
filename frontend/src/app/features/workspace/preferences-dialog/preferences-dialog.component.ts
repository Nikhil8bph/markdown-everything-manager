import { CdkTrapFocus } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { Preferences, normalizePreferences, readPreferences } from '../utils/preferences';

@Component({ selector: 'app-preferences-dialog', standalone: true, imports: [CdkTrapFocus], templateUrl: './preferences-dialog.component.html', styleUrl: './preferences-dialog.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class PreferencesDialogComponent {
  readonly initial = input<Preferences>(readPreferences());
  readonly changed = output<Preferences>();
  readonly dismiss = output<void>();
  readonly prefs = signal<Preferences>(this.initial());
  set<K extends keyof Preferences>(key: K, value: Preferences[K]): void {
    const next = normalizePreferences({ ...this.prefs(), [key]: value });
    this.prefs.set(next);
    this.changed.emit(next);
  }
  keydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') { event.preventDefault(); this.dismiss.emit(); }
  }
}
