/* (c) https://github.com/MontiCore/monticore */


import {
  Component,
  Input, Output, EventEmitter,
  ViewContainerRef
} from "@angular/core";
import {
  CommonModule
} from "@angular/common";

import "@angular/compiler";
import {InspectorService} from "wizard/InspectorService";
import {Observable} from "rxjs";


/**
 * The control buttons
 */
@Component({
  standalone: true,
  imports: [CommonModule],
  selector: 'wizard-controls',
  styles: 'button {width: 12px; height: 12px; font-size: 5px; vertical-align: top; float: left;}' +
      ' span {width: 12px; height: 12px; font-size: 9px; vertical-align: center; float: left; background-color: gray; margin: 1px; } :host {height: 12px; display:flex;}',
  template: `
        <span (click)="inspect()" title="Inspect" style="cursor: help">👁</span>
        <span (click)="remove()" title="Remove">&cross;</span>
        <span (dragstart)="dragStart($event)" draggable="true" title="Move" style="cursor: move">&#10021;</span>
  `,
})
export class WizardControls {


  constructor(private viewContainerRef: ViewContainerRef, private inspectorService: InspectorService) {
    this.showControlsObsv = inspectorService.showControls.asObservable();
  }

  showControlsObsv: Observable<boolean>; // Observable weather to show the controls?

  @Input()
  index: number;
  @Input()
  listName: string;
  @Input()
  length: number;

  @Output() controlEvent: EventEmitter<any> = new EventEmitter();


  inspect() {
    this.controlEvent.next({type: 'inspect', index: this.index, listName: this.listName})
  }

  remove() {
    this.controlEvent.next({type: 'remove', index: this.index, listName: this.listName})
  }

  dragStart($event: DragEvent) {
    console.log("start drag", $event)
    const dragEnd = (e) => {
      $event.target.removeEventListener('dragend', dragEnd);
      this.inspectorService.showDropZone.next(false);
      console.log("drag end");
    }
    $event.target.addEventListener('dragend', dragEnd);
    $event.dataTransfer.dropEffect = 'move';

    setTimeout(() => this.inspectorService.showDropZone.next(true), 100);
    $event.dataTransfer.effectAllowed = "move";

    this.controlEvent.next({type: 'startDrag', index: this.index, listName: this.listName});
  }
}
