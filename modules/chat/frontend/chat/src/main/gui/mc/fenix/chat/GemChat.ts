/* (c) https://github.com/MontiCore/monticore */

import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, HostBinding, inject, NgZone, OnDestroy, ViewChild
} from '@angular/core';
import { GemButton, GemLoadingSpinner } from '@umlp/basic';
import { GemTextareaInput } from '@umlp/input';
import { config, GemChatComponent } from '../../../../../../build/gui2java2ts/mc/fenix/chat/GemChatComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, GemButton, GemLoadingSpinner, GemTextareaInput],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: [...config.styles, `
    :host { display: flex; flex-direction: column; min-height: 0; }
    .gem-chat-history { flex: 1 1 0; min-height: 0; overflow-y: auto; }
    .gem-chat-input { flex: 0 0 auto; display: flex; flex-direction: column; align-items: flex-start; }
  `],
  templateUrl: config.templateUrl,
})
export class GemChat extends GemChatComponent implements AfterViewInit, OnDestroy {
  @HostBinding('style.width')
  _width: string = '100%';

  @HostBinding('style.height')
  _height: string = '100%';

  override _entry: string = '';
  override _placeholder: string = '';
  override _sendLabel: string = 'Send';
  override _waitingForResponse: boolean = false;
  override _waitingLabel: string = '';

  @ViewChild('history', { static: true })
  history: ElementRef<HTMLElement>;

  private zone = inject(NgZone);
  private historyObserver: MutationObserver;
  // Whether the user has not scrolled up, i.e., new content should stay visible
  private followNewContent = true;

  ngAfterViewInit(): void {
    const history = this.history.nativeElement;
    this.zone.runOutsideAngular(() => {
      history.addEventListener('scroll', () => {
        this.followNewContent = history.scrollHeight - history.scrollTop - history.clientHeight < 10;
      });
      this.historyObserver = new MutationObserver(() => {
        if (this.followNewContent) {
          history.scrollTop = history.scrollHeight;
        }
      });
      this.historyObserver.observe(history, { childList: true, subtree: true, characterData: true });
    });
    history.scrollTop = history.scrollHeight;
  }

  ngOnDestroy(): void {
    this.historyObserver?.disconnect();
  }

  onEntryChange(entry: string): void {
    this.setEntryInternal(entry);
    this.entryChange.emit(entry);
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendEntry();
    }
  }

  sendEntry(): void {
    if (this.waitingForResponse || !this.entry?.trim()) {
      return;
    }
    this.followNewContent = true;
    this.send.emit();
  }
}
