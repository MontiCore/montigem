/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, EventEmitter, HostBinding, Input, Output} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { config, GemTextInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemTextInputComponent';
import { GemTextInputMode } from './GemTextInputMode';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, FormsModule, GemTextInputMode, MatTooltipModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemTextInput extends GemTextInputComponent {
  override _id: string = Math.random() + '';
  override _labelText: string = "";
  override _inputType: string = "text";
  override _disabled: boolean = false;
  override _placeholder: string = "";
  override _maxLength: number = 524800;

  isNumeric: boolean = false;

  @HostBinding("attr.id")
  componentId: string;

  @HostBinding("style.width")
  _width: string = '100%';

  @HostBinding("style.height")
  _height: string = '100%';

  @Output() public entryChange = new EventEmitter<string>();
  @Output() public override keyRelease = new EventEmitter<KeyboardEvent>();
  @Output() public override keyPress = new EventEmitter<KeyboardEvent>();

  override get inputType(): string {
    return this._inputType;
  }

  @Input()
  override set inputType(inputType: string) {
    if (inputType === "numericText") {
      this._inputType = "text";
      this.isNumeric = true;
    } else {
      this._inputType = inputType;
    }
  }

  get displayValue(): string {
    if (!this.entry || typeof this.entry !== 'string') return this.entry || '';

    let datePart = '';
    let timePart = '';

    const dateMatch = this.entry.match(/(\d{1,4})-(\d{1,2})-(\d{1,2})/);
    if (dateMatch) {
      const y = dateMatch[1].padStart(4, '0');
      const m = dateMatch[2].padStart(2, '0');
      const d = dateMatch[3].padStart(2, '0');
      datePart = `${y}-${m}-${d}`;
    }

    const timeMatch = this.entry.match(/(\d{1,2}):(\d{1,2})/);
    if (timeMatch) {
      const h = timeMatch[1].padStart(2, '0');
      const min = timeMatch[2].padStart(2, '0');
      timePart = `${h}:${min}`;
    }

    if (this.inputType === 'date') {
      return datePart;
    }
    if (this.inputType === 'time') {
      return timePart;
    }
    if (this.inputType === 'datetime-local') {
      if (datePart) {
        const safeTime = timePart ? timePart : '00:00';
        return `${datePart}T${safeTime}`;
      }
      return '';
    }
    return this.entry;
  }

  onEntryChange(event: string) {
    if (!event) {
      this.entryChange.emit(event);
      return;
    }

    let emittedValue = event;
    let datePart = '1970-01-01';
    let timePart = '00:00:00';

    if (this.entry && typeof this.entry === 'string') {
      const dateMatch = this.entry.match(/(\d{1,4})-(\d{1,2})-(\d{1,2})/);
      if (dateMatch) {
        datePart = `${dateMatch[1].padStart(4, '0')}-${dateMatch[2].padStart(2, '0')}-${dateMatch[3].padStart(2, '0')}`;
      }

      const timeMatch = this.entry.match(/(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?/);
      if (timeMatch) {
        const s = timeMatch[3] ? timeMatch[3].padStart(2, '0') : '00';
        timePart = `${timeMatch[1].padStart(2, '0')}:${timeMatch[2].padStart(2, '0')}:${s}`;
      }
    }

    if (this.inputType === 'date') {
      emittedValue = `${event}T${timePart}`;
    } else if (this.inputType === 'time') {
      let newTime = event;
      if (newTime && newTime.split(':').length === 2) {
        newTime += ':00';
      }
      emittedValue = `${datePart}T${newTime}`;
    } else if (this.inputType === 'datetime-local') {
      let newTime = event.split('T')[1];
      if (newTime && newTime.split(':').length === 2) {
        event += ':00';
      }
      emittedValue = event;
    }

    this.entryChange.emit(emittedValue);
  }

  ngOnInit(): void {
    this.componentId = this.id;
  }
}
