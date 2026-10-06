/* (c) https://github.com/MontiCore/monticore */
import {Directive, ElementRef, EventEmitter, HostListener, Output} from '@angular/core';

/**
 * Turn an element content editable
 *
 * <span contenteditableevents (onChangeContent)="onChangeText($event)"></span>
 */
@Directive({selector: '[contenteditableevents]', standalone: true})
export class ContentEditableDirective {

  // text, width, height, elem, update done
  @Output() onChangeContent = new EventEmitter<[string, number, number, HTMLElement, boolean]>();

  protected before?: string;


  constructor(private el: ElementRef) {
    this.el.nativeElement.setAttribute('contenteditable', 'plaintext-only');
  }

  @HostListener('focus', ['$event'])
  onBeforeEdit($event: FocusEvent) {
    this.before = ($event.target as HTMLElement).textContent;
  }

  @HostListener('blur', ['$event'])
  onEditDone($event: Event) {
    const content = ($event.target as HTMLElement).textContent;
    if (content !== this.before) {
      this.before = content;
      this.onChangeContent.emit([content, ($event.target as HTMLElement).getBoundingClientRect().width, ($event.target as HTMLElement).getBoundingClientRect().height, $event.target as HTMLElement, true]);
    }
  }

  @HostListener('input', ['$event'])
  @HostListener('keyup', ['$event'])
  @HostListener('paste', ['$event'])
  onDuringEdit($event: Event) {
    const content = ($event.target as HTMLElement).textContent;
    if (content !== this.before) {
      this.onChangeContent.emit([content, ($event.target as HTMLElement).getBoundingClientRect().width, ($event.target as HTMLElement).getBoundingClientRect().height, $event.target as HTMLElement, false]);
    }
  }

}


