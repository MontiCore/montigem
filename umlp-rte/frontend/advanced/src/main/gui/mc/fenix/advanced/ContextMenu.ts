
/* (c) https://github.com/MontiCore/monticore */
import {ActivatedRoute, Router} from '@angular/router';
import {NgFor} from "@angular/common";
import {Component, Directive, EventEmitter, HostListener, Injectable, Input, Output, inject, ChangeDetectionStrategy} from "@angular/core";
import {
  config,
  ContextMenuComponent,
} from '../../../../../../build/gui2java2ts/mc/fenix/advanced/ContextMenuComponent';
import {ContextMenuService, ContextMenuItem} from "./context.types";

@Component({
  selector: "context-menu",
  imports: [NgFor],
  template: '<div class="contextContainer" [style]="locationCss"> <ul><li *ngFor="let item of items" (click)="onContextMenuClick($event, item)" class="item" >{{item.title}}</li></ul> </div>',
  styles: [
    '.contextContainer{ width: 150px; background-color: #eee; z-index: 9999}',
    '.item{ border-bottom: 1px #abc solid}',
    '.item:hover{ background-color: #abc}',
    'ul{margin: 0; padding: 0; list-style-type: none}',
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
})


export class ContextMenu extends ContextMenuComponent {

  constructor() {
    super();
    inject(ContextMenuService).show.subscribe(e => this.showMenu(e.event, e.obj));
  }


  @Input()
  defaultContextMenuItems: ContextMenuItem[] = []

  items: ContextMenuItem[] = []

  showContextMenu: boolean = false

  onContextMenuClick($event: any, item: ContextMenuItem) {
    item.onClick(item);
  }

  private mouseLocation: { left: number, top: number } = {left: 0, top: 0};


  get locationCss() {
    return {
      'position': 'fixed',
      'display': this.showContextMenu ? 'block' : 'none',
      left: this.mouseLocation.left + 'px',
      top: this.mouseLocation.top + 'px',
    };
  }

  @HostListener('document:click')
  onMouseMove() {
    this.showContextMenu = false;
  }

  showMenu($event: MouseEvent, items: ContextMenuItem[]) {
    this.mouseLocation = {
      left: $event.clientX,
      top: $event.clientY
    }
    this.showContextMenu = true;

    if (items) {
      this.items = items;
    } else {
      this.items = this.defaultContextMenuItems;
    }
  }

}
