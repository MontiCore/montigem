/* (c) https://github.com/MontiCore/monticore */

import {
  AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, forwardRef, HostBinding, HostListener, inject, Input,
  OnDestroy, ViewChild, ViewEncapsulation
} from '@angular/core';
import { config, GemCollabTextareaComponent } from '../../../../../../build/gui2java2ts/mc/fenix/collabtext/GemCollabTextareaComponent';
import { CommandManager, GemListUtil } from '@umlp/commonj2ts';
import { CollabText } from '../../../../../../build/umlpJava2ts/collabtextmodule/CollabText';
import { CollabTextChunk } from '../../../../../../build/umlpJava2ts/collabtextmodule/CollabTextChunk';
import { CollabTextCursor } from '../../../../../../build/umlpJava2ts/collabtextmodule/CollabTextCursor';
import { CollabTextAllObserver } from '../../../../../../build/umlpJava2ts/collabtextmodule/CollabTextAllObserver';
import { CollabTextChunkAllObserver } from '../../../../../../build/umlpJava2ts/collabtextmodule/CollabTextChunkAllObserver';
import { CollabTextCursorAllObserver } from '../../../../../../build/umlpJava2ts/collabtextmodule/CollabTextCursorAllObserver';
import { CollabTextModuleManager } from '../../../../../../build/umlpJava2ts/collabtextmodule/CollabTextModuleManager';
import { GEM_COLLAB_TEXT_CURSOR_HOST, GemCollabTextCursor } from './GemCollabTextCursor';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, GemCollabTextCursor],
  providers: [{ provide: GEM_COLLAB_TEXT_CURSOR_HOST, useExisting: forwardRef(() => GemCollabTextarea) }],
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

  override _placeholder: string = '';
  override _userName: string = '';
  override _userColor: string = '';
  override _disabled: boolean = false;
  override _showChunks: boolean = false;
  private chunkLengths: number[] = [];
  loaded = false;
  private viewReady = false;
  private binding = 0;
  private bindingTask: Promise<void> = Promise.resolve();
  private requestedText: CollabText;
  private refreshTask: Promise<void> = Promise.resolve();
  private refreshVersion = 0;
  private refreshScheduled = false;
  private boundText: CollabText;
  private textObserver: CollabTextAllObserver;
  private chunkObserver: CollabTextChunkAllObserver;
  private cursorObserver: CollabTextCursorAllObserver;
  private observedChunks: CollabTextChunk[] = [];
  private observedCursors: CollabTextCursor[] = [];
  private ownCursor: CollabTextCursor;
  participants: CollabTextCursor[] = [];
  private writing = false;
  private pendingValue: string | undefined;
  private changeDetector = inject(ChangeDetectorRef);

  @ViewChild('input', { static: true })
  input: ElementRef<HTMLTextAreaElement>;

  @ViewChild('marks', { static: true })
  marks: ElementRef<HTMLElement>;

  private remoteCursors: GemCollabTextCursor[] = [];
  // Selection already written to the current user's cursor.
  private reportedAnchor = 0;
  private reportedHead = 0;
  private drawingScheduled = false;
  private destroyed = false;
  private resizeObserver: ResizeObserver;

  @Input()
  override set text(text: CollabText) {
    super.text = text;
    if (this.viewReady) {
      void this.bindText(text);
    }
  }

  override get text(): CollabText {
    return super.text;
  }

  @Input()
  override set showChunks(show: boolean) {
    super.showChunks = show;
    this.cursorChanged();
  }

  override get showChunks(): boolean {
    return super.showChunks;
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.resizeObserver = new ResizeObserver(() => this.alignMarks());
    this.resizeObserver.observe(this.input.nativeElement);
    void this.bindText(this.text);
  }

  override ngOnDestroy(): void {
    this.destroyed = true;
    this.binding++;
    this.refreshVersion++;
    this.resizeObserver?.disconnect();
    this.bindingTask = this.bindingTask.then(() => this.detachText());
    super.ngOnDestroy();
  }

  private async detachText(): Promise<void> {
    const text = this.boundText;
    this.boundText = undefined;
    this.refreshVersion++;
    await this.refreshTask;
    const observer = this.textObserver;
    const chunks = this.observedChunks;
    const chunkObserver = this.chunkObserver;
    const cursors = this.observedCursors;
    const cursorObserver = this.cursorObserver;
    const ownCursor = this.ownCursor;
    this.observedChunks = [];
    this.observedCursors = [];
    this.ownCursor = undefined;
    this.participants = [];
    this.pendingValue = undefined;
    if (text && observer) {
      await text.removeObserver(observer);
    }
    for (const chunk of chunks) {
      await chunk.removeObserver(chunkObserver);
    }
    for (const cursor of cursors) {
      await cursor.removeObserver(cursorObserver);
    }
    if (text && ownCursor) {
      await text.removeCursors$collabtextmodule_CollabTextCursor(ownCursor);
      await CollabTextModuleManager.deleteCollabTextCursor(ownCursor.getGemId());
    }
  }

  private bindText(text: CollabText): Promise<void> {
    if (text === this.requestedText || this.destroyed) {
      return this.bindingTask;
    }
    this.requestedText = text;
    const binding = ++this.binding;
    this.bindingTask = this.bindingTask.then(() => this.attachText(text, binding));
    return this.bindingTask;
  }

  private async attachText(text: CollabText, binding: number): Promise<void> {
    if (binding !== this.binding || this.destroyed) {
      return;
    }
    await this.detachText();
    if (binding !== this.binding || this.destroyed) {
      return;
    }
    this.loaded = false;
    this.changeDetector.markForCheck();
    if (!text) {
      this.showText('');
      return;
    }
    const observer = await new CollabTextAllObserver().__CollabTextAllObserverConstructor(() => this.scheduleRefresh());
    const chunkObserver = await new CollabTextChunkAllObserver().__CollabTextChunkAllObserverConstructor(() => this.scheduleRefresh());
    const cursorObserver = await new CollabTextCursorAllObserver().__CollabTextCursorAllObserverConstructor(() => {
      this.changeDetector.markForCheck();
    });
    if (binding !== this.binding || this.destroyed) {
      return;
    }
    this.boundText = text;
    this.textObserver = observer;
    this.chunkObserver = chunkObserver;
    this.cursorObserver = cursorObserver;
    const participant = CommandManager.getParticipantId();
    const colors = ['#c2185b', '#1976d2', '#2e7d32', '#ef6c00', '#6a1b9a', '#00838f'];
    this.ownCursor = await (await CollabTextModuleManager.collabTextCursorBuilder())
      .userName(this.userName)
      .color(this.userColor || colors[participant % colors.length])
      .participant(participant)
      .build();
    await text.addCursors$collabtextmodule_CollabTextCursor(this.ownCursor);
    this.reportedAnchor = 0;
    this.reportedHead = 0;
    await text.addObserver(observer);
    await CollabTextModuleManager.loadAllCollabTextChunkWithIDs(await text._internal_getChunksGemList().getIDs());
    if (binding !== this.binding || this.destroyed) {
      await text.removeObserver(observer);
      return;
    }
    await this.refresh();
  }

  private scheduleRefresh(): void {
    this.refreshVersion++;
    if (this.refreshScheduled || this.destroyed) {
      return;
    }
    this.refreshScheduled = true;
    queueMicrotask(() => {
      this.refreshScheduled = false;
      void this.refresh();
    });
  }

  private refresh(): Promise<void> {
    this.refreshTask = this.refreshTask.then(() => this.refreshFromChunks());
    return this.refreshTask;
  }

  private async refreshFromChunks(): Promise<void> {
    const version = ++this.refreshVersion;
    const text = this.boundText;
    if (!text || this.writing || this.destroyed) {
      return;
    }
    const chunks = await GemListUtil.toArraySync(await text.getChunksList());
    if (version !== this.refreshVersion || text !== this.boundText || this.writing || this.destroyed) {
      return;
    }
    await CollabTextModuleManager.loadAllCollabTextCursorWithIDs(await text._internal_getCursorsGemList().getIDs());
    const cursors = (await GemListUtil.toArraySync(await text.getCursorsList()))
      .filter(cursor => cursor.getGemId() !== this.ownCursor?.getGemId());
    if (version !== this.refreshVersion || text !== this.boundText || this.destroyed) {
      return;
    }
    const previousCursors = this.observedCursors;
    this.observedCursors = cursors;
    for (const cursor of previousCursors) {
      if (!cursors.includes(cursor)) {
        await cursor.removeObserver(this.cursorObserver);
      }
    }
    for (const cursor of cursors) {
      if (!await cursor.containsObserver(this.cursorObserver)) {
        await cursor.addObserver(this.cursorObserver);
      }
    }
    this.participants = cursors;
    const previous = this.observedChunks;
    this.observedChunks = chunks;
    for (const chunk of previous) {
      if (!chunks.includes(chunk)) {
        await chunk.removeObserver(this.chunkObserver);
      }
    }
    for (const chunk of chunks) {
      if (!await chunk.containsObserver(this.chunkObserver)) {
        await chunk.addObserver(this.chunkObserver);
      }
    }
    if (version !== this.refreshVersion || text !== this.boundText || this.writing || this.destroyed) {
      return;
    }
    this.chunkLengths = chunks.map(chunk => chunk.getContent().length);
    this.showText(chunks.map(chunk => chunk.getContent()).join(''));
    this.loaded = true;
    this.changeDetector.markForCheck();
    this.cursorChanged();
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

    this.reportedAnchor = direction === 'backward' ? end : start;
    this.reportedHead = direction === 'backward' ? start : end;
    this.cursorChanged();
  }

  onInput(): void {
    if (!this.boundText || !this.loaded) {
      return;
    }
    this.pendingValue = this.input.nativeElement.value;
    this.cursorChanged();
    this.reportSelection();
    if (!this.writing) {
      void this.writeInput();
    }
  }

  private async writeInput(): Promise<void> {
    this.writing = true;
    const text = this.boundText;
    try {
      while (this.pendingValue !== undefined && text === this.boundText && !this.destroyed) {
        const value = this.pendingValue;
        this.pendingValue = undefined;
        await text.setContent(value);
      }
    } finally {
      this.writing = false;
      this.scheduleRefresh();
      if (this.pendingValue !== undefined && this.boundText && !this.destroyed) {
        void this.writeInput();
      }
    }
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
      const cursor = this.ownCursor;
      if (cursor) {
        void cursor.setAnchor(anchor);
        void cursor.setHead(head);
      }
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
    for (const length of this.showChunks ? this.chunkLengths : []) {
      chunkStarts.push(clamp(chunkEnd));
      chunkEnd += length;
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
