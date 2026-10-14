/* (c) https://github.com/MontiCore/monticore */

import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, HostBinding, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemLineChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemLineChartComponent';
import { GemLineChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemlinecharttypes/GemLineChartData';
import { autoColors } from './colors';
import { GemChartOptions } from './GemChartOptions';
import { PlotlyModule } from 'angular-plotly.js';
import * as Plotly from 'plotly.js-dist-min';
import { setupResizeObserver } from './ResizeObserverUtil';
import { GemListUtil } from '@umlp/commonj2ts';

PlotlyModule.plotlyjs = Plotly;

@Component({
  standalone: true,
  imports: [...config.imports, CommonModule, GemChartOptions, PlotlyModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GemLineChart extends GemLineChartComponent implements OnInit, AfterViewInit, OnDestroy {

  @HostBinding('style.display')
  get hostDisplay() {
    return 'block';
  }

  @HostBinding('style.width')
  get hostWidth() {
    return '100%';
  }

  @HostBinding('style.height')
  get hostHeight() {
    return '100%';
  }

  public labels: string[] = [];
  public plotlyData: any[] = [];
  public displayLegend = true;
  public legendPosition: string = 'right';
  public _enableBackgroundColor = false;

  public _maxValue: number | undefined = undefined;
  public _minValue: number | undefined = undefined;

  public _displayValue: (value: number) => string = (v) => String(v);
  public _data!: GemLineChartData;

  private resizeObserver!: ResizeObserver;

  public _layout: any = {
    autosize: true,
    // Increased left margin (l) from 50 to 70 to fix E2E left-border overflow
    margin: { t: 30, b: 55, l: 70, r: 40 },
    showlegend: true,
    legend: { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'left', x: 1.02 },
    xaxis: {
      automargin: true,
      tickangle: 0,
      tickfont: { size: 12 }
    },
    yaxis: {
      autorange: true,
      automargin: true,
      tickfont: { size: 12 }
    }
  };

  public _config: any = {
    displaylogo: false,
    responsive: true
  };

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  public getColors() {
    return autoColors;
  }

  @Input()
  public set displayValue(displayValue: (value: number) => string) {
    this._displayValue = typeof displayValue === 'function' ? displayValue : (v) => String(v);
    if (this._data) this.data = this._data;
  }

  @Input()
  public set enableBackgroundColor(enableBackgroundColor: boolean) {
    this._enableBackgroundColor = enableBackgroundColor;
    if (this._data) this.data = this._data;
  }

  @Input()
  public set data(data: GemLineChartData) {
    if (!data) return;

    this._data = data;
    (async () => {
      const entriesList = typeof data.getEntriesList === 'function' ? data.getEntriesList() : undefined;
      const entries = entriesList ? await GemListUtil.toArraySync(entriesList) : (data as any).entries;
      const labelsList = typeof data.getLabelsList === 'function' ? data.getLabelsList() : undefined;
      const labels = labelsList ? await GemListUtil.toArraySync(labelsList) : (data as any).labels;

      if (!entries || !Array.isArray(entries) || entries.length === 0) {
        this.labels = Array.isArray(labels) ? labels : [];
        this.plotlyData = [];
        this.updatePlot();
        this.cdr.detectChanges();
        return;
      }

    this.labels = Array.isArray(labels) ? labels : [];
    const autocolors = this.getColors();
    let pointerAutoColors = 0;
    const traces: any[] = [];

    for (const entry of entries) {
      const rawEntry: any = entry;
      const label = typeof rawEntry.getLabel === 'function' ? rawEntry.getLabel() : rawEntry.label;
      const valuesList = typeof rawEntry.getDataList === 'function' ? rawEntry.getDataList() : undefined;
      const values = valuesList ? await GemListUtil.toArraySync(valuesList) : rawEntry._data;
      const yValues: number[] = Array.isArray(values) ? values : [];
      const borderColor = this.resolveColor(typeof rawEntry.getBorderColor === 'function' ? rawEntry.getBorderColor() : rawEntry.borderColor, autocolors[pointerAutoColors % autocolors.length]);

      if (!this.isValidColor(typeof rawEntry.getBorderColor === 'function' ? rawEntry.getBorderColor() : rawEntry.borderColor)) {
        pointerAutoColors++;
        if (typeof rawEntry.setBorderColor === 'function') {
          rawEntry.setBorderColor(borderColor);
        }
      }

      const backgroundColor = this.resolveColor(typeof rawEntry.getBackgroundColor === 'function' ? rawEntry.getBackgroundColor() : rawEntry.backgroundColor, borderColor);
      const pointColor = this.resolveColor(typeof rawEntry.getPointBackgroundColor === 'function' ? rawEntry.getPointBackgroundColor() : rawEntry.pointBackgroundColor, borderColor);
      const xValues = yValues.map((_: any, index: number) => this.labels[index] ?? String(index + 1));

      traces.push({
        name: label,
        type: 'scatter',
        mode: 'lines+markers',
        x: xValues,
        y: yValues,
        line: { color: borderColor, shape: 'linear' },
        marker: { color: pointColor, size: 6 },
        fill: this._enableBackgroundColor === true ? 'tozeroy' : 'none',
        fillcolor: this._enableBackgroundColor === true ? backgroundColor : undefined,
        text: yValues.map((value: number) => this.formatValue(value)),
        hovertemplate: `%{x}<br>${this.escapeText(String(label))}: %{text}<extra></extra>`,
        hoverinfo: 'text+name'
      });
    }

    this.plotlyData = traces;
    this.updateAxisBounds();
    this.updatePlot();
    this.cdr.detectChanges();
  })();
}

  @Input()
  public set maxValue(maxValue: number | null | undefined) {
    this._maxValue = (maxValue === null) ? undefined : (maxValue as number);
    this.updateAxisBounds();
  }

  @Input()
  public set minValue(minValue: number | null | undefined) {
    this._minValue = (minValue === null) ? undefined : (minValue as number);
    this.updateAxisBounds();
  }

  public ngOnInit(): void {
    super.ngOnInit();
    this.applyLegendPosition(this.legendPosition);
    this.updateAxisBounds();
  }

  ngAfterViewInit() {
    this.resizeObserver = setupResizeObserver(this.el.nativeElement, (w, h) => this.handleResize(w, h));
  }

  ngOnDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
  }

  public setLegendPosition(legendPosition: string) {
    this.legendPosition = legendPosition;
    this.applyLegendPosition(legendPosition);
    this.updatePlot();
    this.cdr.detectChanges();
  }

  public changeContainer() {
    return '';
  }

  private handleResize(width: number, height: number) {
    if (width === 0 || height === 0) return;

    const isSmall = width < 450 || height < 250;
    const fontSize = 12;

    this._layout.xaxis = { ...this._layout.xaxis, tickangle: isSmall ? -30 : 0, tickfont: { size: fontSize } };
    this._layout.yaxis = { ...this._layout.yaxis, tickfont: { size: fontSize } };

    if (this._layout.legend) {
      this._layout.legend = { ...this._layout.legend, font: { ...(this._layout.legend.font || {}), size: fontSize } };
    }

    this.updatePlot();
    this.cdr.detectChanges();
  }

  private updateAxisBounds(): void {
    if (!this._layout.yaxis) {
      this._layout.yaxis = {};
    }

    if (this._minValue !== undefined || this._maxValue !== undefined) {
      const bounds = this.getValueBounds();
      const padding = Math.max((bounds.max - bounds.min) * 0.05, 1);
      const finalMin = this._minValue !== undefined ? this._minValue : bounds.min - padding;
      const finalMax = this._maxValue !== undefined ? this._maxValue : bounds.max + padding;

      this._layout.yaxis.autorange = false;
      this._layout.yaxis.range = [finalMin, finalMax];
    } else {
      this._layout.yaxis.autorange = true;
      this._layout.yaxis.range = undefined;
    }
  }

  private getValueBounds(): { min: number; max: number } {
    let min = Infinity;
    let max = -Infinity;

    if (!this._data || typeof this._data.getEntriesList !== 'function') {
      return { min: 0, max: 1 };
    }

    const entriesList = this._data.getEntriesList();
    const entries = entriesList ? entriesList.__internalArray() : undefined;
    if (!entries) return { min: 0, max: 1 };
    for (const entry of entries) {
      const values = typeof entry.getDataList === 'function' ? (entry.getDataList() ? entry.getDataList().__internalArray() : []) : (entry as any)._data;
      if (!values || !Array.isArray(values)) continue;

      for (const value of values) {
        if (typeof value !== 'number' || Number.isNaN(value)) continue;
        if (value < min) min = value;
        if (value > max) max = value;
      }
    }

    if (min === Infinity) min = 0;
    if (max === -Infinity) max = 1;
    if (min === max) max = min + 1;

    return { min, max };
  }

  private applyLegendPosition(legendPosition: string) {
    this._layout.showlegend = this.displayLegend;
    const baseMargin = { t: 30, b: 55, l: 70, r: 40 }; // Keep consistent with default margin

    if (legendPosition === 'top') {
      this._layout.legend = { orientation: 'h', yanchor: 'bottom', y: 1.02, xanchor: 'center', x: 0.5 };
      this._layout.margin = { ...baseMargin, t: 70 };
    } else if (legendPosition === 'bottom') {
      this._layout.legend = { orientation: 'h', yanchor: 'top', y: -0.25, xanchor: 'center', x: 0.5 };
      this._layout.margin = { ...baseMargin, b: 80 };
    } else if (legendPosition === 'left') {
      this._layout.legend = { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'right', x: -0.1 };
      this._layout.margin = { ...baseMargin, l: 95 };
    } else {
      this._layout.legend = { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'left', x: 1.02 };
      this._layout.margin = { ...baseMargin, r: 80 };
    }

    this.updatePlot();
  }

  private updatePlot(): void {
    this.plotlyData = [...this.plotlyData];
    this._layout = {
      ...this._layout,
      margin: { ...this._layout.margin },
      legend: this._layout.legend ? { ...this._layout.legend } : this._layout.legend,
      xaxis: this._layout.xaxis ? { ...this._layout.xaxis } : this._layout.xaxis,
      yaxis: this._layout.yaxis ? { ...this._layout.yaxis } : this._layout.yaxis
    };
  }

  private resolveColor(color: string | undefined, fallback: string): string {
    if (this.isValidColor(color)) {
      return color as string;
    }
    return fallback;
  }

  private isValidColor(color: string | undefined): boolean {
    return typeof color === 'string' && color.length > 0 && CSS.supports('color', color);
  }

  private formatValue(value: number): string {
    if (this._displayValue && typeof this._displayValue === 'function') {
      return this._displayValue(value);
    }
    return String(value);
  }

  private escapeText(value: string): string {
    return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }
}
