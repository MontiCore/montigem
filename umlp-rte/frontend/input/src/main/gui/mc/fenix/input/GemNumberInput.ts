/* (c) https://github.com/MontiCore/monticore */

import { Component, EventEmitter, HostBinding, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { config, GemNumberInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemNumberInputComponent';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  standalone: true,
  imports: [...config.imports, MatTooltipModule],
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

  private lastNumericValue: number | null = null;
  private displayString: string = '';

  get displayValue(): string {
    return this.displayString;
  }

  private syncDisplayFromEntry(): void {
    if (this.entry === null || this.entry === undefined || isNaN(Number(this.entry))) {
      this.displayString = '';
    } else {
      this.displayString = String(this.entry);
    }
  }

  private updateValue(val: number): void {
    const rounded = Number(val.toFixed(10));
    this.entry = rounded;
    this.lastNumericValue = rounded;
    this.displayString = String(rounded);
    this.entryChange.emit(rounded);
  }

  private countDecimalSeparators(value: string): number {
    let count = 0;
    for (const char of value) {
      if (char === '.' || char === ',') {
        count++;
      }
    }
    return count;
  }

  private normalizeNumberString(value: string): string {
    return value.replace(',', '.');
  }

  private getStepValue(): number {
    return (this.step !== undefined && this.step !== null && !isNaN(Number(this.step)) && Number(this.step) > 0)
      ? Number(this.step)
      : 1;
  }

  private hasMin(): boolean {
    return this.min !== undefined && this.min !== null && !isNaN(Number(this.min));
  }

  private getMinValue(): number {
    return Number(this.min);
  }

  onStepUp(): void {
    const currentNum = this.lastNumericValue ?? (this.entry !== null && this.entry !== undefined ? Number(this.entry) : (this.hasMin() && this.getMinValue() > 0 ? this.getMinValue() - this.getStepValue() : 0));
    const nextVal = currentNum + this.getStepValue();
    if (this.hasMin() && nextVal < this.getMinValue()) {
      this.updateValue(this.getMinValue());
    } else {
      this.updateValue(nextVal);
    }
  }

  onStepDown(): void {
    const currentNum = this.lastNumericValue ?? (this.entry !== null && this.entry !== undefined ? Number(this.entry) : (this.hasMin() && this.getMinValue() > 0 ? this.getMinValue() : 0));
    if (this.hasMin() && currentNum <= this.getMinValue()) {
      return;
    }
    const nextVal = currentNum - this.getStepValue();
    if (this.hasMin() && nextVal < this.getMinValue()) {
      this.updateValue(this.getMinValue());
    } else {
      this.updateValue(nextVal);
    }
  }

  onKeyDown(event: KeyboardEvent): void {
    this.keyPress.emit(event);

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.onStepUp();
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.onStepDown();
      return;
    }

    const allowedKeys = [
      'Backspace', 'Tab', 'Enter', 'Escape', 'Delete',
      'ArrowLeft', 'ArrowRight', 'Home', 'End'
    ];

    if (allowedKeys.includes(event.key)) {
      return;
    }

    if (event.ctrlKey || event.metaKey) {
      return;
    }

    if (/^[0-9]$/.test(event.key)) {
      return;
    }

    const target = event.target as HTMLInputElement;
    const currentVal = target ? target.value : '';
    const selectionStart = target?.selectionStart ?? 0;
    const selectionEnd = target?.selectionEnd ?? 0;

    if (event.key === '-' && !currentVal.includes('-')) {
      if (selectionStart === 0 && (!this.hasMin() || this.getMinValue() < 0)) {
        return;
      }
    }

    if (event.key === '.' || event.key === ',') {
      const futureValue = currentVal.slice(0, selectionStart) + event.key + currentVal.slice(selectionEnd);
      if (this.countDecimalSeparators(futureValue) <= 1) {
        return;
      }
    }

    event.preventDefault();
  }

  onPaste(event: ClipboardEvent): void {
    const pastedData = event.clipboardData?.getData('text');
    const target = event.target as HTMLInputElement;
    const currentVal = target?.value ?? '';
    const selectionStart = target?.selectionStart ?? 0;
    const selectionEnd = target?.selectionEnd ?? 0;

    if (pastedData) {
      const futureValue = currentVal.slice(0, selectionStart) + pastedData + currentVal.slice(selectionEnd);
      if (this.countDecimalSeparators(futureValue) > 1) {
        event.preventDefault();
        return;
      }
      const sanitized = this.normalizeNumberString(futureValue);
      if (isNaN(Number(sanitized)) || /^\s*$/.test(sanitized)) {
        event.preventDefault();
        return;
      }
      if (this.hasMin() && this.getMinValue() >= 0 && Number(sanitized) < 0) {
        event.preventDefault();
        return;
      }
    }
  }

  onInput(event: Event) {
    const target = event.target as HTMLInputElement;
    const rawValue = target.value;

    const normalized = this.normalizeNumberString(rawValue);

    if (this.countDecimalSeparators(normalized) > 1 || (isNaN(Number(normalized)) && normalized !== '' && normalized !== '-' && normalized !== '.')) {
      target.value = this.displayString;
      return;
    }

    this.displayString = rawValue;

    if (rawValue === '' || rawValue === '-' || rawValue === '.' || rawValue === ',') {
      this.entry = undefined;
      this.lastNumericValue = null;
      this.entryChange.emit(undefined);
      return;
    }

    const sanitized = Number(normalized);
    const rounded = Number(sanitized.toFixed(10));
    this.entry = rounded;
    this.lastNumericValue = rounded;
    this.entryChange.emit(rounded);
  }

  ngOnInit(): void {
    this.componentId = this.id;
    this.syncDisplayFromEntry();
    if (this.entry !== null && this.entry !== undefined && !isNaN(Number(this.entry))) {
      this.lastNumericValue = Number(this.entry);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['entry']) {
      this.syncDisplayFromEntry();
      if (this.entry !== null && this.entry !== undefined && !isNaN(Number(this.entry))) {
        this.lastNumericValue = Number(this.entry);
      }
    }
  }
}
