/* (c) https://github.com/MontiCore/monticore */

import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  HostListener,
  Output,
  SimpleChanges
} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {MatTooltipModule} from '@angular/material/tooltip';
import {
  config,
  GemAutocompleteInputComponent
} from '../../../../../../build/gui2java2ts/mc/fenix/input/GemAutocompleteInputComponent';
import {Option} from '../../../../../../build/gui2java2ts/mc/fenix/input/gemdropdowninputtypes/Option';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, FormsModule, MatTooltipModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemAutocompleteInput extends GemAutocompleteInputComponent {
  @HostBinding("attr.id")
  _id: string;

  @HostBinding("style.width")
  _width: string = '100%';

  @HostBinding("style.height")
  _height: string = '100%';

  override _disabled: boolean = false;

  override _placeholder: string = '';

  @HostListener('document:click', ['$event'])
  clickOutside(event: any): void {
    if (!this.componentRef.nativeElement.contains(event.target)) {
      this.hideDropdown();
    }
  }

  @HostListener('document:keydown', ['$event'])
  enterOutside(event: any): void {
    if (
      (event.key === "Enter" && !this.componentRef.nativeElement.contains(event.target)) ||
      event.key === "Tab"
    ) {
      this.hideDropdown();
    }
  }

  @Output()
  entryChange: EventEmitter<string> = new EventEmitter<string>();
  @Output()
  keyPress: EventEmitter<KeyboardEvent> = new EventEmitter<KeyboardEvent>();
  @Output()
  keyRelease: EventEmitter<KeyboardEvent> = new EventEmitter<KeyboardEvent>();

  elementId: string = Math.random() + '';
  filteredOptions: Option[] = [];
  isContentShown: boolean = false;
  highlightIdx: number = -1;

  constructor(private componentRef: ElementRef) {
    super();
  }

  ngOnInit(): void {
    this.filteredOptions = this.options;
  }

  ngOnChanges(changes: SimpleChanges): void {
   /* if (changes["options"]) {
      this.filteredOptions = changes["options"].currentValue;
    }*/
  }

  public override setOptions(val: Array<Option>) {
    super.setOptions(val);
    this.filteredOptions = val;
  }

  onEntryChange(entry: string): void {
    this.entry = entry;
    this.entryChange.emit(this.entry);
    this.filterOptions();
    this.showDropdown();
  }

  onKeyPress(event: KeyboardEvent): void {
    this.keyPress.emit(event);
  }

  onKeyRelease(event: KeyboardEvent): void {
    this.keyRelease.emit(event);
  }

  showDropdown(): void {
    if (!this.isContentShown && this.filteredOptions.length > 0) {
      this.isContentShown = true;
    }
  }

  selectOption(entry: string): void {
    this.onEntryChange(entry);
    this.hideDropdown();
  }

  selectWithEnter(): void {
    if (this.highlightIdx < 0) {
      this.showDropdown();
      return;
    }
    this.selectOption(this.filteredOptions[this.highlightIdx].getValue());
    this.hideDropdown();
  }

  focusDown(): void {
    const newIdx = this.highlightIdx + 1;
    this.highlightIdx = newIdx < this.filteredOptions.length ? newIdx : this.filteredOptions.length - 1;
    if (this.isOverflown()) {
      this.automaticScrollDown();
    }
  }

  focusUp(): void {
    const newIdx = this.highlightIdx - 1;
    this.highlightIdx = newIdx >= -1 ? newIdx : -1;
    if (this.isOverflown()) {
      this.automaticScrollUp();
    }
  }

  setHighlightIdx(idx: number): void {
    if (idx != this.highlightIdx) {
      this.highlightIdx = idx;
    }
  }

  toggleDropdown(): void {
    if (!this.isContentShown && this.filteredOptions.length > 0) {
      this.showDropdown();
    }
    else {
      this.hideDropdown();
    }
  }

  hideDropdown(): void {
    this.isContentShown = false;
    this.resetHighlightIdx();
  }

  getOptionsId(): string {
    return this.elementId + "Options";
  }

  private filterOptions(): void {
    this.filteredOptions = this.options.filter(option =>
      option.getDisplay().toUpperCase().indexOf(this.entry.toUpperCase()) > -1
    );
    if (this.filteredOptions.length == 1) {
      this.highlightIdx = 0;
    }
    else {
      this.resetHighlightIdx();
    }
  }

  private resetHighlightIdx() {
    this.highlightIdx = -1;
  }

  private isOverflown(): boolean {
    const element: HTMLElement = document.getElementById(this.getOptionsId());
    return element.scrollHeight > element.clientHeight;
  }

  private automaticScrollDown(): void {
    const element: HTMLElement = document.getElementById(this.getOptionsId());
    const highlightOption: HTMLElement = element.querySelector(".highlight-option");
    const optionHeight: number = highlightOption?.offsetHeight;
    if (highlightOption?.offsetTop + optionHeight >= element.clientHeight) {
      element.scrollTop += optionHeight;
    }
  }

  private automaticScrollUp(): void {
    const element: HTMLElement = document.getElementById(this.getOptionsId());
    const highlightOption: HTMLElement = element.querySelector(".highlight-option");
    const optionHeight: number = highlightOption?.offsetHeight;
    if (highlightOption?.offsetTop - optionHeight < element.scrollTop) {
      element.scrollTop -= optionHeight;
    }
  }

}
