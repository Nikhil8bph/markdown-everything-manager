import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { flattenTree, VaultNode } from '../models/vault.model';

@Component({ selector: 'app-dashboard', standalone: true, templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class DashboardComponent {
  readonly tree = input.required<VaultNode[]>();
  readonly openFile = output<string>();
  readonly search = signal('');
  readonly summary = computed(() => flattenTree(this.tree()));
  readonly recent = computed(() => [...this.summary().files].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)).slice(0, 8));
  readonly matches = computed(() => {
    const query = this.search().trim().toLowerCase();
    return this.summary().files.filter(file => file.name.toLowerCase().includes(query) || file.folder.toLowerCase().includes(query));
  });
  onSearch(event: Event): void { this.search.set((event.target as HTMLInputElement).value); }
  size(bytes: number | undefined): string { return bytes === undefined ? '' : bytes < 1024 ? `${bytes} B` : `${(bytes / 1024).toFixed(1)} KB`; }
  date(value: string): string { return value ? new Date(value).toLocaleDateString() : ''; }
}
