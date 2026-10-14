/* (c) https://github.com/MontiCore/monticore */
import {Directive, ElementRef, HostListener, Input} from '@angular/core';

type PositionedLike = {
  setX(_x?: any, scope?: any);
  setY(_y?: any, scope?: any);
  getX(): number;
  getY(): number;
}

/**
 * Allows movement of a component by updating its PositionLike domain object.
 * Works best in combination with the abstractpositioned.directive.
 */
@Directive({selector: '[moveable]', standalone: true})
export class MoveableDirective  {

  @Input() moveable: PositionedLike;

  protected isBeingMoved: boolean = false;
  protected mouse_start_x: number = 0;
  protected mouse_start_y: number = 0;
  protected move_prev_x: number = 0
  protected move_prev_y: number = 0


  constructor(private el: ElementRef) {
    this.el.nativeElement.style.cursor = 'move';
  }

  @HostListener('mousedown', ['$event'])
  onMouseDown($event: MouseEvent) {
    if (!this.moveable) return;
    // if ($event.target instanceof HTMLElement && !$event.target.classList.contains("wb-node") && !$event.target.classList.contains("wb-title"))
    //   return;
    if (this.shouldIgnore($event.target)) return;
    console.log($event.target)
    $event.preventDefault();

    this.startMove($event.x, $event.y);
  }

  @HostListener('touchstart', ['$event'])
  onTouchStart($event: TouchEvent) {
    if (!this.moveable) return;
    let touch = $event.touches[0] || $event.changedTouches[0];
    // if (touch.target instanceof HTMLElement && !touch.target.classList.contains("wb-node") && !touch.target.classList.contains("wb-title"))
    //   return;
    if (this.shouldIgnore(touch.target)) return;
    $event.preventDefault();

    this.startMove(touch.clientX, touch.clientY);
  }

  shouldIgnore(target:any) {
    return target instanceof HTMLInputElement || target instanceof HTMLSelectElement;
  }

  startMove(x: number, y: number) {
    this.mouse_start_x = x;
    this.mouse_start_y = y;
    this.move_prev_x = this.moveable.getX();
    this.move_prev_y = this.moveable.getY();
    this.isBeingMoved = true;
  }


  @HostListener('document:mousemove', ['$event'])
  onMouseMove($event: MouseEvent) {
    if (this.isBeingMoved) {
      this.moveable.setX(this.move_prev_x + $event.x - this.mouse_start_x);
      this.moveable.setY(this.move_prev_y + $event.y - this.mouse_start_y);
      // this.onMove.emit({data:this.data, final: false});
    }
  }

  @HostListener('document:touchmove', ['$event'])
  onTouchMove($event: TouchEvent) {
    if (this.isBeingMoved) {
      let touch = $event.touches[0] || $event.changedTouches[0];
      this.moveable.setX(this.move_prev_x + touch.clientX - this.mouse_start_x);
      this.moveable.setY(this.move_prev_y + touch.clientY - this.mouse_start_y);
      // this.onMove.emit({data:this.data, final: false});
    }
  }

  @HostListener('document:touchend ', [])
  @HostListener('document:mouseup ', [])
  onMoveEnd() {
    // if (this.isBeingMoved)
    //   this.onMove.emit({data:this.data, final: true});
    this.isBeingMoved = false;
  }
}


