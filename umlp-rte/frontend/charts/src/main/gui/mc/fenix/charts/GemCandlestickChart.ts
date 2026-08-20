/* (c) https://github.com/MontiCore/monticore */

import { Component, ChangeDetectionStrategy, Input, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemCandlestickChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemCandlestickChartComponent';
import { GemChartOptions } from './GemChartOptions';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, CommonModule, GemChartOptions],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemCandlestickChart extends GemCandlestickChartComponent implements AfterViewInit {
  @ViewChild('chartContainer') chartContainer!: ElementRef;

  private Plotly: any;
  private plotInitialized = false;

  constructor() {
    super();
  }

  period: boolean = false;
  dateFormat: boolean = false;
  chartType: boolean = false;

  rightMargin: number = 0;
  topMargin: number = 20;
  bottomMargin: number = 0;
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


  ngAfterViewInit(): void {
    if (this.chartContainer) {
      setTimeout(() => {
        this.loadPlotlyAndCreatePlot();
      }, 100);
    }
  }

  // Lazy Load Plotly
  private async loadPlotlyAndCreatePlot() {
    try {
      const plotlyModule = await import('plotly.js-dist-min');
      this.Plotly = plotlyModule.default || plotlyModule;
      this.createPlot();
    } catch (error) {
      console.error("Failed to load Plotly lazily:", error);
    }
  }

  private createPlot(): void {
    if (this.chartContainer && this._data && this._layout && this.Plotly) {
      this.Plotly.newPlot(this.chartContainer.nativeElement, this._data, this._layout, { responsive: true });
      this.plotInitialized = true;
    }
  }

  private updatePlot(): void {
    if (this.plotInitialized && this.chartContainer && this._data && this._layout && this.Plotly) {
      this.Plotly.react(this.chartContainer.nativeElement, this._data, this._layout);
    }
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
        tickformat: tick,
        rangeselector: {
          buttons: this.showButtons()
        }
      }
    };
    this.updatePlot();
  }

  public updateLayout(update: number) {
    if (!this._data[0].x || this._data[0].x.length === 0) return; // Guard clause if no data is present

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
    }
    else if (update === 3) {
      this._layout.xaxis.tickformat = "%d/%m/%Y";
      this._layout.newLayout = "%d/%m/%Y";
      this.updatePlot();
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
    }
    else if (update >= 5 && update <= 10) {
      /** mapping is used in the frontend in updateLayout to determine how many days to go back based on the button clicked. The keys represent the button index and the values represent the number of days to go back from the last date in the data. */
      let mapping: { [key: number]: number } = {
        5: 14,      // 2 Weeks
        6: 31,      // 1 Month
        7: 91,      // 3 Months
        8: 183,     // 6 Months
        9: 365,     // 1 Year
        10: this._data[0].x.length // All time
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
    return date.toISOString().split('T')[0]; // returns 'YYYY-MM-DD'
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

    // Duck typing: Check if it's a method (instantiated class) or a raw JSON array
    let entries = typeof data.getEntriesList === 'function' ? data.getEntriesList().__internalArray() : data.entries;

    // Fallback if "data" is already the array
    if (Array.isArray(data)) entries = data;

    if (!entries || !Array.isArray(entries) || entries.length === 0) {
      console.warn("Candlestick Chart received empty or invalid data:", data);
      return;
    }

    // Flush previous values
    this._data[0].x = [];
    this._data[0].open = [];
    this._data[0].high = [];
    this._data[0].low = [];
    this._data[0].close = [];

    for (let i = 0; i < entries.length; i++) {
      let entry = entries[i];

      let label = typeof entry.getLabel === 'function' ? entry.getLabel() : entry.label;
      this._data[0].x.push(label);

      let values = typeof entry.getValueList === 'function' ? entry.getValueList().__internalArray() : (entry.value || entry.values);

      if (values && values.length >= 4) {
        this._data[0].open.push(values[0]);
        this._data[0].high.push(values[1]);
        this._data[0].low.push(values[2]);
        this._data[0].close.push(values[3]);
      } else if (typeof entry.getValue === 'function') {
        // Fallback for old MontiCore getters
        this._data[0].open.push(entry.getValue(0));
        this._data[0].high.push(entry.getValue(1));
        this._data[0].low.push(entry.getValue(2));
        this._data[0].close.push(entry.getValue(3));
      }
    }

    // Trigger Angular & Plotly Reactivity
    this._data = [...this._data];
    this.updatePlot();
  }

  @Input()
  public set margins(margins: number[] | number) {
    if (margins != null) {
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
      this._layout.margin = { t: this.topMargin, b: this.bottomMargin, l: this.leftMargin, r: this.rightMargin };
    }
    this.updatePlot();
  }

  public ngOnInit(): void { }
}
