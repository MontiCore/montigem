/* (c) https://github.com/MontiCore/monticore */
import {Directive, EventEmitter, Injectable, Output} from "@angular/core";
import {Subject} from "rxjs";

// Class for defining context menu types

export interface ContextMenuItem {
  title: string;
  data?: any;
  onClick: OnContextClick;
}

interface OnContextClick {
  (clicked: ContextMenuItem): void;
}

export interface ContextMenuRequestEvent {
  show: (items: ContextMenuItem[]) => void
}

@Injectable(
  {providedIn: "root"}
)
export class ContextMenuService {

  public show: Subject<{ event: MouseEvent, obj: ContextMenuItem[] }> = new Subject<{
    event: MouseEvent,
    obj: ContextMenuItem[]
  }>();
}


@Directive({
  selector: '[context-menu-emitter]',
  standalone: true,
  host: {'(contextmenu)': 'rightClicked($event)'}
})
export class LazyContextMenuDirective {

  @Output("context-menu-emitter")
  requestData: EventEmitter<{ show: (items: ContextMenuItem[]) => void }> = new EventEmitter<any>();

  constructor(private _contextMenuService: ContextMenuService) {
  }

  rightClicked(event: MouseEvent) {
    event.preventDefault(); // to prevent the browser contextmenu
    event.stopPropagation();
    this.requestData.emit({
      show: (items: ContextMenuItem[]) => {
        this._contextMenuService.show.next({event: event, obj: items});
      }
    });
  }
}

