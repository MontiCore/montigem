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
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { config, GemDropdownInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemDropdownInputComponent';
import { Option } from '../../../../../../build/gui2java2ts/mc/fenix/input/gemdropdowninputtypes/Option';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, FormsModule, MatIconModule, MatTooltipModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemDropdownInput extends GemDropdownInputComponent {
  @HostBinding("attr.id")
  _id: string;

  @HostBinding("style.width")
  _width: string = 'auto';

  @HostBinding("style.height")
  _height: string = 'auto';

  override _disabled: boolean = false;

  override _searchEnabled: boolean = false;

  @HostListener('document:click', ['$event'])
  clickOutside(event: any): void {
    if (!this.componentRef.nativeElement.contains(event.target)) {
      this.hideDropdown();
    }
  }

  @HostListener('document:keydown', ['$event'])
  enterOutside(event: any): void {
    if (event.key === "Enter" && !this.componentRef.nativeElement.contains(event.target)) {
      this.hideDropdown();
    }
  }

  @Output()
  entryChange: EventEmitter<string> = new EventEmitter<string>();

  selectedOption: Option;
  elementId: string = Math.random() + '';
  isContentShown: boolean = false;
  filterString: string = "";
  filteredOptions: Option[] = [];
  highlightIdx: number = -1;

  constructor(private componentRef: ElementRef) {
    super();
  }

  ngOnInit(): void {
    this.filteredOptions = this.options;
    if (this.filteredOptions?.length > 0) {
      this.selectOption(this.filteredOptions[0]);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
/*    if (changes["options"]) {
      this.filteredOptions = changes["options"].currentValue;
    }*/
    if (changes["entry"]) {
      this.selectedOption = this.options?.find(o => o.getValue() === this.entry);
    }
  }

  public override setOptions(val: Array<Option>) {
    super.setOptions(val);
    this.filteredOptions = val;
  }

  onKeyPress(event: KeyboardEvent): void {
    this.keyPress.emit(event);
  }

  onKeyRelease(event: KeyboardEvent): void {
    this.keyRelease.emit(event);
  }

  toggleDropdown(): void {
    if (!this.isContentShown && this.filteredOptions.length > 0) {
      this.showDropdown();
    }
    else {
      this.hideDropdown();
    }
  }

  selectOption(option: Option): void {
    this.entry = option.getValue();
    this.entryChange.emit(this.entry);
    this.hideDropdown();
  }

  selectWithEnter(event: any): void {
    event.preventDefault();
    if (this.highlightIdx < 0) {
      this.toggleDropdown();
      return;
    }
    this.selectOption(this.filteredOptions[this.highlightIdx]);
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

  filterOptions(): void {
    if (this.options) {
      this.filteredOptions = this.options.filter(option =>
        option.getDisplay().toUpperCase().indexOf(this.filterString.toUpperCase()) > -1
      );
    }
  }

  getInputId(): string {
    return this.elementId + "Input";
  }

  getSearchInputId(): string {
    return this.elementId + "Search";
  }

  getOptionsId(): string {
    return this.elementId + "Options";
  }

  hideDropdown(): void {
    this.isContentShown = false;
    this.highlightIdx = -1;
    this.filterString = "";
    this.filteredOptions = this.options;
  }

  tabOut(): void {
    if (this.isContentShown) {
      this.hideDropdown();
    }
  }

  private showDropdown(): void {
    this.isContentShown = true;
  }

  private isOverflown(): boolean {
    const element: HTMLElement = document.getElementById(this.getOptionsId());
    return element.scrollHeight > element.clientHeight;
  }

  private automaticScrollDown(): void {
    const element: HTMLElement = document.getElementById(this.getOptionsId());
    const highlightOption: HTMLElement = element.querySelector(".highlight-option");
    const optionHeight: number = highlightOption?.offsetHeight;
    const optionOffset: number = this.searchEnabled ? 0 : optionHeight;
    if (highlightOption?.offsetTop + optionOffset >= element.clientHeight) {
      element.scrollTop += optionHeight;
    }
  }

  private automaticScrollUp(): void {
    const element: HTMLElement = document.getElementById(this.getOptionsId());
    const highlightOption: HTMLElement = element.querySelector(".highlight-option");
    const optionHeight: number = highlightOption?.offsetHeight;
    const optionOffset: number = this.searchEnabled ? optionHeight * 2 : optionHeight;
    if (highlightOption?.offsetTop - optionOffset < element.scrollTop) {
      element.scrollTop -= optionHeight;
    }
  }

}
