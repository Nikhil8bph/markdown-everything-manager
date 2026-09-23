import { ChangeDetectionStrategy, Component, ElementRef, output, viewChild } from '@angular/core';
import { FormatAction } from '../utils/markdown-edit';

interface Tool { action: FormatAction; label: string; title: string }

@Component({ selector: 'app-format-toolbar', standalone: true, templateUrl: './format-toolbar.component.html',
  styleUrl: './format-toolbar.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class FormatToolbarComponent {
  readonly format = output<FormatAction>();
  readonly openTable = output<void>();
  readonly openFind = output<void>();
  readonly tableTrigger = viewChild<ElementRef<HTMLButtonElement>>('tableTrigger');
  focusTableTrigger(): void { this.tableTrigger()?.nativeElement.focus(); }
  readonly tools: readonly Tool[] = [
    { action: 'h1', label: 'H1', title: 'Heading 1' }, { action: 'h2', label: 'H2', title: 'Heading 2' },
    { action: 'h3', label: 'H3', title: 'Heading 3' }, { action: 'bold', label: 'B', title: 'Bold (Ctrl+B)' },
    { action: 'italic', label: 'I', title: 'Italic (Ctrl+I)' }, { action: 'strike', label: 'S̶', title: 'Strikethrough' },
    { action: 'inline_code', label: '</>', title: 'Inline code' }, { action: 'quote', label: '❝', title: 'Blockquote (Ctrl+Q)' },
    { action: 'ul', label: '• List', title: 'Bulleted list' }, { action: 'ol', label: '1. List', title: 'Numbered list' },
    { action: 'task', label: '☐', title: 'Task list' }, { action: 'link', label: '🔗', title: 'Insert link (Ctrl+K)' },
    { action: 'image', label: '▧', title: 'Insert image' }, { action: 'codeblock', label: '{ }', title: 'Code block' },
    { action: 'hr', label: '—', title: 'Horizontal rule' },
  ];
}
