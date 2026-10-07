/* (c) https://github.com/MontiCore/monticore */

import {
  Component,
  Input,
  ElementRef,
  ChangeDetectorRef,
  OnInit,
  AfterViewInit,
  OnDestroy,
  HostBinding,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemGaugeChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemGaugeChartComponent';
import { GemGaugeChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemgaugecharttypes/GemGaugeChartData';
import { autoColors } from './colors';
import { GemChartOptions } from './GemChartOptions';
import { PlotlyModule } from 'angular-plotly.js';
import * as Plotly from 'plotly.js-dist-min';
import { setupResizeObserver } from './ResizeObserverUtil';

PlotlyModule.plotlyjs = Plotly;

@Component({
  standalone: true,
  imports: [...config.imports, CommonModule, GemChartOptions, PlotlyModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GemGaugeChart extends GemGaugeChartComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() height: number = 400;

  @HostBinding('style.height.px')
  get hostHeight() { return this.height || 400; }

  @HostBinding('style.max-height')
  get hostMaxHeight() { return '100%'; }

  @HostBinding('style.width')
  get hostWidth() { return '100%'; }

  @HostBinding('style.display')
  get hostDisplay() { return 'block'; }

  public plotlyData: any[] = [];

  public _layout: any = {
    autosize: true,
    margin: { t: 55, b: 15, l: 35, r: 35 },
    paper_bgcolor: 'transparent'
  };

  public _config: any = {
    displaylogo: false,
    responsive: true,
    displayModeBar: false
  };

  private resizeObserver!: ResizeObserver;

  public _data!: GemGaugeChartData;
  public _min: number = 0;
  public _max: number = 100;
  public _unit: string = '';
  public _textValue: string = '';
  public _textPosition: string = 'center';

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  ngOnInit(): void {
    super.ngOnInit();
  }

  ngAfterViewInit() {
    this.resizeObserver = setupResizeObserver(this.el.nativeElement, () => this.handleResize());
  }

  ngOnDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
  }

  private handleResize() {
    // autosize only measures the container on the first render; each gauge is its own plot, so all of
    // them have to be told to take the size of their (flex) container again
    const plots = this.el.nativeElement.querySelectorAll('.js-plotly-plot');
    plots.forEach((plot: HTMLElement) => (Plotly as any).Plots.resize(plot));
  }

  @Input()
  public set data(data: GemGaugeChartData) {
    if (!data) return;
    this._data = data;
    this.updatePlotData();
  }

  @Input()
  public set min(val: number) {
    this._min = val !== undefined ? val : 0;
    this.updatePlotData();
  }

  @Input()
  public set max(val: number) {
    this._max = val !== undefined ? val : 100;
    this.updatePlotData();
  }

  @Input()
  public set unit(val: string) {
    this._unit = val;
    this.updatePlotData();
  }

  @Input()
  public set textValue(val: string) {
    this._textValue = val;
    this.cdr.detectChanges();
  }

  @Input()
  public set textPosition(val: string) {
    this._textPosition = val;
    this.cdr.detectChanges();
  }

  /** applies a new minimum from the settings menu; empty, invalid or not below max resets the input */
  public changeMin(input: HTMLInputElement) {
    const val = input.value === '' ? NaN : Number(input.value);
    if (Number.isFinite(val) && val < this._max) {
      this.min = val;
    } else {
      input.value = String(this._min);
    }
  }

  /** applies a new maximum from the settings menu; empty, invalid or not above min resets the input */
  public changeMax(input: HTMLInputElement) {
    const val = input.value === '' ? NaN : Number(input.value);
    if (Number.isFinite(val) && val > this._min) {
      this.max = val;
    } else {
      input.value = String(this._max);
    }
  }

  public changeUnit(val: string) {
    this.unit = val;
  }

  private wrapText(text: string, maxLen: number = 25): string {
    if (!text) return '';
    let words = text.split(' ');
    let lines = [];
    let currentLine = '';

    words.forEach(word => {
      if ((currentLine + word).length > maxLen) {
        if (currentLine) lines.push(currentLine.trim());
        currentLine = word + ' ';
      } else {
        currentLine += word + ' ';
      }
    });
    if (currentLine) lines.push(currentLine.trim());

    return lines.join('<br>');
  }

  private updatePlotData() {
    if (!this._data || !this._data.getEntriesList()) return;
    let entries = this._data.getEntriesList();
    let traces = [];

    if (this._layout.grid) {
      delete this._layout.grid;
    }

    let entriesArray = entries.__internalArray ? entries.__internalArray() : [];
    for (let i = 0; i < entriesArray.length; i++) {
      let entry = entriesArray[i];
      let color = entry.getBackgroundColor();

      if (!color || !CSS.supports('color', color)) {
        color = '#1f77b4';
      }

      traces.push({
        type: 'indicator',
        mode: 'gauge+number',
        value: entry.getValue(),
        title: { text: this.wrapText(entry.getName(), 25), font: { size: 10 } },
        number: { font: { size: 14 }, suffix: this._unit ? " " + this._unit : "" },
        gauge: {
          axis: { range: [this._min, this._max], tickwidth: 1 },
          bar: { color: color },
          bgcolor: "white",
          borderwidth: 1,
          bordercolor: "gray"
        }
      });
    }

    this.plotlyData = traces;
    this._layout = { ...this._layout };
    this.cdr.detectChanges();
  }
}
