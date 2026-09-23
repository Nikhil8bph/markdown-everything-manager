import { CdkTrapFocus } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { VaultDocument, VaultNode } from '../models/vault.model';

export type MutationKind = 'folder' | 'document' | 'rename' | 'delete';
export interface MutationSubmit { name: string }

@Component({ selector: 'app-mutation-dialog', standalone: true, imports: [CdkTrapFocus], templateUrl: './mutation-dialog.component.html', styleUrl: './mutation-dialog.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class MutationDialogComponent {
  readonly kind = input.required<MutationKind>();
  readonly node = input<VaultNode | null>(null);
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly confirmAction = output<MutationSubmit>();
  readonly dismiss = output<void>();
  readonly deleteItem = output<void>();
  readonly reviewExisting = output<void>();
  readonly overwrite = output<void>();
  readonly collision = input(false);
  readonly existing = input<VaultDocument | null>(null);
  readonly name = signal('');
  label(): string { return this.kind() === 'folder' ? 'New folder name' : this.kind() === 'document' ? 'New Markdown filename' : 'New name'; }
  title(): string { return this.kind() === 'folder' ? 'New Folder' : this.kind() === 'document' ? 'New Markdown File' : this.kind() === 'rename' ? 'Rename item' : 'Delete permanently'; }
  isDelete(): boolean { return this.kind() === 'delete'; }
  valid(): boolean { const n = this.name().trim(); return !!n && !n.includes('/') && !n.includes('\\') && !n.startsWith('.') && (this.kind() !== 'document' || /\.md$/i.test(n)); }
  change(event: Event): void { this.name.set((event.target as HTMLInputElement).value); }
  confirm(): void { if (this.isDelete() || this.valid()) this.confirmAction.emit({ name: this.name().trim() }); }
  keydown(event: KeyboardEvent): void { if (event.key === 'Escape' && !this.loading()) { event.preventDefault(); this.dismiss.emit(); } }
  backdrop(event: MouseEvent): void { if (event.target === event.currentTarget && !this.loading()) this.dismiss.emit(); }
}
