import { CdkTrapFocus } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { TemplateItem } from '../data/default-templates';
import { flattenTree, VaultNode } from '../models/vault.model';
import { VaultDocument } from '../models/vault.model';

export interface TemplateDestination { folder: string; name: string }

@Component({ selector: 'app-template-dialog', standalone: true, imports: [CdkTrapFocus],
  templateUrl: './template-dialog.component.html', styleUrl: './template-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush })
export class TemplateDialogComponent {
  readonly template = input.required<TemplateItem>();
  readonly tree = input.required<VaultNode[]>();
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly collision = input(false);
  readonly currentCopy = input<VaultDocument | null>(null);
  readonly create = output<TemplateDestination>();
  readonly reviewExisting = output<TemplateDestination>();
  readonly overwrite = output<TemplateDestination>();
  readonly dismiss = output<void>();
  readonly folder = signal('');
  readonly name = signal('');
  readonly folders = computed(() => flattenTree(this.tree()).folders);
  readonly proposal = computed(() => this.folder() ? `${this.folder()}/${this.name().trim()}` : this.name().trim());
  readonly reviewed = computed(() => this.currentCopy()?.path === this.proposal());
  readonly valid = computed(() => {
    const name = this.name().trim();
    return !!name && !name.startsWith('.') && !name.includes('/') && !name.includes('\\')
      && !Array.from(name).some(char => char.charCodeAt(0) < 32 || char.charCodeAt(0) === 127) && /\.md$/i.test(name)
      && (this.folder() === '' || this.folders().some(node => node.path === this.folder()));
  });

  constructor() {
    effect(() => {
      const title = this.template().title;
      this.name.set(`${title.toLowerCase().replace(/[^\w\s-]/g, '').replace(/\s+/g, '-')}.md`);
    });
  }

  onFolder(event: Event): void { this.folder.set((event.target as HTMLSelectElement).value); }
  onName(event: Event): void { this.name.set((event.target as HTMLInputElement).value); }
  submit(): void { if (this.valid() && !this.loading()) this.create.emit({ folder: this.folder(), name: this.name().trim() }); }
  review(): void { if (this.valid() && !this.loading()) this.reviewExisting.emit({ folder: this.folder(), name: this.name().trim() }); }
  confirmOverwrite(): void { if (this.valid() && this.reviewed() && !this.loading()) this.overwrite.emit({ folder: this.folder(), name: this.name().trim() }); }
  onKeydown(event: KeyboardEvent): void { if (event.key === 'Escape' && !this.loading()) { event.preventDefault(); this.dismiss.emit(); } }
}
