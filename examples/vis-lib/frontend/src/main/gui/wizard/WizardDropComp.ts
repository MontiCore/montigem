/* (c) https://github.com/MontiCore/monticore */


import {
  Component,
  Input,
  HostBinding, HostListener,
  ElementRef, Output, EventEmitter
} from "@angular/core";
import {
  CommonModule
} from "@angular/common";

import "@angular/compiler";


/**
 * This component is a drop-target for elements to be moved to
 */
@Component({
  standalone: true,
  imports: [CommonModule],
  selector: 'wizard-drop-comp',
  styles: ':host {min-width: 20px; wixdth: 100%; min-height: 20px; hxeight: 100%; background-color: gray; } :host.dropzone {background-color: yellow}',
  template: `{{defaultContent}}`,
})
export class WizardDropComp {
  @HostBinding('class') @Input('type') className: string = "dropcomp";

  @Input()
  defaultContent: string = "Drop here";
  @Input()
  index: number;
  @Input()
  listName: string;
  @Output() controlEvent: EventEmitter<any> = new EventEmitter();

  constructor(private _el: ElementRef) {
  }

  @HostListener('dragover', ['$event'])
  dragover($event: DragEvent) {
    this._el.nativeElement.classList.add("dropzone");
    $event.preventDefault();
  }

  @HostListener('dragleave', ['$event'])
  dragleave($event: DragEvent) {
    this._el.nativeElement.classList.remove("dropzone");
  }

  @HostListener('drop', ['$event'])
  drop($event: DragEvent) {
    this._el.nativeElement.classList.remove("dropzone");
    console.log("dropped in", this.index);
    this.controlEvent.next({type: 'drop', index: this.index, listName: this.listName})
  }

}
