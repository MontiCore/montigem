/* (c) https://github.com/MontiCore/monticore */
import {Directive, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges} from '@angular/core';

type PositionedLike = {

  addObserver(observer: PositionedObserverLike): void;
  getX(): number;
  getY(): number;
  removeObserver(observer: PositionedObserverLike): void;
}


type PositionedObserverLike = {}

/**
 * Makes an element positioned via a domain object.
 * Requires an implementation that adds a PositionedObserver calling AbstractPositionedDirective#update()!
 */
@Directive()
export class AbstractPositionedDirective implements OnChanges, OnDestroy {

  @Input() positioned: PositionedLike;

  protected observer: PositionedObserverLike;

  constructor(private el: ElementRef) {
    this.update();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.positioned)
      this.positioned.addObserver(this.observer);
    this.update();
  }

  update() {
    if (this.positioned)
      this.el.nativeElement.style.transform = 'translate(' + this.positioned.getX() + 'px, ' + this.positioned.getY() + 'px )';
  }

  ngOnDestroy() {
    if (this.positioned)
      this.positioned.removeObserver(this.observer);
  }


}
