/* (c) https://github.com/MontiCore/monticore */
import {
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  HostListener,
  Output,
  ViewChild,
  ViewChildren,
  QueryList,
  AfterViewInit,
  OnChanges,
  OnDestroy,
  SimpleChanges, ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { config, GemDualRangeInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemDualRangeInputComponent';
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
export class GemDualRangeInput extends GemDualRangeInputComponent
  implements AfterViewInit, OnChanges, OnDestroy {

  @HostBinding('style.display') hostDisplay = 'inline-block';
  @HostBinding('style.width.px') hostWidthPx?: number;
  @HostBinding('class.vertical') get hostIsVertical() { return !!this.vertical; }
  private readonly minVerticalWidthPx = 56;

  @HostBinding('attr.id') componentId!: string;

  @ViewChild('sliderLow', { static: false })
  sliderLowEl!: ElementRef<HTMLInputElement>;

  @ViewChild('sliderHigh', { static: false })
  sliderHighEl!: ElementRef<HTMLInputElement>;

  @ViewChild('rangeVStart', { static: false })
  rangeVStartEl!: ElementRef<HTMLElement>;

  @ViewChild('rangeVEnd', { static: false })
  rangeVEndEl!: ElementRef<HTMLElement>;

  @ViewChild('titleLabel', { static: false })
  titleLabelEl!: ElementRef<HTMLElement>;

  @ViewChildren('tickLabel', { read: ElementRef })
  tickLabelEls!: QueryList<ElementRef<HTMLElement>>;

  private tickLabelsSub?: Subscription;
  private firstTime = true;

  @Output() startValueChange = new EventEmitter<number>();
  @Output() endValueChange = new EventEmitter<number>();
  @Output() rangeChange = new EventEmitter<{ start: number; end: number }>();

  ngOnChanges(changes: SimpleChanges): void {
    if (
      typeof this.minValue === 'number' &&
      typeof this.maxValue === 'number'
    ) {
      this.firstTime = true;

      const lo =
        typeof this.startValue === 'number'
          ? Math.max(this.minValue, Math.min(this.startValue, this.maxValue))
          : this.minValue;

      const hi =
        typeof this.endValue === 'number'
          ? Math.max(lo, Math.min(this.endValue, this.maxValue))
          : this.maxValue;

      this.setStart(lo);
      this.setEnd(hi);
      this.emitAll();

      if (this.vertical) {
        setTimeout(() => this.adjustVerticalWidth());
      }
    }
  }

  ngAfterViewInit(): void {
    this.tickLabelsSub = this.tickLabelEls?.changes.subscribe(() =>
      setTimeout(() => {
        this.adjustVerticalLabelMargins();
        this.adjustVerticalWidth();
      })
    );
    setTimeout(() => {
      this.adjustVerticalLabelMargins();
      this.adjustVerticalWidth();
    });
  }

  ngOnDestroy(): void {
    this.tickLabelsSub?.unsubscribe();
  }

  @HostListener('window:resize')
  onResize() {
    if (this.vertical) this.adjustVerticalWidth();
  }

  onStartChange(v: number) {
    const loRaw = Math.max(this.minValue, Math.min(v ?? this.minValue, this.maxValue));
    const hi = this.endValue ?? this.maxValue;

    if (this.snapping && this.ticks?.length) {
      const ticks = this.getTickValuesSorted();
      let loIdx = this.nearestTickIndexFor(loRaw, ticks);
      let hiIdx = this.nearestTickIndexFor(hi, ticks);
      if (loIdx >= hiIdx) {
        // mind. 1 Tick Abstand
        loIdx = Math.max(0, hiIdx - 1);
      }
      this.setStart(this.valueAtTickIndex(loIdx, ticks));
    } else {
      const step = Number(this.step ?? 1);
      const lo = loRaw > (hi - step) ? Math.max(this.minValue, hi - step) : loRaw;
      this.setStart(lo);
    }

    if (this.sliderLowEl?.nativeElement) {
      this.sliderLowEl.nativeElement.value = String(this.startValue);
    }
    this.emitAll();
  }

  onEndChange(v: number) {
    const hiRaw = Math.max(this.minValue, Math.min(v ?? this.maxValue, this.maxValue));
    const lo = this.startValue ?? this.minValue;

    if (this.snapping && this.ticks?.length) {
      const ticks = this.getTickValuesSorted();
      let loIdx = this.nearestTickIndexFor(lo, ticks);
      let hiIdx = this.nearestTickIndexFor(hiRaw, ticks);
      if (hiIdx <= loIdx) {
        hiIdx = Math.min(ticks.length - 1, loIdx + 1);
      }
      this.setEnd(this.valueAtTickIndex(hiIdx, ticks));
    } else {
      const step = Number(this.step ?? 1);
      const hi = hiRaw < (lo + step) ? Math.min(this.maxValue, lo + step) : hiRaw;
      this.setEnd(hi);
    }

    if (this.sliderHighEl?.nativeElement) {
      this.sliderHighEl.nativeElement.value = String(this.endValue);
    }
    this.emitAll();
  }

  snapToTick(): void {
    if (!this.snapping || !this.ticks?.length) return;

    const ticks = this.getTickValuesSorted();
    if (ticks.length < 2) return;

    let loIdx = this.nearestTickIndexFor(this.startValue ?? this.minValue, ticks);
    let hiIdx = this.nearestTickIndexFor(this.endValue ?? this.maxValue, ticks);

    if (loIdx > hiIdx) {
      const t = loIdx; loIdx = hiIdx; hiIdx = t;
    }
    if (hiIdx - loIdx < 1) {
      if (hiIdx + 1 < ticks.length) {
        hiIdx = hiIdx + 1;
      } else if (loIdx - 1 >= 0) {
        loIdx = loIdx - 1;
      } else {
      }
    }

    const snappedLo = this.valueAtTickIndex(loIdx, ticks);
    const snappedHi = this.valueAtTickIndex(hiIdx, ticks);

    this.setStart(snappedLo);
    this.setEnd(snappedHi);

    if (this.sliderLowEl?.nativeElement) {
      this.sliderLowEl.nativeElement.value = String(this.startValue);
    }
    if (this.sliderHighEl?.nativeElement) {
      this.sliderHighEl.nativeElement.value = String(this.endValue);
    }

    this.emitAll();
  }

  private setStart(v: number) {
    this.startValue = v;
    if (!this.rangeVStartEl || !this.sliderLowEl) return;

    const setBubble = () => {
      this.rangeVStartEl.nativeElement.innerHTML = `<span>${this.startValue}</span>`;
      this.rangeVStartEl.nativeElement.style.left = `${
        this.getThumbPosPxFor(this.sliderLowEl.nativeElement, v)}px`;
    };

    if (!this.firstTime) {
      setBubble();
    } else {
      this.firstTime = false;
      setTimeout(setBubble);
    }
  }

  private setEnd(v: number) {
    this.endValue = v;
    if (!this.rangeVEndEl || !this.sliderLowEl) return;

    const setBubble = () => {
      this.rangeVEndEl.nativeElement.innerHTML = `<span>${this.endValue}</span>`;
      this.rangeVEndEl.nativeElement.style.left = `${
        this.getThumbPosPxFor(this.sliderLowEl.nativeElement, v)}px`;
    };

    setTimeout(setBubble);
  }

  get sliderBottomLength(): string {
    return this.vertical ? '100%' : '2em';
  }

  get visibleTicks(): Marker[] {
    if (!this.ticks) return [];
    const m = new Map<number, Marker>();
    for (const marker of this.ticks as Marker[]) {
      const val = marker.getValue();
      if (val >= this.minValue && val <= this.maxValue) m.set(val, marker);
    }
    return Array.from(m.values());
  }

  getTickPosition(marker: Marker): string {
    return `${this.getThumbPosPxFor(this.sliderLowEl?.nativeElement, marker.getValue())}px`;
  }

  private getThumbPosPxFor(input: HTMLInputElement | undefined, value: number): number {
    if (!input) return 0;
    const min = this.minValue;
    const max = this.maxValue;
    const v = Math.min(max, Math.max(min, value));

    const rect = input.getBoundingClientRect();
    const trackPx = this.vertical ? rect.height : rect.width;

    const cs = getComputedStyle(input);
    const thumbSizePx = parseFloat(cs.getPropertyValue('--thumb-size')) || 16;
    const usable = Math.max(0, trackPx - thumbSizePx);

    const denom = (max - min) || 1;
    const t = (v - min) / denom;

    return thumbSizePx / 2 + t * usable;
  }

  private adjustVerticalWidth(): void {
    if (!this.vertical) return;

    const input = this.sliderLowEl?.nativeElement;
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
    if (!this.vertical || !this.tickLabelEls?.length) return;
    this.tickLabelEls.forEach(labelEl => {
      const el = labelEl.nativeElement;
      const length = (el.innerText || '').length;
      const fontSizePx = parseFloat(getComputedStyle(el).fontSize) || 12;
      el.style.marginTop = `${length * fontSizePx * 0.3}px`;
    });
  }

  private emitAll(): void {
    this.startValueChange.emit(this.startValue ?? this.minValue);
    this.endValueChange.emit(this.endValue ?? this.maxValue);
    this.rangeChange.emit({
      start: this.startValue ?? this.minValue,
      end: this.endValue ?? this.maxValue,
    });
  }

  private getTickValuesSorted(): number[] {
    const set = new Set<number>();
    set.add(this.minValue);
    set.add(this.maxValue);
    if (this.ticks?.length) {
      for (const m of this.ticks as Marker[]) {
        const v = m.getValue();
        if (v >= this.minValue && v <= this.maxValue) set.add(v);
      }
    }
    return Array.from(set.values()).sort((a, b) => a - b);
  }

  private nearestTickIndexFor(val: number, ticks: number[]): number {
    let idx = 0;
    let bestDiff = Math.abs(val - ticks[0]);
    for (let i = 1; i < ticks.length; i++) {
      const d = Math.abs(val - ticks[i]);
      if (d < bestDiff) { bestDiff = d; idx = i; }
    }
    return idx;
  }

  private valueAtTickIndex(idx: number, ticks: number[]): number {
    const i = Math.max(0, Math.min(idx, ticks.length - 1));
    return ticks[i];
  }
}
