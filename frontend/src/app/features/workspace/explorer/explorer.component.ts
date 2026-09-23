import { ChangeDetectionStrategy, Component, computed, ElementRef, input, output, signal, viewChild } from '@angular/core';
import { filterTree, VaultNode } from '../models/vault.model';
import { DEFAULT_TEMPLATES, TemplateItem } from '../data/default-templates';

@Component({
  selector: 'app-explorer', standalone: true, templateUrl: './explorer.component.html',
  styleUrl: './explorer.component.scss', changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExplorerComponent {
  readonly tree = input.required<VaultNode[]>();
  readonly selectedPath = input<string | null>(null);
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly openFile = output<string>();
  readonly showDashboard = output<void>();
  readonly refresh = output<void>();
  readonly retry = output<void>();
  readonly selectTemplate = output<TemplateItem>();
  readonly createFolder = output<void>(); readonly createDocument = output<void>(); readonly upload = output<void>(); readonly mutate = output<VaultNode>();
  readonly activeTab = signal<'files' | 'templates'>('files');
  readonly templates = DEFAULT_TEMPLATES;
  readonly filter = signal('');
  readonly expanded = signal<ReadonlySet<string>>(new Set());
  readonly filteredTree = computed(() => filterTree(this.tree(), this.filter()));
  readonly treeRoot = viewChild<ElementRef<HTMLElement>>('treeRoot');
  readonly templatePanel = viewChild<ElementRef<HTMLElement>>('templatePanel');

  focusTree(): void { this.treeRoot()?.nativeElement.focus(); }

  focusTemplate(id: string): void {
    const buttons = this.templatePanel()?.nativeElement.querySelectorAll<HTMLButtonElement>('button[data-template-id]') ?? [];
    Array.from(buttons).find(button => button.dataset['templateId'] === id)?.focus();
  }

  onFilter(event: Event): void { this.filter.set((event.target as HTMLInputElement).value); }
  toggle(path: string): void {
    const next = new Set(this.expanded());
    if (next.has(path)) next.delete(path); else next.add(path);
    this.expanded.set(next);
  }
  visibleNodes(nodes = this.filteredTree()): VaultNode[] {
    return nodes.flatMap(node => [node, ...(node.type === 'folder' && (this.expanded().has(node.path) || !!this.filter())
      ? this.visibleNodes(node.children ?? []) : [])]);
  }
  onTreeKey(event: KeyboardEvent): void {
    const rows = Array.from(this.treeRoot()?.nativeElement.querySelectorAll<HTMLElement>('[role="treeitem"]') ?? []);
    const index = rows.indexOf(event.target as HTMLElement);
    if (index < 0) return;
    const node = this.visibleNodes()[index];
    if (!node) return;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      rows[Math.max(0, Math.min(rows.length - 1, index + (event.key === 'ArrowDown' ? 1 : -1)))]?.focus();
    } else if (event.key === 'ArrowRight' && node.type === 'folder') {
      event.preventDefault(); if (!this.expanded().has(node.path)) this.toggle(node.path);
    } else if (event.key === 'ArrowLeft' && node.type === 'folder') {
      event.preventDefault(); if (this.expanded().has(node.path)) this.toggle(node.path);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      if (node.type === 'folder') this.toggle(node.path); else this.openFile.emit(node.path);
    }
  }
  select(node: VaultNode): void { if (node.type === 'folder') this.toggle(node.path); else this.openFile.emit(node.path); }
  formatSize(size: number | undefined): string { return size === undefined ? '' : size < 1024 ? `${size} B` : `${(size / 1024).toFixed(1)} KB`; }
}
