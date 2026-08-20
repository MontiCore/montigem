/* (c) https://github.com/MontiCore/monticore */

// This component is based upon https://github.com/chrisengelsma/draggable-resizable-div/blob/master/LICENSE
// and thus upon the MIT license

import {NgIf, } from "@angular/common";


import { Component, ElementRef, Inject, ViewChild, Input } from '@angular/core';
import { DOCUMENT } from '@angular/common';

@Component({
  standalone: true,
  imports: [NgIf],
  selector: 'wizard-modal',
  template: `
    <ng-container *ngIf="this.open">
    <div #wrapper class="wrapper"
        [style.top.px]="position.top"
        [style.left.px]="position.left"
    >
    <div #dragBar> </div>

    <div class="content"
        [style.width.px]="size.width"
        [style.height.px]="size.height"
    >

    <div class="contentTopBar" (mousedown)="startDrag($event)" > </div>

    <ng-content/>

</div>

</div></ng-container>
  `,
  styleUrl:"WizardModal.scss"
})
export class WizardModal   {
  @ViewChild('wrapper') wrapperRef!: ElementRef;

  @ViewChild('topBar') topBarRef!: ElementRef;

  @Input()
  open: boolean = true;


  position: { top: number, left: number } = { top: 100, left: 100 };

  size: { width: number, height: number } = { width: 200, height: 200 };

  lastPosition: { top: number, left: number };

  constructor(@Inject(DOCUMENT) private _document: Document,
              private _el: ElementRef) { }

  startDrag($event): void {
    $event.preventDefault();
    const mouseX = $event.clientX;
    const mouseY = $event.clientY;

    const positionX = this.position.left;
    const positionY = this.position.top;

    const duringDrag = (e) => {
      const dx = e.clientX - mouseX;
      const dy = e.clientY - mouseY;
      this.position.left = positionX + dx;
      this.position.top = positionY + dy;
      this.lastPosition = { ...this.position };
    };

    const finishDrag = (e) => {
      this._document.removeEventListener('mousemove', duringDrag);
      this._document.removeEventListener('mouseup', finishDrag);
    };

    this._document.addEventListener('mousemove', duringDrag);
    this._document.addEventListener('mouseup', finishDrag);
  }

}
