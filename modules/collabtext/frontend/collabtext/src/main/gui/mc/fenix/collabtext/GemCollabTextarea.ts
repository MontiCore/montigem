/* (c) https://github.com/MontiCore/monticore */

import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, HostBinding, HostListener, inject, Injector, Input,
  OnDestroy, ViewChild, ViewEncapsulation
} from '@angular/core';
import { config, GemCollabTextareaComponent } from '../../../../../../build/gui2java2ts/mc/fenix/collabtext/GemCollabTextareaComponent';
import { GEM_COLLAB_TEXT_CURSOR_HOST, GemCollabTextCursor } from './GemCollabTextCursor';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  // The marks are created without Angular, encapsulated styles would not apply to them
  encapsulation: ViewEncapsulation.None,
  templateUrl: config.templateUrl,
})
export class GemCollabTextarea extends GemCollabTextareaComponent implements AfterViewInit, OnDestroy {
  @HostBinding('style.width')
  _width: string = '100%';

  @HostBinding('style.height')
  _height: string = '100%';

  override _value: string = '';
  override _placeholder: string = '';
  override _disabled: boolean = false;
  override _chunks: string = '';

  @ViewChild('input', { static: true })
  input: ElementRef<HTMLTextAreaElement>;

  @ViewChild('marks', { static: true })
  marks: ElementRef<HTMLElement>;

  // The cursors are rendered from templates of the using component, which do not see this component otherwise
  cursorInjector = Injector.create({
    providers: [{ provide: GEM_COLLAB_TEXT_CURSOR_HOST, useValue: this }],
    parent: inject(Injector),
  });

  private remoteCursors: GemCollabTextCursor[] = [];
  // Text and selection the using component knows, i.e., as of the last edit and selection it was told
  private reportedText = '';
  private reportedAnchor = 0;
  private reportedHead = 0;
  private drawingScheduled = false;
  private destroyed = false;
  private resizeObserver: ResizeObserver;

  @Input()
  override set value(text: string) {
    super.value = text;
    this.showText(text ?? '');
  }

  override get value(): string {
    return super.value;
  }

  @Input()
  override set chunks(chunks: string) {
    super.chunks = chunks;
    this.cursorChanged();
  }

  override get chunks(): string {
    return super.chunks;
  }

  ngAfterViewInit(): void {
    this.showText(this.value ?? '');
    this.resizeObserver = new ResizeObserver(() => this.alignMarks());
    this.resizeObserver.observe(this.input.nativeElement);
  }

  override ngOnDestroy(): void {
    this.destroyed = true;
    this.resizeObserver?.disconnect();
    super.ngOnDestroy();
  }

  addCursor(cursor: GemCollabTextCursor): void {
    this.remoteCursors.push(cursor);
    this.cursorChanged();
  }

  removeCursor(cursor: GemCollabTextCursor): void {
    this.remoteCursors = this.remoteCursors.filter(c => c !== cursor);
    this.cursorChanged();
  }

  // Several cursors and the text usually change together
  cursorChanged(): void {
    if (!this.drawingScheduled) {
      this.drawingScheduled = true;
      queueMicrotask(() => {
        this.drawingScheduled = false;
        if (!this.destroyed) {
          this.drawCursors();
        }
      });
    }
  }

  /** Replaces the text by one changed elsewhere, the selection stays at the characters it was at. */
  private showText(text: string): void {
    const input = this.input?.nativeElement;
    if (!input || input.value === text) {
      return;
    }
    const old = input.value;
    const maxCommon = Math.min(old.length, text.length);
    let prefix = 0;
    while (prefix < maxCommon && old[prefix] === text[prefix]) {
      prefix++;
    }
    let suffix = 0;
    while (suffix < maxCommon - prefix && old[old.length - 1 - suffix] === text[text.length - 1 - suffix]) {
      suffix++;
    }
    const move = (index: number) => {
      if (index <= prefix) {
        return index;
      }
      return index >= old.length - suffix ? index + text.length - old.length : text.length - suffix;
    };
    const start = move(input.selectionStart);
    const end = move(input.selectionEnd);
    const direction = input.selectionDirection;
    const scrollTop = input.scrollTop;
    input.value = text;
    input.setSelectionRange(start, end, direction);
    input.scrollTop = scrollTop;

    this.reportedText = text;
    // The using component moves the cursors along with the edits itself
    this.reportedAnchor = direction === 'backward' ? end : start;
    this.reportedHead = direction === 'backward' ? start : end;
    this.cursorChanged();
  }

  onInput(): void {
    const input = this.input.nativeElement;
    const text = input.value;
    const old = this.reportedText;
    // The change ends at the cursor. Otherwise, typing a character next to the same one is ambiguous.
    const maxSuffix = Math.min(old.length, text.length, text.length - input.selectionEnd);
    let suffix = 0;
    while (suffix < maxSuffix && old[old.length - 1 - suffix] === text[text.length - 1 - suffix]) {
      suffix++;
    }
    const maxPrefix = Math.min(old.length, text.length) - suffix;
    let prefix = 0;
    while (prefix < maxPrefix && old[prefix] === text[prefix]) {
      prefix++;
    }
    const inserted = text.substring(prefix, text.length - suffix);
    const deleteCount = old.length - suffix - prefix;
    this.reportedText = text;
    if (deleteCount > 0 || inserted.length > 0) {
      this.reportedAnchor = this.reportedHead = prefix + inserted.length;
      this.edit.emit(prefix + '|' + deleteCount + '|' + inserted);
      this.cursorChanged();
    }
    this.reportSelection();
  }

  @HostListener('document:selectionchange')
  reportSelection(): void {
    const input = this.input.nativeElement;
    if (document.activeElement !== input) {
      return;
    }
    const backward = input.selectionDirection === 'backward';
    const anchor = backward ? input.selectionEnd : input.selectionStart;
    const head = backward ? input.selectionStart : input.selectionEnd;
    if (anchor !== this.reportedAnchor || head !== this.reportedHead) {
      this.reportedAnchor = anchor;
      this.reportedHead = head;
      this.selectionChange.emit(anchor + '|' + head);
    }
  }

  alignMarks(): void {
    const input = this.input.nativeElement;
    const marks = this.marks.nativeElement;
    // Without the scrollbar, such that the text wraps as in the text area
    marks.style.width = input.clientWidth + 'px';
    marks.style.transform = `translate(${-input.scrollLeft}px, ${-input.scrollTop}px)`;
  }

  private drawCursors(): void {
    const text = this.input.nativeElement.value;
    const clamp = (index: number) => Math.max(0, Math.min(text.length, index ?? 0));
    const cursors = this.remoteCursors.map(c => ({
      label: c.label, color: c.color, head: clamp(c.head),
      from: Math.min(clamp(c.anchor), clamp(c.head)), to: Math.max(clamp(c.anchor), clamp(c.head)),
    }));
    // Start of each chunk, the shading stops at the end of the text if the chunks do not fit it
    const chunkStarts: number[] = [];
    let chunkEnd = 0;
    for (const length of (this.chunks ?? '').split(',').filter(l => l !== '')) {
      chunkStarts.push(clamp(chunkEnd));
      chunkEnd += Number(length);
    }
    const bounds = new Set<number>([0, text.length, ...chunkStarts]);
    cursors.forEach(c => bounds.add(c.from).add(c.to).add(c.head));
    const sorted = [...bounds].sort((a, b) => a - b);

    const nodes: Node[] = [];
    sorted.forEach((from, i) => {
      for (const cursor of cursors.filter(c => c.head === from)) {
        const caret = document.createElement('span');
        caret.className = 'gem-collab-textarea-caret';
        caret.style.borderColor = cursor.color;
        if (cursor.label) {
          const label = document.createElement('span');
          label.className = 'gem-collab-textarea-caret-label';
          label.style.background = cursor.color;
          label.textContent = cursor.label;
          caret.appendChild(label);
        }
        nodes.push(caret);
      }
      const to = sorted[i + 1] ?? from;
      const selection = cursors.find(c => c.from <= from && to <= c.to && c.from < c.to);
      const part = text.substring(from, to);
      if (!part) {
        return;
      }
      let node: Node = document.createTextNode(part);
      if (selection) {
        const selected = document.createElement('span');
        selected.style.background = `color-mix(in srgb, ${selection.color} 30%, transparent)`;
        selected.appendChild(node);
        node = selected;
      }
      const chunk = chunkStarts.filter(start => start <= from).length - 1;
      if (chunk >= 0 && from < chunkEnd) {
        const shaded = document.createElement('span');
        shaded.className = 'gem-collab-textarea-chunk-' + (chunk % 2 === 0 ? 'even' : 'odd');
        shaded.appendChild(node);
        node = shaded;
      }
      nodes.push(node);
    });
    // A final line break only starts a new line if something follows it
    nodes.push(document.createTextNode('​'));
    this.marks.nativeElement.replaceChildren(...nodes);
    this.alignMarks();
  }
}
