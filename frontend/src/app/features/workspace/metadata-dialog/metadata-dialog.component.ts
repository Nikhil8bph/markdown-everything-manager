import { CdkTrapFocus } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, input, OnInit, output, signal } from '@angular/core';
import { OkfMetadata } from '../utils/okf';

interface MetadataForm {
  type: string;
  version: string;
  title: string;
  name: string;
  description: string;
  status: string;
  author: string;
  tags: string;
  sources: string;
  resource: string;
  stale_after: string;
  generatedBy: string;
  generatedModel: string;
  verifiedBy: string;
  verifiedAt: string;
}

type MetadataField = keyof MetadataForm;

const EMPTY_FORM: MetadataForm = {
  type: '', version: '', title: '', name: '', description: '', status: '', author: '', tags: '', sources: '', resource: '', stale_after: '',
  generatedBy: '', generatedModel: '', verifiedBy: '', verifiedAt: '',
};

@Component({
  selector: 'app-metadata-dialog', standalone: true, imports: [CdkTrapFocus],
  templateUrl: './metadata-dialog.component.html', styleUrl: './metadata-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MetadataDialogComponent implements OnInit {
  readonly metadata = input.required<OkfMetadata>();
  readonly apply = output<OkfMetadata>();
  readonly dismiss = output<void>();
  readonly values = signal<MetadataForm>({ ...EMPTY_FORM });

  ngOnInit(): void {
    const metadata = this.metadata();
    this.values.set({
      type: metadata.type,
      version: metadata.version ?? '',
      title: metadata.title ?? '',
      name: metadata.name ?? '',
      description: metadata.description ?? '',
      status: metadata.status ?? '',
      author: metadata.author ?? '',
      tags: metadata.tags?.join('\n') ?? '',
      sources: metadata.sources?.join('\n') ?? '',
      resource: metadata.resource ?? '',
      stale_after: metadata.stale_after ?? '',
      generatedBy: stringValue(metadata.generated?.['by']),
      generatedModel: stringValue(metadata.generated?.['model']),
      verifiedBy: stringValue(metadata.verified?.['by']),
      verifiedAt: stringValue(metadata.verified?.['at']),
    });
  }

  change(field: MetadataField, event: Event): void {
    const value = (event.target as HTMLInputElement | HTMLTextAreaElement).value;
    this.values.update(current => ({ ...current, [field]: value }));
  }

  submit(): void {
    const form = this.values();
    if (!this.metadata().yamlValid || !form.type.trim()) return;
    this.apply.emit({
      ...this.metadata(),
      type: form.type.trim(),
      version: optional(form.version),
      title: optional(form.title), name: optional(form.name), description: optional(form.description),
      status: optional(form.status), author: optional(form.author), tags: lines(form.tags), sources: lines(form.sources),
      resource: optional(form.resource), stale_after: optional(form.stale_after),
      generated: nested(this.metadata().generated, { by: optional(form.generatedBy), model: optional(form.generatedModel) }),
      verified: nested(this.metadata().verified, { by: optional(form.verifiedBy), at: optional(form.verifiedAt) }),
      valid: true, yamlValid: true, errors: [],
    });
  }

  keydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') { event.preventDefault(); this.dismiss.emit(); }
  }

}

function stringValue(value: unknown): string { return typeof value === 'string' ? value : ''; }
function optional(value: string): string | undefined { return value.trim() || undefined; }
function lines(value: string): string[] { return value.split(/\r?\n/).map(item => item.trim()).filter(Boolean); }
function nested(current: OkfMetadata['generated'], values: Record<string, string | undefined>): OkfMetadata['generated'] {
  const result = { ...current };
  for (const [key, value] of Object.entries(values)) {
    if (value) result[key] = value;
    else delete result[key];
  }
  return Object.keys(result).length ? result : undefined;
}
