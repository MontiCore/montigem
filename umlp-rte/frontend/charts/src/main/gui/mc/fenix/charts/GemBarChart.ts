/* (c) https://github.com/MontiCore/monticore */

import { Component, Input, OnInit, ChangeDetectionStrategy, ElementRef, AfterViewInit, OnDestroy, ChangeDetectorRef, ViewChild, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemBarChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemBarChartComponent';
import { GemBarChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gembarcharttypes/GemBarChartData';
import { autoColors } from './colors';
import { GemChartOptions } from './GemChartOptions';
import { PlotlyModule } from 'angular-plotly.js';
import * as Plotly from 'plotly.js-dist-min';
import { setupResizeObserver } from './ResizeObserverUtil';
import { fitsSideLegend, topLegendFallback } from './LegendLayout';
import { GemListUtil } from '@umlp/commonj2ts';

PlotlyModule.plotlyjs = Plotly;

@Component({
  standalone: true,
  imports: [CommonModule, GemChartOptions, PlotlyModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GemBarChart extends GemBarChartComponent implements OnInit, AfterViewInit, OnDestroy {

  @HostBinding('style.display') get hostDisplay() { return 'block'; }
  @HostBinding('style.width') get hostWidth() { return '100%'; }
  @HostBinding('style.height') get hostHeight() { return '100%'; }

  public labels: string[] = [];
  public _displayValue: (value: number) => string = (v) => String(v);
  public displayLegend = true;

  @ViewChild('innerContainer') innerContainer!: ElementRef;
  @ViewChild('outerContainer') outerContainer!: ElementRef;

  public legendPosition: string = 'right';
  private resizeObserver!: ResizeObserver;
  private lastWidth: number = 0;
  // true while a side legend is shown above the plot because the chart is too narrow
  private legendFallback: boolean = false;

  public autoMargins: boolean = true;
  public rightMargin: number = 20;
  public topMargin: number = 60;
  public bottomMargin: number = 25;
  public leftMargin: number = 50;

  public plotlyData: any[] = [];

  _layout: any = {
    barmode: 'group',
    margin: {
      r: this.rightMargin,
      t: this.topMargin,
      b: this.bottomMargin,
      l: this.leftMargin
    },
    showlegend: true,
    xaxis: {
      type: 'category',
      showticklabels: true,
      tickfont: { size: 11 },
      tickangle: 'auto',
      automargin: true
    },
    yaxis: { autorange: true, automargin: true }

  };

  _config: any = {
    displaylogo: false,
    displayModeBar: false,
    responsive: true
  };

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  public getColors() {
    return autoColors;
  }

  @Input()
  public set data(data: any) {
    if (!data) return;
    this._data = data;
    (async () => {
      let autocolors = this.getColors();
      let pointerAutoColors = 0;

      const labelsList = typeof data.getLabelsList === 'function' ? data.getLabelsList() : undefined;
      this.labels = labelsList ? await GemListUtil.toArraySync(labelsList) : (data.labels || []);

      let entriesList = typeof data.getEntriesList === 'function' ? data.getEntriesList() : undefined;
      let entries = entriesList ? await GemListUtil.toArraySync(entriesList) : data.entries;

      if (Array.isArray(data)) entries = data;

      if (!entries || !Array.isArray(entries)) {
        console.warn("GemBarChart: Received empty or structurally invalid data payload.", data);
        return;
      }

      let parsedTraces: any[] = [];

      for (let i = 0; i < entries.length; i++) {
        let entry = entries[i];
        let label = typeof entry.getLabel === 'function' ? entry.getLabel() : entry.label;

        let dataList = typeof entry.getDataList === 'function' ? entry.getDataList() : undefined;
        let values = dataList ? await GemListUtil.toArraySync(dataList) : (entry.data || entry.values || []);

        let color = typeof entry.getBackgroundColor === 'function' ? entry.getBackgroundColor() : entry.backgroundColor;

        if (!color || !CSS.supports('color', color)) {
          color = autocolors[pointerAutoColors++];
          if (typeof entry.setBackgroundColor === 'function') {
            entry.setBackgroundColor(color);
          } else {
            entry.backgroundColor = color;
          }
        }

        let trace = {
          x: this.labels,
          y: values,
          name: label,
          type: 'bar',
          marker: { color: color }
        };
        parsedTraces.push(trace);
      }

      this.plotlyData = parsedTraces;
      // a missing axis bound depends on the data
      this.updateAxisBounds();
      if (this.lastWidth > 0) {
        this.applyLegendFallback(this.lastWidth);
      }
      this.updatePlot();
      this.cdr.detectChanges();
    })();
  }

  private updateAxisBounds() {
    if (this._minValue == null && this._maxValue == null) {
      this._layout.yaxis.autorange = true;
      this._layout.yaxis.range = undefined;
    } else {
      // plotly 2.18 cannot autorange only one end of an axis (a null bound falls back to [-1, 1]),
      // so a missing bound is derived from the data
      const bounds = this.getValueBounds();
      const padding = Math.max((bounds.max - bounds.min) * 0.05, 1);
      this._layout.yaxis.autorange = false;
      this._layout.yaxis.range = [
        this._minValue != null ? this._minValue : Math.min(bounds.min - padding, 0),
        this._maxValue != null ? this._maxValue : Math.max(bounds.max + padding, 0)
      ];
    }
  }

  /** Lowest and highest bar value; stacked bars count with the sum of their positive or negative parts. */
  private getValueBounds(): { min: number; max: number } {
    const stacked = this._stacked && !this._overlapping;
    const positive: number[] = [];
    const negative: number[] = [];
    let min = 0;
    let max = 0;
    for (const trace of this.plotlyData) {
      (trace.y ?? []).forEach((value: number, i: number) => {
        if (typeof value !== 'number' || isNaN(value)) return;
        if (stacked) {
          if (value >= 0) positive[i] = (positive[i] ?? 0) + value;
          else negative[i] = (negative[i] ?? 0) + value;
        } else {
          min = Math.min(min, value);
          max = Math.max(max, value);
        }
      });
    }
    if (stacked) {
      min = Math.min(0, ...negative.filter(v => v != null));
      max = Math.max(0, ...positive.filter(v => v != null));
    }
    return { min, max };
  }

  @Input()
  public set minValue(minValue: number) {
    this._minValue = minValue;
    this.updateAxisBounds();
    this.updatePlot();
    this.cdr.detectChanges();
  }

  @Input()
  public set maxValue(maxValue: number) {
    this._maxValue = maxValue;
    this.updateAxisBounds();
    this.updatePlot();
    this.cdr.detectChanges();
  }

  @Input()
  public set displayValue(displayValue: (value: number) => string) {
    this._displayValue = displayValue;
  }

  @Input()
  public set stacked(stacked: boolean) {
    this._stacked = stacked;
    this.updateBarmode();
  }

  @Input()
  public set overlapping(overlapping: boolean) {
    this._overlapping = overlapping;
    this.updateBarmode();
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
    this.cdr.detectChanges();
  }

  private updateBarmode() {
    if (this._overlapping) {
      this._layout.barmode = 'overlay';
    } else if (this._stacked) {
      this._layout.barmode = 'stack';
    } else {
      this._layout.barmode = 'group';
    }
    // stacked bars need a larger range
    this.updateAxisBounds();
    this.updatePlot();
    this.cdr.detectChanges();
  }

  public ngOnInit(): void {
    super.ngOnInit();
  }

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
    this.lastWidth = width;
    this.applyLegendFallback(width);

    if (this._layout.xaxis.showticklabels === false) {
      this._layout.xaxis.showticklabels = true;
    }

    if (this.autoMargins && this.labels && this.labels.length > 0) {
      if (!this._layout.xaxis) this._layout.xaxis = {};
      this._layout.xaxis.tickmode = 'array';
      this._layout.xaxis.tickvals = this.labels;
      this._layout.xaxis.ticktext = this.labels;

      // Adjust font size and rotation for very small layout containers
      const isSmall = width < 450 || height < 250;
      const fontSize = 12;

      this._layout.xaxis.tickfont = { ...this._layout.xaxis.tickfont, size: fontSize };
      this._layout.xaxis.tickangle = isSmall ? -30 : 'auto';

      // the fallback legend above the plot brings its own, possibly reduced font size
      if (this._layout.legend && !this.legendFallback) {
        this._layout.legend.font = { ...this._layout.legend.font, size: fontSize };
      }

      // Reserve enough space for the right-side legend and rotated x-axis labels on small layouts
      this._layout.margin.r = Math.max(this.rightMargin, Math.min(width * 0.15, 80));
      this._layout.margin.b = Math.max(this.bottomMargin, Math.min(height * 0.25, 50));
    }

    this._layout.width = width;
    this._layout.height = height;

    this.updatePlot();
    this.cdr.detectChanges();
  }

  private updatePlot(): void {
    this.plotlyData = [...this.plotlyData];
    this._layout = {
      ...this._layout,
      margin: { ...this._layout.margin },
      xaxis: { ...this._layout.xaxis },
      yaxis: { ...this._layout.yaxis }
    };
  }

  public setLegendPosition(legendPosition: string) {
    this.legendPosition = legendPosition;
    this.legendFallback = false;
    this.applyLegendPosition(legendPosition);
    if (this.lastWidth > 0) {
      this.applyLegendFallback(this.lastWidth);
    }
    this.updatePlot();
    this.cdr.detectChanges();
  }

  /**
   * A legend left or right of the plot does not fit into narrow charts, it would stick out of the
   * component. Then the legend is placed above the plot until the chart is wide enough again.
   */
  private applyLegendFallback(width: number) {
    const names = this.plotlyData.map(trace => trace.name);
    const sideLegend = this.legendPosition === 'left' || this.legendPosition === 'right';
    if (this.displayLegend && sideLegend && names.length > 0 && !fitsSideLegend(names, width)) {
      const fallback = topLegendFallback(names, width, this._layout.margin.l, this._layout.margin.r);
      this._layout.legend = fallback.legend;
      this._layout.margin = { ...this._layout.margin, t: this.topMargin + fallback.height };
      this.legendFallback = true;
    } else if (this.legendFallback) {
      this.legendFallback = false;
      this.applyLegendPosition(this.legendPosition);
    }
  }

  private applyLegendPosition(legendPosition: string) {
    this._layout.showlegend = this.displayLegend;
    const baseMargin = { t: this.topMargin, b: this.bottomMargin, l: this.leftMargin, r: this.rightMargin };

    if (legendPosition === 'top') {
      this._layout.legend = { orientation: "h", yanchor: "bottom", y: 1.02, xanchor: "center", x: 0.5 };
      this._layout.margin = { ...baseMargin, t: this.topMargin + 40 };
    } else if (legendPosition === 'bottom') {
      this._layout.legend = { orientation: "h", yanchor: "top", y: -0.25, xanchor: "center", x: 0.5 };
      this._layout.margin = { ...baseMargin, b: this.bottomMargin + 45 };
    } else if (legendPosition === 'left') {
      this._layout.legend = { orientation: "v", yanchor: "auto", y: 0.5, xanchor: "right", x: -0.1 };
      this._layout.margin = { ...baseMargin, l: this.leftMargin + 45 };
    } else {
      this._layout.legend = { orientation: "v", yanchor: "auto", y: 0.5, xanchor: "left", x: 1.02 };
      this._layout.margin = { ...baseMargin, r: this.rightMargin + 40 };
    }
  }

  public changeContainer() {
    return '';
  }
}

