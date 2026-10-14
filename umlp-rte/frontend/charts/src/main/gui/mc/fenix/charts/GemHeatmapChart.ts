/* (c) https://github.com/MontiCore/monticore */

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostBinding,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemHeatmapChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemHeatmapChartComponent';
import { GemChartOptions } from './GemChartOptions';
import { PlotlyModule } from 'angular-plotly.js';
import * as Plotly from 'plotly.js-dist-min';
import { GemHeatmapChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemheatmapcharttypes/GemHeatmapChartData';
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
export class GemHeatmapChart extends GemHeatmapChartComponent implements OnInit, AfterViewInit, OnDestroy {

  @HostBinding('style.height.px')
  get hostHeight() {
    return this.height || 500;
  }

  @HostBinding('style.max-height')
  get hostMaxHeight() {
    return '100%';
  }

  @HostBinding('style.width')
  get hostWidth() {
    return '100%';
  }

  @HostBinding('style.display')
  get hostDisplay() {
    return 'block';
  }

  @Input() height?: number;

  public _data!: GemHeatmapChartData;
  public _xBins: number = 6;
  public _yBins: number = 6;

  public _xLabel: string = '';
  public _yLabel: string = '';

  private resizeObserver!: ResizeObserver;

  @ViewChild('innerContainer') innerContainer!: ElementRef;
  @ViewChild('outerContainer') outerContainer!: ElementRef;

  @Input() defaultColor?: string = '#303C46';
  @Input() defaultXBins?: number = 6;
  @Input() defaultYBins?: number = 6;

  private min_num_seperations = 1;
  private max_num_seperations = 32;

  public plotlyData: any[] = [];

  colorScheme = ['#A3CAEC', '#98BCDB', '#8CAECB', '#819FBA', '#7591AA', '#6A8399', '#5E7588', '#536778', '#3C4A57', '#303C46'];

  _layout: any = {
    autosize: true,
    margin: { t: 30, b: 60, l: 50, r: 20 },
    showlegend: false,
    xaxis: { automargin: true },
    yaxis: { automargin: true }
  };

  _config: any = { displaylogo: false, responsive: true };

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  @Input()
  public set data(data: GemHeatmapChartData | any) {
    if (!data) return;
    this._data = data;
    const entries = typeof data.getEntriesList === 'function' ? (data.getEntriesList() ? data.getEntriesList().__internalArray() : undefined) : data.entries;
    if (!entries || !Array.isArray(entries)) return;
    const { xLabels, yLabels, z } = this.computeHeatmap(entries);

    const domain = this.colorScheme;
    const colorscale = domain.map((c, i) => [i / (domain.length - 1), c]);

    const trace = {
      z: z,
      x: xLabels,
      y: yLabels,
      type: 'heatmap',
      colorscale: colorscale,
      hoverongaps: false,
      colorbar: { titleside: 'right' },
      xgap: 2,
      ygap: 2
    };

    this.plotlyData = [trace];
    this.updateAxisTitles();
    this.cdr.detectChanges();
  }

  private computeHeatmap(entries: any[]) {
    if (!entries || entries.length === 0) return { xLabels: [], yLabels: [], z: [] };

    let minX = entries[0].getXValue ? entries[0].getXValue() : entries[0].xValue;
    let maxX = minX;
    let minY = entries[0].getYValue ? entries[0].getYValue() : entries[0].yValue;
    let maxY = minY;

    for (const e of entries) {
      const xv = e.getXValue ? e.getXValue() : e.xValue;
      const yv = e.getYValue ? e.getYValue() : e.yValue;
      if (xv < minX) minX = xv;
      if (xv > maxX) maxX = xv;
      if (yv < minY) minY = yv;
      if (yv > maxY) maxY = yv;
    }

    if (minX === maxX) maxX += 1;
    if (minY === maxY) maxY += 1;

    const xBins = this._xBins || this.defaultXBins || 6;
    const yBins = this._yBins || this.defaultYBins || 6;

    const stepX = (maxX - minX) / xBins;
    const stepY = (maxY - minY) / yBins;

    const grid = new Array(xBins);
    for (let i = 0; i < xBins; i++) grid[i] = new Array(yBins).fill(0);

    for (const e of entries) {
      const xv = e.getXValue ? e.getXValue() : e.xValue;
      const yv = e.getYValue ? e.getYValue() : e.yValue;
      let xIdx = Math.floor((xv - minX) / stepX);
      let yIdx = Math.floor((yv - minY) / stepY);
      if (xIdx >= xBins) xIdx = xBins - 1;
      if (yIdx >= yBins) yIdx = yBins - 1;
      if (xIdx < 0) xIdx = 0;
      if (yIdx < 0) yIdx = 0;
      grid[xIdx][yIdx]++;
    }

    const z: number[][] = [];
    for (let j = 0; j < yBins; j++) {
      const row = [];
      for (let i = 0; i < xBins; i++) {
        row.push(grid[i][j]);
      }
      z.push(row);
    }

    const xLabels = [];
    const yLabels = [];
    for (let i = 0; i < xBins; i++) {
      xLabels.push(Math.floor(minX + i * stepX) + '-' + Math.floor(minX + (i + 1) * stepX));
    }
    for (let j = 0; j < yBins; j++) {
      yLabels.push(Math.floor(minY + j * stepY) + '-' + Math.floor(minY + (j + 1) * stepY));
    }

    return { xLabels, yLabels, z };
  }

  public enter_color(value: string) {
    if (!value || !/^#[0-9A-F]{6}$/i.test(value)) return;

    const steps = 6;
    const color = parseInt(value.substr(1), 16);
    const blue = (color % 256);
    const green = ((color - blue) / 256) % 256;
    const red = (color - blue - (256 * green)) / 65536;
    const domain = [];

    // from a light tint for the lowest count to the selected color for the highest, like the default scheme;
    // the lightest step keeps a share of the color, pure white cells would vanish on the white background
    const minFactor = 0.35;
    for (let i = 0; i < steps; i++) {
      const factor = minFactor + (1 - minFactor) * i / (steps - 1);
      const r = Math.round(255 * (1 - factor) + red * factor);
      const g = Math.round(255 * (1 - factor) + green * factor);
      const b = Math.round(255 * (1 - factor) + blue * factor);
      domain.push('#' + ('0' + r.toString(16)).slice(-2) + ('0' + g.toString(16)).slice(-2) + ('0' + b.toString(16)).slice(-2));
    }
    this.colorScheme = domain;

    if (this.plotlyData && this.plotlyData.length > 0) {
      const colorscale = this.colorScheme.map((c, i) => [i / (this.colorScheme.length - 1), c]);
      this.plotlyData[0].colorscale = colorscale;
      this.updatePlot();
      this.cdr.detectChanges();
    }
  }

  @Input()
  public set xBins(value: number) {
    this.enter_x_bins(value);
  }

  @Input()
  public set yBins(value: number) {
    this.enter_y_bins(value);
  }

  public enter_x_bins(value: string | number) {
    const bins = this.toBinCount(value);
    if (bins === null) return;
    this.setXBins(bins);
    if (this._data) this.data = this._data;
  }

  public enter_y_bins(value: string | number) {
    const bins = this.toBinCount(value);
    if (bins === null) return;
    this.setYBins(bins);
    if (this._data) this.data = this._data;
  }

  /** returns a whole number of bins within the allowed range, or null for empty or invalid input */
  private toBinCount(value: string | number): number | null {
    if (value === null || value === undefined || value === '') return null;
    const bins = Math.round(+value);
    if (!Number.isFinite(bins)) return null;
    return Math.max(this.min_num_seperations, Math.min(this.max_num_seperations, bins));
  }

  public setXLabel(value: string) {
    super.setXLabel(value);
    this.updateAxisTitles();
  }

  public setYLabel(value: string) {
    super.setYLabel(value);
    this.updateAxisTitles();
  }

  public ngOnInit(): void {
    super.ngOnInit();
  }

  ngAfterViewInit() {
    this.resizeObserver = setupResizeObserver(this.el.nativeElement, (w, h) => this.handleResize(w, h));
  }

  ngOnDestroy() {
    if (this.resizeObserver) this.resizeObserver.disconnect();
  }

  private handleResize(width: number, height: number) {
    if (width === 0 || height === 0) return;

    const fontSize = 12;

    this._layout.margin = {
      t: 30,
      b: 50,
      l: 50,
      r: 30
    };

    if (this.autoMarginsEnabled()) {
      if (!this._layout.xaxis) this._layout.xaxis = {};
      if (!this._layout.yaxis) this._layout.yaxis = {};

      this._layout.xaxis.automargin = true;
      this._layout.yaxis.automargin = true;

      this._layout.xaxis.tickfont = { size: fontSize };
      this._layout.yaxis.tickfont = { size: fontSize };
    }

    this.updatePlot();
    this.cdr.detectChanges();
  }

  private updateAxisTitles(): void {
    if (!this._layout.xaxis) this._layout.xaxis = {};
    if (!this._layout.yaxis) this._layout.yaxis = {};
    this._layout.xaxis.title = this.xLabel ? { text: this.xLabel, standoff: 12 } : undefined;
    this._layout.yaxis.title = this.yLabel ? { text: this.yLabel, standoff: 12 } : undefined;
    this.updatePlot();
  }

  private autoMarginsEnabled() {
    return true;
  }

  private updatePlot(): void {
    this.plotlyData = [...this.plotlyData];
    this._layout = { ...this._layout, margin: { ...this._layout.margin }, xaxis: { ...this._layout.xaxis }, yaxis: { ...this._layout.yaxis } };
  }


  public changeContainer() {
    return '';
  }
}
