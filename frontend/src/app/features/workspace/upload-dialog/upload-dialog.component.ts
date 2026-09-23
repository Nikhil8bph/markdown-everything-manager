import { CdkTrapFocus } from '@angular/cdk/a11y';
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { UploadFile, UploadResult, VaultNode } from '../models/vault.model';
export interface UploadRequest { folder: string; files: UploadFile[] }
@Component({ selector:'app-upload-dialog', standalone:true, imports:[CdkTrapFocus, CommonModule], templateUrl:'./upload-dialog.component.html', styleUrl:'./upload-dialog.component.scss', changeDetection:ChangeDetectionStrategy.OnPush })
export class UploadDialogComponent {
 readonly tree=input.required<VaultNode[]>(); readonly loading=input(false); readonly error=input<string|null>(null); readonly results=input<UploadResult[]>([]); readonly uploadFiles=output<UploadRequest>(); readonly dismiss=output<void>(); readonly folder=signal(''); readonly files=signal<UploadFile[]>([]); readonly fileErrors=signal<Record<string,string>>({}); readonly replacements=signal<Set<string>>(new Set()); readonly max=25000000;
 folders(nodes=this.tree()): VaultNode[]{ return nodes.flatMap(n=>n.type==='folder'?[n,...this.folders(n.children??[])]:[]); }
 chooseFolder(e:Event){this.folder.set((e.target as HTMLSelectElement).value);this.recheckCollisions()}
 async pick(e:Event){const list=Array.from((e.target as HTMLInputElement).files??[]); await this.add(list)}
 async drop(e:DragEvent){e.preventDefault(); await this.add(Array.from(e.dataTransfer?.files??[]))}
 async add(list:File[]){const next=[...this.files()]; const errors={...this.fileErrors()}; for(const file of list){if(!/\.md$/i.test(file.name)|| (file.type && file.type!=='text/markdown' && file.type!=='text/plain')){errors[file.name]='Only Markdown (.md) files are supported.';continue;} const content=await file.text(); const path=this.folder()?`${this.folder()}/${file.name}`:file.name; const existing=this.findFile(path); const current=next.findIndex(item=>item.name===file.name); const entry:UploadFile={name:file.name,content,expectedRevision:existing&&this.replacements().has(file.name)?existing.revision:null}; if(current>=0) next[current]=entry; else next.push(entry); if(existing&&!this.replacements().has(file.name)) errors[file.name]='This name already exists. Confirm replacement to continue.'; else delete errors[file.name];} this.fileErrors.set(errors); this.files.set(next)}
 private findFile(path:string,nodes=this.tree()):VaultNode|undefined { for(const node of nodes){if(node.path===path&&node.type==='file')return node; const child=node.type==='folder'?this.findFile(path,node.children??[]):undefined;if(child)return child;}return undefined;}
 confirmReplacement(name:string,accepted:boolean){const replacements=new Set(this.replacements()); if(accepted){replacements.add(name);this.files.update(items=>items.map(item=>{if(item.name!==name)return item;const existing=this.findFile(this.folder()?`${this.folder()}/${name}`:name);return {...item,expectedRevision:existing?.revision??null};}));}else{replacements.delete(name);this.files.update(items=>items.filter(item=>item.name!==name));}this.replacements.set(replacements);this.fileErrors.update(all=>{const next={...all};delete next[name];return next;});}
 private recheckCollisions(){this.replacements.set(new Set());this.files.update(files=>files.map(file=>({...file,expectedRevision:null})));this.fileErrors.update(all=>{const next={...all};for(const file of this.files()){const exists=this.findFile(this.folder()?`${this.folder()}/${file.name}`:file.name);if(exists)next[file.name]='This name already exists. Confirm replacement to continue.';else if(next[file.name]?.startsWith('This name'))delete next[file.name];}return next;});}
 bytes(file:UploadFile){return new TextEncoder().encode(file.content).length}
 remove(i:number){this.files.set(this.files().filter((_,index)=>index!==i))}
 total(){return this.files().reduce((n,f)=>n+new TextEncoder().encode(f.content).length,0)}
 valid(){return this.files().length>0&&this.total()<=this.max&&!this.loading()&&Object.keys(this.fileErrors()).length===0}
 send(){if(this.valid())this.uploadFiles.emit({folder:this.folder(),files:this.files()})}
 keydown(e:KeyboardEvent){if(e.key==='Escape'&&!this.loading()){e.preventDefault();this.dismiss.emit()}}
}
