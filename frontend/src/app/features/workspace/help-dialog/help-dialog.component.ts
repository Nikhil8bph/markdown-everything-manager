import { CdkTrapFocus } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core';

@Component({ selector: 'app-help-dialog', standalone: true, imports: [CdkTrapFocus],
  templateUrl: './help-dialog.component.html', styleUrl: './help-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush })
export class HelpDialogComponent {
  readonly dismiss = output<void>();
  readonly tab = signal<'shortcuts' | 'cheatsheet'>('shortcuts');
  readonly shortcuts = [
    ['Toggle Edit / View Mode', 'Ctrl + E'], ['Bold Text', 'Ctrl + B'], ['Italic Text', 'Ctrl + I'],
    ['Insert Link', 'Ctrl + K'], ['Find & Replace', 'Ctrl + F'], ['Save Document', 'Ctrl + S'],
    ['Indent Line/Selection', 'Tab'], ['Outdent Line/Selection', 'Shift + Tab'], ['Blockquote', 'Ctrl + Q'],
  ] as const;
  readonly syntax = [
    ['Heading 1', '# Title', 'Title'], ['Heading 2', '## Subheading', 'Subheading'],
    ['Bold', '**bold text**', 'bold text'], ['Italic', '*italic text*', 'italic text'],
    ['Strikethrough', '~~strike~~', 'strike'], ['Task List', '- [x] Done', '☑ Done'],
    ['Blockquote', '> Quoted note', 'Quoted note'], ['Code Block', '```js … ```', 'code'],
    ['Link', '[label](url)', 'label'], ['Table', '| A | B |', 'A / B'],
  ] as const;
  onKeydown(event: KeyboardEvent): void { if (event.key === 'Escape') { event.preventDefault(); this.dismiss.emit(); } }
}
