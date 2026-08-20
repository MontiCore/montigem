/* (c) https://github.com/MontiCore/monticore */

import { Component, EventEmitter, HostBinding, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { config, GemNumberInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemNumberInputComponent';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  standalone: true,
  imports: [...config.imports, FormsModule, MatTooltipModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemNumberInput extends GemNumberInputComponent implements OnInit, OnChanges {
  override _id: string = Math.random() + '';
  override _labelText: string = "";
  override _step: number = 1;
  override _disabled: boolean = false;
  override _placeholder: string = "";

  @HostBinding("attr.id")
  componentId: string;

  @HostBinding("style.width")
  _width: string = '100%';

  @HostBinding("style.height")
  _height: string = '100%';

  @Output() public entryChange = new EventEmitter<number>();
  @Output() public override keyRelease = new EventEmitter<KeyboardEvent>();
  @Output() public override keyPress = new EventEmitter<KeyboardEvent>();

  private isTyping: boolean = false;
  private lastNumericValue: number | null = null;

  get displayValue(): number | null {
    return this.entry ?? null;
  }

  get stepAttribute(): string {
    return 'any';
  }

  private updateValue(val: number): void {
    const rounded = Number(val.toFixed(10));
    this.entry = rounded;
    this.lastNumericValue = rounded;
    this.entryChange.emit(rounded);
  }

  onKeyDown(event: KeyboardEvent): void {
    this.keyPress.emit(event);

    const stepVal = (this.step !== undefined && this.step !== null && !isNaN(Number(this.step)) && Number(this.step) > 0)
      ? Number(this.step)
      : 1;

    const currentNum = this.lastNumericValue ?? (this.entry !== null && this.entry !== undefined ? Number(this.entry) : 0);

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.updateValue(currentNum + stepVal);
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.updateValue(currentNum - stepVal);
      return;
    }

    const allowedKeys = [
      'Backspace', 'Tab', 'Enter', 'Escape', 'Delete',
      'ArrowLeft', 'ArrowRight', 'Home', 'End'
    ];

    if (allowedKeys.includes(event.key)) {
      this.isTyping = true;
      return;
    }

    if (event.ctrlKey || event.metaKey) {
      this.isTyping = true;
      return;
    }

    if (/^[0-9]$/.test(event.key)) {
      this.isTyping = true;
      return;
    }

    const target = event.target as HTMLInputElement;
    const currentVal = target ? target.value : '';

    if (event.key === '-' && !currentVal.includes('-')) {
      const selectionStart = target?.selectionStart ?? 0;
      if (selectionStart === 0) {
        this.isTyping = true;
        return;
      }
    }

    if ((event.key === '.' || event.key === ',') && !currentVal.includes('.') && !currentVal.includes(',')) {
      this.isTyping = true;
      return;
    }

    event.preventDefault();
  }

  onPaste(event: ClipboardEvent): void {
    const pastedData = event.clipboardData?.getData('text');
    if (pastedData) {
      const sanitized = pastedData.replace(',', '.');
      if (isNaN(Number(sanitized)) || /^\s*$/.test(sanitized)) {
        event.preventDefault();
      } else {
        this.isTyping = true;
      }
    }
  }

  onEntryChange(event: number | string) {
    if (event === null || event === undefined || event === '') {
      this.entry = undefined;
      this.lastNumericValue = null;
      this.isTyping = false;
      this.entryChange.emit(undefined);
      return;
    }

    const sanitized = typeof event === 'number' ? event : Number(String(event).replace(',', '.'));

    if (isNaN(sanitized)) {
      this.isTyping = false;
      return;
    }

    const stepVal = (this.step !== undefined && this.step !== null && !isNaN(Number(this.step)) && Number(this.step) > 0)
      ? Number(this.step)
      : 1;

    if (this.isTyping) {
      this.isTyping = false;
      const rounded = Number(sanitized.toFixed(10));
      this.entry = rounded;
      this.lastNumericValue = rounded;
      this.entryChange.emit(rounded);
    } else {
      const prev = this.lastNumericValue ?? (this.entry !== null && this.entry !== undefined ? Number(this.entry) : 0);
      let calculatedVal: number;
      if (sanitized > prev) {
        calculatedVal = prev + stepVal;
      } else if (sanitized < prev) {
        calculatedVal = prev - stepVal;
      } else {
        calculatedVal = sanitized;
      }
      const rounded = Number(calculatedVal.toFixed(10));
      this.entry = rounded;
      this.lastNumericValue = rounded;
      this.entryChange.emit(rounded);
    }
  }

  ngOnInit(): void {
    this.componentId = this.id;
    if (this.entry !== null && this.entry !== undefined && !isNaN(Number(this.entry))) {
      this.lastNumericValue = Number(this.entry);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['entry'] && this.entry !== null && this.entry !== undefined && !isNaN(Number(this.entry))) {
      this.lastNumericValue = Number(this.entry);
    }
  }
}
