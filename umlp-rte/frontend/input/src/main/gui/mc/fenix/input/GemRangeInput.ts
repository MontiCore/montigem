/* (c) https://github.com/MontiCore/monticore */
import {
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  HostListener,
  Output,
  ViewChild,
  SimpleChanges,
  ViewChildren,
  QueryList,
  AfterViewInit,
  OnChanges,
  OnDestroy, ChangeDetectionStrategy
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { config, GemRangeInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemRangeInputComponent';
import { Marker } from '../../../../../../build/gui2java2ts/mc/fenix/input/gemrangeinputtypes/Marker';
import { Subscription } from 'rxjs';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, FormsModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemRangeInput extends GemRangeInputComponent implements AfterViewInit, OnChanges, OnDestroy {
  private firstTime: boolean = true;

  @HostBinding('style.display') hostDisplay = 'inline-block';
  @HostBinding('style.width.px') hostWidthPx?: number;
  @HostBinding('class.vertical') get hostIsVertical() { return !!this.vertical; }
  private readonly minVerticalWidthPx = 56;

  @HostBinding('attr.id')
  componentId!: string;

  @ViewChild('slider', { static: false })
  sliderEl!: ElementRef<HTMLInputElement>;

  @ViewChild('rangeVElement', { static: false })
  rangeVElement!: ElementRef<HTMLElement>;

  @ViewChild('titleLabel', { static: false })
  titleLabelEl!: ElementRef<HTMLElement>;

  @ViewChildren('tickLabel', { read: ElementRef })
  tickLabelEls!: QueryList<ElementRef<HTMLElement>>;

  private tickLabelsSub?: Subscription;

  @Output()
  valueChange = new EventEmitter<number>();

  ngOnChanges(changes: SimpleChanges): void {
    if (
      typeof this.minValue === 'number' &&
      typeof this.maxValue === 'number'
    ) {
      this.firstTime = true;
      const initial = (typeof this.value === 'number' && this.value !== this.minValue && this.value > this.minValue && this.value <= this.maxValue) ? this.value : this.minValue;
      this.setValue(initial);
      this.valueChange.emit(initial);

      if (this.vertical) {
        setTimeout(() => this.adjustVerticalWidth());
      }
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.adjustVerticalLabelMargins();
      this.adjustVerticalWidth();
    });

    this.tickLabelsSub = this.tickLabelEls.changes.subscribe(() => {
      setTimeout(() => {
        this.adjustVerticalLabelMargins();
        this.adjustVerticalWidth();
      });
    });
  }

  ngOnDestroy(): void {
    this.tickLabelsSub?.unsubscribe();
  }

  @HostListener('window:resize')
  onResize() {
    if (this.vertical) this.adjustVerticalWidth();
  }

  onValueChange(event: number) {
    if (event === undefined) { return; }
    this.value = event;
    this.setValue(this.value);
    this.valueChange.emit(this.value);
  }

  snapToTick(): void {
    if (!this.snapping) return;
    if (!this.ticks || this.ticks.length === 0) return;

    const tickValues = this.ticks.map((marker: Marker) => marker.getValue());
    tickValues.push(this.minValue, this.maxValue);

    let nearest = tickValues[0];
    let minDiff = Math.abs(this.value - nearest);
    for (const v of tickValues) {
      const diff = Math.abs(this.value - v);
      if (diff < minDiff) {
        minDiff = diff;
        nearest = v;
      }
    }

    this.setValue(nearest);
    this.valueChange.emit(this.value);
  }

  override setValue(v: number): void {
    super.setValue(v);
    if (!this.rangeVElement) return;

    if (!this.firstTime) {
      this.rangeVElement.nativeElement.innerHTML = `<span>${this.value}</span>`;
      this.rangeVElement.nativeElement.style.left = `${this.getThumbPosPxFor(v) + 2}px`;
    } else {
      this.firstTime = false;
      setTimeout(() => {
        this.rangeVElement.nativeElement.innerHTML = `<span>${this.value}</span>`;
        this.rangeVElement.nativeElement.style.left = `${this.getThumbPosPxFor(v) + 2}px`;
      });
    }
  }

  get sliderBottomLength(): string {
    return this.vertical ? '100%' : '2em';
  }

  getTickPosition(marker: Marker): string {
    return `${this.getThumbPosPxFor(marker.getValue()) + 2}px`;
  }

  get visibleTicks() {
    if (!this.ticks) return [];
    const markerMap = new Map<number, Marker>();
    for (const marker of this.ticks) {
      if (marker.getValue() >= this.minValue && marker.getValue() <= this.maxValue) {
        markerMap.set(marker.getValue(), marker);
      }
    }
    return Array.from(markerMap.values());
  }

  public getThumbPosPxFor(value: number): number {
    const input = this.sliderEl?.nativeElement;
    if (!input) return 0;

    const min = this.minValue;
    const max = this.maxValue;
    const v = Math.min(max, Math.max(min, value)); // clamp

    const rect = input.getBoundingClientRect();
    const trackPx = this.vertical ? rect.height : rect.width;

    const cs = getComputedStyle(input);
    const thumbSizePx = parseFloat(cs.getPropertyValue('--thumb-size')) || 16;

    const usable = Math.max(0, trackPx - thumbSizePx);

    const denom = (max - min) || 1;
    const t = (v - min) / denom;

    const posPx = (thumbSizePx / 2) + t * usable;
    return posPx;
  }

  private adjustVerticalWidth(): void {
    if (!this.vertical) return;

    const input = this.sliderEl?.nativeElement;
    const cs = input ? getComputedStyle(input) : undefined;
    const thumbSizePx = cs ? parseFloat(cs.getPropertyValue('--thumb-size')) || 16 : 16;

    const gapPx = 12;

    let maxTickLabelWidth = 0;
    if (this.tickLabelEls?.length) {
      this.tickLabelEls.forEach(ref => {
        const w = ref.nativeElement.getBoundingClientRect().width;
        if (w > maxTickLabelWidth) maxTickLabelWidth = w;
      });
    }

    let titleWidth = 0;
    if (this.titleLabelEl?.nativeElement) {
      titleWidth = this.titleLabelEl.nativeElement.getBoundingClientRect().width;
    }

    const farthestLabelWidth = Math.max(maxTickLabelWidth, titleWidth);

    const needed = Math.max(
      this.minVerticalWidthPx,
      Math.ceil(thumbSizePx + gapPx + farthestLabelWidth)
    );

    this.hostWidthPx = needed;
  }

  private adjustVerticalLabelMargins(): void {
    if (!this.vertical) return;
    if (!this.tickLabelEls || this.tickLabelEls.length === 0) return;

    this.tickLabelEls.forEach(labelEl => {
      const el = labelEl.nativeElement;
      const text = el.innerText || '';
      const length = text.length;

      const style = getComputedStyle(el);
      const fontSizePx = parseFloat(style.fontSize) || 12; // Fallback: 12px

      const factor = 0.3;
      const margin = length * fontSizePx * factor;

      el.style.marginTop = `${margin}px`;
    });
  }
}
