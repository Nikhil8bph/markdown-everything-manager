import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, take } from 'rxjs';
import { DashboardComponent } from '../dashboard/dashboard.component';
import { TemplateItem } from '../data/default-templates';
import { EditorComponent } from '../editor/editor.component';
import { ExplorerComponent } from '../explorer/explorer.component';
import { FormatToolbarComponent } from '../format-toolbar/format-toolbar.component';
import { HelpDialogComponent } from '../help-dialog/help-dialog.component';
import { VaultDocument, VaultNode } from '../models/vault.model';
import { DocumentStoreService } from '../services/document-store.service';
import { VaultReadService } from '../services/vault-read.service';
import { TableDialogComponent } from '../table-dialog/table-dialog.component';
import { TemplateDestination, TemplateDialogComponent } from '../template-dialog/template-dialog.component';
import { FormatAction } from '../utils/markdown-edit';
import { MutationDialogComponent, MutationKind } from '../mutation-dialog/mutation-dialog.component';
import { UploadDialogComponent, UploadRequest } from '../upload-dialog/upload-dialog.component';
import { ViewerComponent } from '../viewer/viewer.component';
import { MetadataDialogComponent } from '../metadata-dialog/metadata-dialog.component';
import { OkfMetadata, parseOkf, updateOkf } from '../utils/okf';
import { ExportDialogComponent } from '../export-dialog/export-dialog.component';
import { PreferencesDialogComponent } from '../preferences-dialog/preferences-dialog.component';
import { cursorPosition, metrics } from '../utils/document-metrics';
import { renderedHtmlBody, renderedHtmlDocument } from '../utils/rendered-export';
import { DEFAULT_PREFERENCES, PREFERENCES_KEY, Preferences, readPreferences } from '../utils/preferences';

@Component({ selector: 'app-workspace', standalone: true, imports: [DashboardComponent, ExplorerComponent, EditorComponent,
  FormatToolbarComponent, TableDialogComponent, TemplateDialogComponent, HelpDialogComponent, MutationDialogComponent, UploadDialogComponent, ViewerComponent, MetadataDialogComponent, ExportDialogComponent, PreferencesDialogComponent],
  host: { '(document:keydown)': 'onShortcut($event)' },
  templateUrl: './workspace.component.html', styleUrl: './workspace.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class WorkspaceComponent implements OnInit {
  private readonly vault = inject(VaultReadService);
  readonly store = inject(DocumentStoreService);
  readonly tree = signal<VaultNode[]>([]);
  readonly loadingTree = signal(true);
  readonly treeError = signal<string | null>(null);
  readonly selectedPath = this.store.selectedPath;
  readonly announcement = signal('');
  readonly drawerOpen = signal(false);
  readonly tableOpen = signal(false);
  readonly helpOpen = signal(false);
  readonly selectedTemplate = signal<TemplateItem | null>(null);
  readonly templateLoading = signal(false);
  readonly templateError = signal<string | null>(null);
  readonly templateCollision = signal(false);
  readonly templateCurrentCopy = signal<VaultDocument | null>(null);
  readonly mutation = signal<MutationKind | null>(null);
  readonly mutationNode = signal<VaultNode | null>(null);
  readonly mutationError = signal<string | null>(null);
  readonly mutationLoading = signal(false);
  readonly mutationCollision = signal(false);
  readonly mutationExisting = signal<VaultDocument | null>(null);
  readonly mutationTargetPath = signal<string | null>(null);
  readonly uploadOpen = signal(false);
  readonly uploadLoading = signal(false);
  readonly uploadError = signal<string | null>(null);
  readonly uploadResults = signal<import('../models/vault.model').UploadResult[]>([]);
  readonly metadataOpen = signal(false);
  readonly exportOpen = signal(false); readonly preferencesOpen = signal(false); readonly preferences = signal<Preferences>(readPreferences());
  readonly cursor = signal({ line: 1, column: 1 });
  readonly printContent = signal(''); readonly printMode = signal(false);
  readonly explorerTrigger = viewChild<ElementRef<HTMLButtonElement>>('explorerTrigger');
  readonly helpTrigger = viewChild<ElementRef<HTMLButtonElement>>('helpTrigger');
  readonly errorRetry = viewChild<ElementRef<HTMLButtonElement>>('errorRetry');
  readonly exportTrigger = viewChild<ElementRef<HTMLButtonElement>>('exportTrigger');
  readonly preferencesTrigger = viewChild<ElementRef<HTMLButtonElement>>('preferencesTrigger');
  readonly editorPane = viewChild(EditorComponent);
  readonly explorerPane = viewChild(ExplorerComponent);
  readonly formatToolbar = viewChild(FormatToolbarComponent);
  private treeRequest = 0;
  private tableRange: { start: number; end: number; path: string } | null = null;
  private templateTrigger: HTMLElement | null = null;
  private mutationTrigger: HTMLElement | null = null;
  private uploadTrigger: HTMLElement | null = null;
  private metadataTrigger: HTMLElement | null = null;
  private exportInvoker: HTMLElement | null = null;
  private preferencesInvoker: HTMLElement | null = null;

  constructor() {
    this.store.saved.pipe(takeUntilDestroyed()).subscribe(() => this.loadTree());
    if (typeof window.matchMedia === 'function') window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', () => { if (this.preferences().theme === 'system') this.applyDocumentTheme(); });
    effect(() => {
      const status = this.store.saveStatus();
      if (status) this.announcement.set(status === 'Save failed' ? 'Save failed. Edits kept here.' : status);
    });
    effect(() => {
      if (this.store.readError()) setTimeout(() => this.errorRetry()?.nativeElement.focus());
    });
  }

  ngOnInit(): void { this.store.setMode(this.readViewMode()); this.applyDocumentTheme(); this.loadTree(); }
  loadTree(): void {
    const request = ++this.treeRequest;
    this.loadingTree.set(true); this.treeError.set(null);
    this.vault.tree().pipe(take(1)).subscribe({
      next: tree => {
        if (request !== this.treeRequest) return;
        this.tree.set(tree); this.loadingTree.set(false);
        const active = this.selectedPath();
        if (active && !this.containsFile(tree, active)) {
          this.showDashboard(); this.announcement.set('The selected file is no longer in the vault. Showing dashboard.');
        } else this.announcement.set(tree.length ? 'Vault loaded.' : 'Vault is empty.');
      },
      error: () => { if (request === this.treeRequest) { this.loadingTree.set(false); this.treeError.set('Could not load the vault. Check the connection and retry.'); this.announcement.set('Could not load the vault.'); } },
    });
  }
  openFile(path: string): void {
    this.closeDrawer(); this.store.open(path);
  }
  onShortcut(event: KeyboardEvent): void {
    if (this.tableOpen() || this.helpOpen() || this.selectedTemplate() || !this.selectedPath() || !(event.ctrlKey || event.metaKey) || event.altKey) return;
    const key = event.key.toLowerCase();
    if (key === 's') { event.preventDefault(); this.store.save(); return; }
    const target = event.target as HTMLElement;
    if (key === 'e' && this.store.activeDraft() && target.tagName !== 'INPUT' && target.tagName !== 'SELECT') {
      event.preventDefault(); this.store.setMode(this.store.mode() === 'edit' ? 'view' : 'edit'); return;
    }
    if (!this.editorPane()?.isEditorFocused() && !(key === 'f' && target.id === 'find-text')) return;
    if (key === 'f') { event.preventDefault(); this.editorPane()?.openSearch(); return; }
    const actions: Record<string, FormatAction> = { b: 'bold', i: 'italic', k: 'link', q: 'quote' };
    if (actions[key]) { event.preventDefault(); this.editorPane()?.applyFormat(actions[key]); }
  }
  format(action: FormatAction): void { this.editorPane()?.applyFormat(action); }
  openFind(): void { this.editorPane()?.openSearch(); }
  openTable(): void { this.tableRange = this.editorPane()?.captureSelection() ?? null; this.tableOpen.set(true); }
  insertTable(markdown: string): void {
    this.tableOpen.set(false);
    this.editorPane()?.applyFormat('table', this.tableRange ?? undefined, markdown);
    this.tableRange = null;
  }
  closeTable(): void { this.tableOpen.set(false); this.tableRange = null; setTimeout(() => this.formatToolbar()?.focusTableTrigger()); }
  closeHelp(): void { this.helpOpen.set(false); setTimeout(() => this.helpTrigger()?.nativeElement.focus()); }
  openTemplate(template: TemplateItem): void {
    this.templateTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    this.selectedTemplate.set(template);
    this.templateError.set(null); this.templateCollision.set(false); this.templateCurrentCopy.set(null);
  }
  closeTemplate(restoreFocus = true): void {
    if (this.templateLoading()) return;
    const templateId = this.selectedTemplate()?.id;
    this.selectedTemplate.set(null); this.templateError.set(null); this.templateCollision.set(false);
    this.templateCurrentCopy.set(null); this.templateLoading.set(false);
    if (restoreFocus && templateId) setTimeout(() => {
      if (this.templateTrigger?.isConnected && this.templateTrigger.tagName === 'BUTTON') this.templateTrigger.focus();
      else this.explorerPane()?.focusTemplate(templateId);
    }, 30);
  }
  createTemplate(destination: TemplateDestination): void {
    const template = this.selectedTemplate();
    if (!template || this.templateLoading()) return;
    const path = this.templatePath(destination);
    this.templateLoading.set(true); this.templateError.set(null); this.templateCollision.set(false); this.templateCurrentCopy.set(null);
    this.vault.createDocument(path, template.content).pipe(take(1)).subscribe({
      next: document => this.templateCreated(document),
      error: error => this.templateFailed(error),
    });
  }
  reviewTemplateExisting(destination: TemplateDestination): void {
    if (this.templateLoading()) return;
    const path = this.templatePath(destination);
    this.templateLoading.set(true); this.templateError.set(null); this.templateCurrentCopy.set(null);
    this.vault.document(path).pipe(take(1)).subscribe({
      next: document => { this.templateCurrentCopy.set(document); this.templateLoading.set(false); },
      error: error => this.templateFailed(error),
    });
  }
  overwriteTemplate(destination: TemplateDestination): void {
    const template = this.selectedTemplate();
    const path = this.templatePath(destination);
    const current = this.templateCurrentCopy();
    if (!template || !current || current.path !== path || this.templateLoading()) return;
    if (this.store.hasUnsavedDraft(path)) {
      this.templateError.set('This document has unsaved edits here. Save it or choose another name before overwriting.');
      return;
    }
    this.templateLoading.set(true); this.templateError.set(null);
    this.vault.saveDocument(path, template.content, current.revision).pipe(take(1)).subscribe({
      next: document => this.templateCreated(document),
      error: error => {
        if (error instanceof HttpErrorResponse && error.status === 412) {
          this.templateCurrentCopy.set(null);
          this.templateError.set('The disk copy changed again. Review its latest content before overwriting.');
          this.templateLoading.set(false);
        } else this.templateFailed(error);
      },
    });
  }
  private templateCreated(document: VaultDocument): void {
    this.templateLoading.set(false);
    if (!this.store.acceptCreatedDocument(document)) {
      this.templateError.set('This document has unsaved edits here. Save them before opening the new copy.');
      return;
    }
    this.closeTemplate(false); this.closeDrawer(); this.loadTree();
    this.announcement.set(`${document.name} created from template.`);
  }
  private templateFailed(error: unknown): void {
    this.templateLoading.set(false);
    const status = error instanceof HttpErrorResponse ? error.status : 0;
    const code = error instanceof HttpErrorResponse ? error.error?.error?.code : null;
    if (status === 412 && code === 'PATH_EXISTS') {
      this.templateCollision.set(true);
      this.templateError.set('A document already exists at this path. Review it before replacing it.');
    } else if (status === 404) this.templateError.set('The destination folder no longer exists. Refresh the vault and choose another folder.');
    else if (status === 413) this.templateError.set('This template exceeds the 25 MB document limit.');
    else if (status === 422) this.templateError.set('The template frontmatter is invalid. Choose a different template.');
    else this.templateError.set('Could not create the document. Check the connection and retry.');
  }
  private templatePath(destination: TemplateDestination): string { return destination.folder ? `${destination.folder}/${destination.name}` : destination.name; }
  showDashboard(): void { this.store.selectDashboard(); this.closeDrawer(); }
  closeDrawer(): void { if (this.drawerOpen()) { this.drawerOpen.set(false); setTimeout(() => { this.explorerTrigger()?.nativeElement.focus(); }); } }
  openMutation(kind: MutationKind, node: VaultNode|null = null): void { if(!this.mutation())this.mutationTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null; this.mutation.set(kind); this.mutationNode.set(node); this.mutationError.set(null); this.mutationCollision.set(false); this.mutationExisting.set(null); this.mutationTargetPath.set(null); }
  closeMutation(): void { if (!this.mutationLoading()) { this.mutation.set(null); this.mutationNode.set(null); this.mutationCollision.set(false); this.mutationExisting.set(null); this.mutationTargetPath.set(null); setTimeout(() => { if(this.mutationTrigger?.isConnected)this.mutationTrigger.focus();else this.explorerPane()?.focusTree(); }, 0); } }
  submitMutation(value: { name: string }): void {
    const kind = this.mutation(), node = this.mutationNode(); if (!kind || this.mutationLoading()) return;
    this.mutationLoading.set(true); this.mutationError.set(null);
    const parent = kind === 'rename' ? (node?.path.split('/').slice(0, -1).join('/') ?? '') : node?.type === 'folder' ? node.path : (node?.path.split('/').slice(0, -1).join('/') ?? '');
    const target = parent ? `${parent}/${value.name}` : value.name;
    this.mutationTargetPath.set(target);
    const request = kind === 'folder' ? this.vault.createFolder(target) : kind === 'document' ? this.vault.createDocument(target, '') : kind === 'rename' && node ? this.vault.moveItem(node.path, target, node.revision) : node ? this.vault.deleteItem(node.path, node.revision) : null;
    (request as Observable<VaultDocument | VaultNode | void> | null)?.pipe(take(1)).subscribe({ next: result => { this.mutationLoading.set(false); if(kind==='document' && result && 'content' in result){if(!this.store.acceptCreatedDocument(result)){this.mutationError.set('This file already has an unsaved draft. Save it before opening the new file.');return;}} if(kind==='rename' && node && result && 'type' in result)this.store.acceptMovedItem(node.path,result); this.closeMutation(); this.loadTree(); this.announcement.set(kind === 'delete' ? 'Item permanently deleted.' : 'Vault updated.'); }, error: (e: HttpErrorResponse) => { this.mutationLoading.set(false); if(kind==='document'&&e.status===412&&e.error?.error?.code==='PATH_EXISTS'){this.mutationCollision.set(true);this.mutationError.set(null);return;} this.mutationError.set(e.status === 409 ? 'That path already exists.' : e.status === 412 ? 'The item changed on disk. Refresh and retry.' : e.status===404 ? 'The source or destination no longer exists. Refresh and retry.' : 'Could not update the vault.'); } });
  }
  reviewMutationExisting():void {const path=this.mutationTargetPath();if(!path)return;this.mutationLoading.set(true);this.vault.document(path).pipe(take(1)).subscribe({next:doc=>{this.mutationExisting.set(doc);this.mutationLoading.set(false);},error:()=>{this.mutationLoading.set(false);this.mutationError.set('Could not read the existing file. Refresh and retry.');}});}
  overwriteMutation():void {const existing=this.mutationExisting();if(!existing||this.mutationLoading())return;this.mutationLoading.set(true);this.vault.saveDocument(existing.path,'',existing.revision).pipe(take(1)).subscribe({next:doc=>{this.mutationLoading.set(false);this.store.acceptCreatedDocument(doc);this.closeMutation();this.loadTree();this.announcement.set(`${doc.name} replaced and opened.`);},error:(e:HttpErrorResponse)=>{this.mutationLoading.set(false);this.mutationExisting.set(null);this.mutationError.set(e.status===412?'The file changed again. Review its latest content before replacing it.':'Could not replace the existing file.');}});}
  openUpload(): void { this.uploadTrigger=document.activeElement instanceof HTMLElement?document.activeElement:null;this.uploadOpen.set(true); this.uploadError.set(null); this.uploadResults.set([]); }
  submitUpload(request: UploadRequest): void {this.uploadLoading.set(true); this.vault.upload(request.folder, request.files).pipe(take(1)).subscribe({ next: results => {this.uploadLoading.set(false); this.uploadResults.set(results); this.loadTree(); this.announcement.set('Upload results received.'); }, error: () => {this.uploadLoading.set(false);this.uploadError.set('Upload failed. No automatic retry was attempted.') } }); }
  closeUpload(): void {if(!this.uploadLoading()){this.uploadOpen.set(false);setTimeout(()=>this.uploadTrigger?.isConnected&&this.uploadTrigger.focus(),0);} }
  currentMetadata(): OkfMetadata { return parseOkf(this.store.activeDraft()?.content ?? ''); }
  openMetadata(): void { this.metadataTrigger=document.activeElement instanceof HTMLElement?document.activeElement:null;this.metadataOpen.set(true); }
  closeMetadata(): void { this.metadataOpen.set(false);setTimeout(()=>this.metadataTrigger?.isConnected&&this.metadataTrigger.focus(),0); }
  applyMetadata(metadata: OkfMetadata): void { const draft = this.store.activeDraft(); if (draft) this.store.edit(updateOkf(draft.content, metadata)); this.announcement.set('Metadata updated in the draft.');this.closeMetadata(); }
  documentMetrics(){return metrics(this.store.activeDraft()?.content??'');}
  setCursor(position: { start: number; end: number }): void {
    this.cursor.set(cursorPosition(this.store.activeDraft()?.content ?? '', position.start));
  }
  setMode(mode: 'edit' | 'view'): void { this.store.setMode(mode); try { localStorage.setItem('markcraft.view-mode.v1', mode); } catch { this.announcement.set('View preference could not be saved in this browser.'); } }
  private readViewMode(): 'edit' | 'view' {
    try { return localStorage.getItem('markcraft.view-mode.v1') === 'view' ? 'view' : 'edit'; } catch { return 'edit'; }
  }
  openExport(): void { this.exportInvoker = document.activeElement instanceof HTMLElement ? document.activeElement : this.exportTrigger()?.nativeElement ?? null; this.exportOpen.set(true); }
  closeExport(): void { this.exportOpen.set(false); setTimeout(() => this.exportInvoker?.isConnected && this.exportInvoker.focus(), 0); }
  openPreferences(): void { this.preferencesInvoker = document.activeElement instanceof HTMLElement ? document.activeElement : this.preferencesTrigger()?.nativeElement ?? null; this.preferencesOpen.set(true); }
  closePreferences(): void { this.preferencesOpen.set(false); setTimeout(() => this.preferencesInvoker?.isConnected && this.preferencesInvoker.focus(), 0); }
  effectiveTheme(): 'dark' | 'light' { const setting = this.preferences().theme; return setting === 'system' ? (typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark') : setting; }
  private applyDocumentTheme(): void { document.documentElement.dataset['markcraftTheme'] = this.effectiveTheme(); }
  async exportDocument(kind:'md'|'html'|'copy'|'print'):Promise<void> {
    const draft=this.store.activeDraft(); if(!draft)return;
    const base=draft.document.name.replace(/\.md$/i,'');
    if(kind==='print') {
      try {
        this.printContent.set(renderedHtmlBody(draft.content)); this.printMode.set(true); this.closeExport();
        setTimeout(() => { try { window.print(); this.announcement.set('Print dialog opened.'); } catch { this.announcement.set('Could not open the print dialog. Try your browser print command.'); } finally { setTimeout(() => this.printMode.set(false), 0); } }, 0);
      } catch { this.printMode.set(false); this.announcement.set('Could not prepare the document for printing.'); this.closeExport(); }
      return;
    }
    if(kind==='copy') {
      try { if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable'); await navigator.clipboard.writeText(renderedHtmlDocument(draft.content, base)); this.announcement.set('Rendered HTML copied.'); }
      catch { this.announcement.set('Could not copy rendered HTML. Check clipboard access, then try again.'); }
      this.closeExport(); return;
    }
    try {
      const content=kind==='html'?renderedHtmlDocument(draft.content, base):draft.content;
      const blob=new Blob([content],{type:kind==='html'?'text/html':'text/markdown'});
      const link=document.createElement('a'); link.href=URL.createObjectURL(blob); link.download=`${base}.${kind}`; link.click();
      setTimeout(()=>URL.revokeObjectURL(link.href),0); this.announcement.set(`${link.download} download prepared.`);
    } catch { this.announcement.set(`Could not prepare the ${kind.toUpperCase()} download. Try again.`); }
    this.closeExport();
  }
  applyPreferences(prefs:Preferences):void {
    const normalized = { ...DEFAULT_PREFERENCES, ...prefs };
    this.preferences.set(normalized); this.applyDocumentTheme();
    try { localStorage.setItem(PREFERENCES_KEY,JSON.stringify(normalized)); }
    catch { this.announcement.set('Preferences apply for this session but could not be saved in this browser.'); }
  }
  toggleDrawer(): void { if (window.innerWidth >= 768) { this.applyPreferences({ ...this.preferences(), sidebar: !this.preferences().sidebar }); return; } if (this.drawerOpen()) this.closeDrawer(); else this.drawerOpen.set(true); }
  private containsFile(nodes: VaultNode[], path: string): boolean { return nodes.some(node => node.type === 'file' ? node.path === path : this.containsFile(node.children ?? [], path)); }
}
