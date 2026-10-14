/* (c) https://github.com/MontiCore/monticore */
import {Directive, HostListener} from '@angular/core';

interface TouchState {
  startTime: number;
  timeoutId?: any;
  clientX: number;
  clientY: number;
}

/**
 * Trigger context menu events when a user holds a touch event 2s on the same element & location.
 * You can add it by adding the attribute selector, such as <div touchContextMenu></div>
 */
@Directive({selector: 'touchContextMenu', standalone: true})
export class TouchWrapperDirective {
  private touchMap = new Map<number, TouchState>();

  private RIGHT_CLICK_TIME = 2000;

  @HostListener('document:touchstart', ['$event'])
  onTouchStart(event: TouchEvent) {
    const touch = event.changedTouches[0];
    const tgt = event.target as HTMLElement;

    const startTime = Date.now();

    const timeoutId = setTimeout(() => {
      const contextMenuEvent = new MouseEvent('contextmenu', {
        bubbles: true,
        cancelable: true,
        view: window,
        button: 2,
        clientX: touch.clientX,
        clientY: touch.clientY,
      });

      tgt.dispatchEvent(contextMenuEvent);
    }, this.RIGHT_CLICK_TIME);

    const clientX = touch.clientX;
    const clientY = touch.clientY;

    this.touchMap.set(touch.identifier, {startTime, timeoutId, clientX, clientY});
  }

  @HostListener('document:touchmove', ['$event'])
  onTouchMove(event: TouchEvent) {
    const touch = event.changedTouches[0];
    const state = this.touchMap.get(touch.identifier);
    if (!state) return;
    if (Math.abs(touch.clientX - state.clientX) > 10
      || Math.abs(touch.clientY - state.clientY) > 10) {
      clearTimeout(state.timeoutId);
      this.touchMap.delete(touch.identifier);
    }
  }

  @HostListener('document:touchend', ['$event'])
  onTouchEnd(event: TouchEvent) {
    const touch = event.changedTouches[0];
    const state = this.touchMap.get(touch.identifier);
    if (!state) return;
    // const tgt = event.target as HTMLElement;
    clearTimeout(state.timeoutId);
    this.touchMap.delete(touch.identifier);
  }

}
