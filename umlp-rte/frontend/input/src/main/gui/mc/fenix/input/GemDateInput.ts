/* (c) https://github.com/MontiCore/monticore */

import {
  AfterViewInit,
  Component,
  HostBinding,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ElementRef, ChangeDetectionStrategy,
} from '@angular/core';
import { config, GemDateInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemDateInputComponent';

import flatpickr from 'flatpickr';
import {LocalDateTimeHelper, Temporal} from '@umlp/commonj2ts';
import {MatIconModule} from "@angular/material/icon";
import {MatTooltipModule} from "@angular/material/tooltip";

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, MatIconModule, MatTooltipModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemDateInput extends GemDateInputComponent implements AfterViewInit, OnDestroy, OnInit {

  @ViewChild('dateInput', { static: false }) dateInput!: ElementRef<HTMLInputElement>;
  private pickerElement: HTMLInputElement;

  private static readonly DATE_FORMAT = "d.m.Y";
  private static readonly TIME_FORMAT = "H:i";

  private picker: flatpickr.Instance | null = null;
  // the entry as Date for flatpickr
  private _pickerEntry: Date;

  dateTimeEnabled: boolean = false;
  timeOnlyEnabled: boolean = false;

  override _id: string = Math.random() + '';
  override _labelText: string = "";
  override _disabled: boolean = false;

  @HostBinding("attr.id")
  componentId: string;

  @HostBinding("style.width")
  _width: string = '100%';

  @HostBinding("style.height")
  _height: string = '100%';

  @Input()
  override set disabled(isDisabled: boolean) {
    if (this.dateInput && isDisabled) {
      this.dateInput.nativeElement.disabled = isDisabled;
    }
  }

  get pickerEntry() {
    return this._pickerEntry;
  }

  @Input()
  override set entry(dateTime: Temporal.PlainDateTime) {
    this._entry = dateTime;
    this._pickerEntry = dateTime ? LocalDateTimeHelper.toDate(dateTime) : null;
    if (this.picker) {
      this.picker.setDate(this._pickerEntry);
    }
  }

  @Input()
  override set showDateTime(dateTimeEnabled: boolean) {
    this.dateTimeEnabled = dateTimeEnabled;
    this.reinitPicker();
  }

  @Input()
  override set showTimeOnly(timeOnlyEnabled: boolean) {
    this.timeOnlyEnabled = timeOnlyEnabled;
    this.reinitPicker();
  }

  ngOnInit(): void {
    this.componentId = this.id;
  }

  ngAfterViewInit(): void {
    this.initPicker();
  }

  ngOnDestroy() {
    this.picker?.destroy();
  }

  private initPicker() {
    this.pickerElement = this.dateInput.nativeElement;
    this.createPicker();
  }

  private reinitPicker() {
    if (!this.dateInput) return;
    this.picker?.destroy();
    this.createPicker();
  }

  private createPicker() {
    this.picker = flatpickr(this.pickerElement, {
      enableTime: this.timeOnlyEnabled || this.dateTimeEnabled,
      noCalendar: this.timeOnlyEnabled,
      dateFormat: this.getFormat(),
      time_24hr: true,
      allowInput: true,
      onChange: ([date]) => {
        this.onDateTimeChange(date);
      },
      onClose: ([date]) => {
        this.onDateTimeChange(date);
      }
    });
    this.picker.setDate(this.pickerEntry);
  }

  /**
   * the entry with the fields the picker shows (the date, hours and minutes) from the picked date; the others, e.g. the
   * seconds and their fraction, stay
   */
  private onDateTimeChange(date: Date) {
    if (!date) {
      this.entryChange.emit(null);
      return;
    }
    const picked = LocalDateTimeHelper.fromDate(date);
    const dateFields = {year: picked.year, month: picked.month, day: picked.day};
    const timeFields = {hour: picked.hour, minute: picked.minute};
    const current = this._entry
        ?? (this.timeOnlyEnabled ? Temporal.Now.plainDateISO() : picked.toPlainDate()).toPlainDateTime();
    if (this.timeOnlyEnabled) {
      this.entryChange.emit(current.with(timeFields));
    } else if (!this.dateTimeEnabled) {
      this.entryChange.emit(current.with(dateFields));
    } else {
      this.entryChange.emit(current.with({...dateFields, ...timeFields}));
    }
  }

  private getFormat(): string {
    if (!this.timeOnlyEnabled && this.dateTimeEnabled) {
      return `${GemDateInput.DATE_FORMAT} ${GemDateInput.TIME_FORMAT}`
    }
    else if (this.timeOnlyEnabled) {
      return GemDateInput.TIME_FORMAT;
    }
    return GemDateInput.DATE_FORMAT;
  }

}
