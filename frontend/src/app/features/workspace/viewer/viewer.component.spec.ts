import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VaultDocument } from '../models/vault.model';
import { ViewerComponent } from './viewer.component';

describe('ViewerComponent', () => {
  let fixture: ComponentFixture<ViewerComponent>;
  const doc = (content: string): VaultDocument => ({ path: 'a.md', name: 'a.md', content, updatedAt: '', size: content.length, revision: 'x' });

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ViewerComponent] }).compileComponents();
    fixture = TestBed.createComponent(ViewerComponent);
    fixture.componentRef.setInput('document', doc('---\ntype: note\ntitle: Doc\ncustom: keep\n---\n# Hi\n\n- [ ] Do'));
    fixture.detectChanges();
  });

  it('does not render frontmatter as Markdown and derives matching heading IDs for the TOC', () => {
    fixture.componentRef.setInput('document', doc('---\ntype: note\ntitle: Doc\n---\n# Navigation target\n\n## Same\n\n## Same'));
    fixture.detectChanges();
    expect(fixture.componentInstance.html()).not.toContain('type: note');
    expect(fixture.componentInstance.headings().map(heading => heading.id)).toEqual(['navigation-target', 'same', 'same-2']);
    const rendered = fixture.nativeElement.querySelector('.rendered') as HTMLElement;
    expect(rendered.querySelector('h1')?.id).toBe(fixture.componentInstance.headings()[0].id);
    expect(rendered.querySelectorAll('h2')[1].id).toBe('same-2');
  });

  it('scrolls to and focuses the same rendered heading ID', () => {
    const heading = fixture.nativeElement.querySelector('h1') as HTMLElement;
    heading.scrollIntoView = vi.fn();
    fixture.componentInstance.scrollTo('hi');
    expect(heading.getAttribute('tabindex')).toBe('-1');
    expect(heading.scrollIntoView).toHaveBeenCalledWith({ block: 'start' });
    expect(document.activeElement).toBe(heading);
  });

  it('renders enabled task checkboxes and updates the matching draft marker', () => {
    const changed: string[] = [];
    fixture.componentInstance.contentChange.subscribe(content => changed.push(content));
    const checkbox = fixture.nativeElement.querySelector('.rendered input[type="checkbox"]') as HTMLInputElement;
    expect(checkbox).not.toBeNull();
    expect(checkbox.disabled).toBe(false);
    checkbox.checked = true;
    checkbox.dispatchEvent(new Event('change', { bubbles: true }));
    expect(changed.at(-1)).toContain('- [x] Do');
  });

  it('maps each visible task control to its ordered or unordered source marker', () => {
    fixture.componentRef.setInput('document', doc('# Tasks\n\n- [ ] First\n\n2. [x] Second'));
    fixture.detectChanges();
    const checkboxes = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLInputElement>('.rendered input[type="checkbox"]'));
    expect(checkboxes.map(checkbox => checkbox.checked)).toEqual([false, true]);
    const changed: string[] = [];
    fixture.componentInstance.contentChange.subscribe(content => changed.push(content));
    checkboxes[1].checked = false;
    checkboxes[1].dispatchEvent(new Event('change', { bubbles: true }));
    expect(changed.at(-1)).toContain('2. [ ] Second');
  });

  it('sets safe external-link behavior and strips executable markup and unsafe URLs', () => {
    fixture.componentRef.setInput('document', doc('# Links\n\n[Safe](https://example.test) [Unsafe](javascript:alert(1))\n\n<img src=x onerror="alert(1)"><script>window.__xss = 1</script>'));
    fixture.detectChanges();
    const root = fixture.nativeElement.querySelector('.rendered') as HTMLElement;
    const safe = root.querySelector('a[href^="https://"]') as HTMLAnchorElement;
    expect(safe.target).toBe('_blank');
    expect(safe.rel).toContain('noopener');
    expect(safe.getAttribute('aria-label')).toContain('opens in a new tab');
    expect(root.querySelector('a[href^="javascript:"]')).toBeNull();
    expect(root.querySelector('script')).toBeNull();
    expect(root.querySelector('img[onerror]')).toBeNull();
  });

  it('highlights supported fenced code and adds a keyboard-accessible copy action', () => {
    fixture.componentRef.setInput('document', doc('# Code\n\n```js\nconst answer = 42;\n```'));
    fixture.detectChanges();
    const root = fixture.nativeElement.querySelector('.rendered') as HTMLElement;
    expect(root.querySelector('code .token.keyword')).not.toBeNull();
    expect(root.querySelector('button.code-copy')?.getAttribute('aria-label')).toBe('Copy js code');
  });

  it('highlights Python, Java, and YAML code fences', () => {
    fixture.componentRef.setInput('document', doc('```python\ndef greet(name):\n    return name\n```\n\n```java\npublic class Note {}\n```\n\n```yaml\ntype: note\n```'));
    fixture.detectChanges();
    const root = fixture.nativeElement.querySelector('.rendered') as HTMLElement;
    expect(root.querySelector('code.language-python .token.keyword')).not.toBeNull();
    expect(root.querySelector('code.language-java .token.keyword')).not.toBeNull();
    expect(root.querySelector('code.language-yaml .token.atrule, code.language-yaml .token.key')).not.toBeNull();
  });

  it('announces copy failures instead of claiming success', async () => {
    const announce: string[] = [];
    fixture.componentInstance.announce.subscribe(message => announce.push(message));
    const button = document.createElement('button');
    button.className = 'code-copy';
    const pre = document.createElement('pre');
    pre.innerHTML = '<code>const x = 1;</code>';
    pre.append(button);
    await fixture.componentInstance.copyCode({ target: button } as unknown as Event);
    expect(announce.at(-1)).toContain('Could not copy code');
  });
});
