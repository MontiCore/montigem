/* (c) https://github.com/MontiCore/monticore */

import { Component, Input, OnInit, ChangeDetectionStrategy, ElementRef, AfterViewInit, OnDestroy, ChangeDetectorRef, ViewChild, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemCandlestickChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemCandlestickChartComponent';
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
export class GemCandlestickChart extends GemCandlestickChartComponent implements OnInit, AfterViewInit, OnDestroy {

  @HostBinding('style.display') get hostDisplay() { return 'block'; }
  @HostBinding('style.width') get hostWidth() { return '100%'; }
  @HostBinding('style.height') get hostHeight() { return '100%'; }

  private resizeObserver!: ResizeObserver;

  @ViewChild('innerContainer') innerContainer!: ElementRef;
  @ViewChild('outerContainer') outerContainer!: ElementRef;

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  period: boolean = false;
  dateFormat: boolean = false;
  chartType: boolean = false;

  autoMargins: boolean = true;
  rightMargin: number = 20;
  topMargin: number = 20;
  bottomMargin: number = 40;
  leftMargin: number = 50;

  fourClicked: boolean = false;
  builtinButtons: boolean = false;

  private readonly COLOR_DECREASING = '#cc071e';
  private readonly COLOR_INCREASING = '#11750e';

  _data: any = [
    {
      type: "candlestick",
      x: [],
      close: [],
      open: [],
      high: [],
      low: [],
      decreasing: { line: { color: this.COLOR_DECREASING } },
      increasing: { line: { color: this.COLOR_INCREASING } },
      xaxis: 'x',
      yaxis: 'y'
    }
  ];

  _layout: any = {
    newLayout: null,
    dragmode: 'zoom',
    margin: {
      r: this.rightMargin,
      t: this.topMargin,
      b: this.bottomMargin,
      l: this.leftMargin
    },
    xaxis: {
      range: [],
      tickformat: null,
      rangeslider: { visible: true },
      rangeselector: { buttons: this.showButtons() },
      type: 'date'
    },
    yaxis: {
      autorange: true,
      type: 'linear'
    }
  };

  _config: any = {
    displaylogo: false,
    displayModeBar: false,
    responsive: true
  };

  ngAfterViewInit() {
    this.resizeObserver = setupResizeObserver(this.el.nativeElement, (w, h) => this.handleResize(w, h));
  }

  ngOnDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
  }

  private handleResize(width: number, height: number) {
    if (width === 0 || height === 0) return;

    if (height < 250) {
      this._layout.xaxis.rangeslider.visible = false;
    } else {
      this._layout.xaxis.rangeslider.visible = true;
    }

    if (this.autoMargins) {
      this.leftMargin = Math.max(30, Math.min(50, width * 0.1));
      this.rightMargin = Math.max(15, Math.min(30, width * 0.05));

      /* Increase minimum top margin from 10 to 30 to prevent SVG modebar and axis label overflow during dynamic resizing */
      this.topMargin = Math.max(30, height * 0.05);

      let baseBottom = this._layout.xaxis.rangeslider.visible ? 40 : 25;
      this.bottomMargin = Math.max(baseBottom, height * 0.15);

      this._layout.margin.l = this.leftMargin;
      this._layout.margin.r = this.rightMargin;
      this._layout.margin.b = this.bottomMargin;
      this._layout.margin.t = this.topMargin;
    }

    this._layout.width = width;
    this._layout.height = height;

    this.updatePlot();
    this.cdr.detectChanges();
  }

  private updatePlot(): void {
    this._data = [...this._data];
    this._layout = { ...this._layout };
  }


  public showButtons(): any[] {
    if (this.builtinButtons) {
      return [
        { step: 'day', stepmode: 'backward', count: 7, label: '1 week' },
        { step: 'day', stepmode: 'backward', count: 14, label: '2 weeks' },
        { step: 'month', stepmode: 'backward', count: 1, label: '1 month' },
        { step: 'month', stepmode: 'backward', count: 3, label: '3 months' },
        { step: 'month', stepmode: 'backward', count: 6, label: '6 months' },
        { step: 'year', stepmode: 'backward', count: 1, label: '1 year' },
        { step: 'all', label: 'All dates' }
      ];
    } else {
      return [];
    }
  }

  public applyLayout(range: string[], tick: string | null) {
    this._layout = {
      ...this._layout,
      xaxis: {
        ...this._layout.xaxis,
        range: range,
        // Plotly writes autorange: true back into the layout on the first render, which would override the range
        autorange: !(range && range.length === 2),
        tickformat: tick,
        rangeselector: {
          buttons: this.showButtons()
        }
      }
    };
    this.updatePlot();
    this.cdr.detectChanges();
  }

  public updateLayout(update: number) {
    if (!this._data[0].x || this._data[0].x.length === 0) return;

    if (update === 1) {
      let lastX = this._data[0].x[this._data[0].x.length - 1];
      let newFirstDate: Date = this.stringToDate(lastX);
      let newLastDate: Date = this.stringToDate(lastX);

      newFirstDate.setDate(newFirstDate.getDate() - 7);
      let firstDate = this.dateToString(newFirstDate);
      let lastDate = this.dateToString(newLastDate);

      this.applyLayout([firstDate, lastDate], this._layout.xaxis.tickformat);
    }
    else if (update === 2) {
      this._layout.xaxis.tickformat = "%b %d, %Y";
      this._layout.newLayout = "%b %d, %Y";
      this.updatePlot();
      this.cdr.detectChanges();
    }
    else if (update === 3) {
      this._layout.xaxis.tickformat = "%d/%m/%Y";
      this._layout.newLayout = "%d/%m/%Y";
      this.updatePlot();
      this.cdr.detectChanges();
    }
    else if (update === 4) {
      if (this.fourClicked) {
        this._data[0].type = "candlestick";
        this.fourClicked = false;
      }
      else {
        this._data[0].type = "ohlc";
        this.fourClicked = true;
      }
      this.updatePlot();
      this.cdr.detectChanges();
    }
    else if (update === 10) {
      // all dates: from the first to the last entry, independent of the spacing between entries
      const firstDate = this.dateToString(this.stringToDate(this._data[0].x[0]));
      const lastDate = this.dateToString(this.stringToDate(this._data[0].x[this._data[0].x.length - 1]));
      this.applyLayout([firstDate, lastDate], this._layout.xaxis.tickformat);
    }
    else if (update >= 5 && update <= 9) {
      let mapping: { [key: number]: number } = {
        5: 14,      // 2 Weeks
        6: 31,      // 1 Month
        7: 91,      // 3 Months
        8: 183,     // 6 Months
        9: 365      // 1 Year
      };
      let lastX = this._data[0].x[this._data[0].x.length - 1];
      let newDate: Date = this.stringToDate(lastX);
      let newLastDate: Date = this.stringToDate(lastX);
      newDate.setDate(newDate.getDate() - mapping[update]);
      let firstDate = this.dateToString(newDate);
      let lastDate = this.dateToString(newLastDate);

      if (this._layout.xaxis.range[0] != firstDate) {
        this.applyLayout([firstDate, lastDate], this._layout.xaxis.tickformat);
      }
    }
  }

  protected stringToDate(date: string): Date {
    return new Date(date);
  }

  protected dateToString(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  @Input()
  public set type(type: string) {
    if (type != null && (type === "ohlc" || type === "candlestick")) {
      this._data[0].type = type;
    } else {
      this._data[0].type = 'candlestick';
    }
    this.updatePlot();
  }

  @Input()
  public set buttons(buttons: boolean | string) {
    if (buttons === true) {
      this.builtinButtons = true; this.chartType = false; this.period = false;
    } else if (buttons === false) {
      this.builtinButtons = false; this.chartType = false; this.period = false;
    } else if (buttons === 'period') {
      this.builtinButtons = false; this.period = true; this.chartType = false; this.dateFormat = false;
    } else if (buttons === 'dateformat') {
      this.builtinButtons = false; this.dateFormat = true; this.chartType = false; this.period = false;
    } else if (buttons === 'type') {
      this.builtinButtons = false; this.chartType = true; this.period = false;
    } else if (buttons === 'period+dateformat' || buttons === 'dateformat+period') {
      this.builtinButtons = false; this.dateFormat = true; this.period = true; this.chartType = false;
    } else if (buttons === 'period+type' || buttons === 'type+period') {
      this.builtinButtons = false; this.dateFormat = false; this.period = true; this.chartType = true;
    } else if (typeof buttons === 'string' && buttons.includes('period') && buttons.includes('type') && buttons.includes('dateformat')) {
      this.builtinButtons = false; this.dateFormat = true; this.period = true; this.chartType = true;
    }

    this.applyLayout(this._layout.xaxis.range, this._layout.xaxis.tickformat);
  }

  @Input()
  public set colors(colors: string[]) {
    if (colors != null && colors.length === 2) {
      this._data[0].decreasing.line.color = colors[0];
      this._data[0].increasing.line.color = colors[1];
    } else {
      this._data[0].decreasing.line.color = this.COLOR_DECREASING;
      this._data[0].increasing.line.color = this.COLOR_INCREASING;
    }
    this.updatePlot();
  }

  @Input()
  public set data(data: any) {
    if (!data) return;

    let entries = typeof data.getEntriesList === 'function' ? (data.getEntriesList() ? data.getEntriesList().__internalArray() : undefined) : data.entries;

    if (Array.isArray(data)) entries = data;

    if (!entries || !Array.isArray(entries) || entries.length === 0) {
      console.warn("Candlestick Chart received empty or invalid data:", data);
      return;
    }

    this._data[0].x = [];
    this._data[0].open = [];
    this._data[0].high = [];
    this._data[0].low = [];
    this._data[0].close = [];

    for (let i = 0; i < entries.length; i++) {
      let entry = entries[i];

      let label = typeof entry.getLabel === 'function' ? entry.getLabel() : entry.label;
      this._data[0].x.push(label);

      let values = typeof entry.getValueList === 'function' ? (entry.getValueList() ? entry.getValueList().__internalArray() : undefined) : (entry.value || entry.values);

      if (values && values.length >= 4) {
        this._data[0].open.push(values[0]);
        this._data[0].high.push(values[1]);
        this._data[0].low.push(values[2]);
        this._data[0].close.push(values[3]);
      } else if (typeof entry.getValue === 'function') {
        this._data[0].open.push(entry.getValue(0));
        this._data[0].high.push(entry.getValue(1));
        this._data[0].low.push(entry.getValue(2));
        this._data[0].close.push(entry.getValue(3));
      }
    }

    this.updatePlot();
  }

  @Input()
  public set margins(margins: number[] | number) {
    if (margins != null) {
      this.autoMargins = false;
      if (typeof margins === "number") {
        this._layout.margin = { t: margins, b: margins, l: margins, r: margins };
      } else {
        if (margins.length === 1) {
          this._layout.margin = { t: margins[0], b: margins[0], l: margins[0], r: margins[0] };
        } else if (margins.length === 2) {
          this._layout.margin = { t: margins[0], b: margins[0], l: margins[1], r: margins[1] };
        } else if (margins.length === 3) {
          this._layout.margin = { t: margins[0], b: margins[2], l: margins[1], r: margins[1] };
        } else if (margins.length >= 4) {
          this._layout.margin = { t: margins[0], r: margins[1], b: margins[2], l: margins[3] };
        }
      }
    } else {
      this.autoMargins = true;
      this._layout.margin = { t: this.topMargin, b: this.bottomMargin, l: this.leftMargin, r: this.rightMargin };
    }
    this.updatePlot();
  }

  public ngOnInit(): void {
    super.ngOnInit();
  }
}
