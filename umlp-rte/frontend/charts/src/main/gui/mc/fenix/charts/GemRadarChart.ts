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
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemRadarChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemRadarChartComponent';
import { GemRadarChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemradarcharttypes/GemRadarChartData';
import { GemListUtil } from '@umlp/commonj2ts';
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
export class GemRadarChart extends GemRadarChartComponent implements OnInit, AfterViewInit, OnDestroy {

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

  /** Labels displayed on the angular axis. */
  public labels: string[] = [];

  /** Plotly trace data. */
  public plotlyData: any[] = [];

  /** If true, the legend is displayed. */
  public displayLegend = true;

  /** Current legend position. */
  public legendPosition: string = 'bottom';

  private _rawData?: GemRadarChartData;
  private resizeObserver!: ResizeObserver;

  private _borderWidthValue: number = 3;
  private _fillValue: boolean = false;
  private _minValue?: number;
  private _maxValue?: number;
  private _stepSizeValue?: number;
  private _maxLabelLengthValue: number = 20;
  private _showLabelsValue: boolean = true;
  private _opacityValue: number = 0.4;

  private _lastWidth: number = 0;
  // measured after rendering, see onAfterPlot()
  private labelExtents: { t: number, b: number, l: number, r: number } = null;
  private legendSize: { w: number, h: number, horizontal: boolean } = null;
  private _adjustCount: number = 0;
  private _lastHeight: number = 0;

  public _layout: any = {
    autosize: true,
    margin: { t: 30, b: 30, l: 30, r: 30 },
    hovermode: 'closest',
    showlegend: true,
    legend: { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'left', x: 1.02 },
    polar: {
      radialaxis: {
        visible: true,
        autorange: true,
        automargin: true,
        showticklabels: true,
        tickfont: { size: 12 }
      },
      angularaxis: {
        visible: true,
        showticklabels: true,
        tickfont: { size: 12 },
        rotation: 90,
        direction: 'clockwise'
      }
    }
  };

  public _config: any = {
    displaylogo: false,
    responsive: true
  };

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  /** Gives the colors from the constant autoColors. */
  public getColors() {
    return autoColors;
  }

  @Input()
  public set data(data: GemRadarChartData) {
    if (!data) {
      return;
    }
    this._rawData = data;
    this.processData();
  }

  private processData() {
    if (!this._rawData) {
      return;
    }

    (async () => {
      const data = this._rawData!;
      const dataSets = typeof data.getDataSetsList === 'function'
        ? await GemListUtil.toArraySync(data.getDataSetsList())
        : (data as any).dataSets;
      const rawLabels = typeof data.getLabelsList === 'function'
        ? await GemListUtil.toArraySync(data.getLabelsList())
        : (data as any).labels;
      const dataMaxLabelLength = typeof data.getMaxLabelLength === 'function'
        ? data.getMaxLabelLength()
        : (data as any).maxLabelLength;

      if (typeof dataMaxLabelLength === 'number') {
        this._maxLabelLengthValue = dataMaxLabelLength;
      }

      this.labels = Array.isArray(rawLabels) ? this.formatLabels(rawLabels) : [];
      const autocolors = this.getColors();
      let pointerAutoColors = 0;
      const traces: any[] = [];

      if (Array.isArray(dataSets)) {
        for (const ds of dataSets) {
          const rawDs: any = ds;
          const label = typeof rawDs.getLabel === 'function' ? rawDs.getLabel() : rawDs.label;
          const dataList = typeof rawDs.getDataList === 'function' ? rawDs.getDataList() : undefined;
          const values = dataList ? await GemListUtil.toArraySync(dataList) : rawDs.data;
          const yValues: number[] = Array.isArray(values) ? values : [];

          let backgroundColor = typeof rawDs.getBackgroundColor === 'function' ? rawDs.getBackgroundColor() : rawDs.backgroundColor;
          let borderColor = typeof rawDs.getBorderColor === 'function' ? rawDs.getBorderColor() : rawDs.borderColor;
          const dsFill = typeof rawDs.getFill === 'function' ? rawDs.getFill() : rawDs.fill;
          const dsBorderWidth = typeof rawDs.getBorderWidth === 'function' ? rawDs.getBorderWidth() : rawDs.borderWidth;

          if (!backgroundColor || !CSS.supports('color', backgroundColor)) {
            backgroundColor = autocolors[pointerAutoColors % autocolors.length];
            pointerAutoColors++;
            if (typeof rawDs.setBackgroundColor === 'function') {
              rawDs.setBackgroundColor(backgroundColor);
            } else {
              rawDs.backgroundColor = backgroundColor;
            }
          }

          if (!borderColor || !CSS.supports('color', borderColor)) {
            borderColor = backgroundColor;
            if (typeof rawDs.setBorderColor === 'function') {
              rawDs.setBorderColor(borderColor);
            } else {
              rawDs.borderColor = borderColor;
            }
          }

          const useFill = (dsFill !== null && dsFill !== undefined) ? dsFill : this._fillValue;
          const fillMode = useFill ? 'toself' : 'none';
          const fillColor = useFill ? this.applyOpacity(backgroundColor, this._opacityValue) : undefined;
          const lineWidth = (dsBorderWidth !== null && dsBorderWidth !== undefined) ? dsBorderWidth : this._borderWidthValue;

          traces.push({
            type: 'scatterpolar',
            r: [...yValues, yValues[0]],
            theta: [...this.labels, this.labels[0]],
            mode: 'lines+markers',
            name: label,
            fill: fillMode,
            fillcolor: fillColor,
            hoveron: 'points',
            line: { color: borderColor, width: lineWidth },
            marker: { color: borderColor, size: 6 },
            hovertemplate: `<b>${this.escapeText(String(label))}</b><br>%{theta}: %{r}<extra></extra>`
          });
        }
      }

      traces.sort((a, b) => {
        const sumA = (a.r as number[]).slice(0, -1).reduce((acc, v) => acc + v, 0);
        const sumB = (b.r as number[]).slice(0, -1).reduce((acc, v) => acc + v, 0);
        return sumB - sumA;
      });

      this.plotlyData = traces;
      this.updateAxisConfig();
    })();
  }

  @Input()
  public set borderWidth(borderWidth: number) {
    this._borderWidthValue = borderWidth;
    this.processData();
  }

  @Input()
  public set fill(fill: boolean) {
    this._fillValue = fill;
    this.processData();
  }

  @Input()
  public set min(min: number) {
    this._minValue = min;
    this.updateAxisConfig();
  }

  @Input()
  public set max(max: number) {
    this._maxValue = max;
    this.updateAxisConfig();
  }

  @Input()
  public set stepSize(stepSize: number) {
    this._stepSizeValue = stepSize;
    this.updateAxisConfig();
  }

  @Input()
  public set maxLabelLength(maxLabelLength: number) {
    this._maxLabelLengthValue = maxLabelLength;
    if (this._rawData) {
      this.processData();
    }
  }

  @Input()
  public set showLegend(show: boolean) {
    this.displayLegend = show;
    this._layout.showlegend = this.displayLegend;
    this.applyMargins();
    this.updatePlot();
    this.cdr.detectChanges();
  }

  @Input()
  public set showLabels(show: boolean) {
    this._showLabelsValue = show;
    this.updateAxisConfig();
  }

  @Input()
  public set opacity(opacity: number) {
    this._opacityValue = opacity;
    this.processData();
  }

  public ngOnInit(): void {
    super.ngOnInit();
    this.applyLegendPosition(this.legendPosition);
    this.updateAxisConfig();
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
    if (width === 0 || height === 0) {
      return;
    }

    this._lastWidth = width;
    this._lastHeight = height;
    this._adjustCount = 0;

    const fontSize = 12;

    this._layout.polar = {
      ...this._layout.polar,
      radialaxis: { ...this._layout.polar.radialaxis, tickfont: { size: fontSize } },
      angularaxis: { ...this._layout.polar.angularaxis, tickfont: { size: fontSize } }
    };

    if (this._layout.legend) {
      this._layout.legend = { ...this._layout.legend, font: { ...(this._layout.legend.font || {}), size: fontSize } };
    }

    this.applyMargins();
    this.updatePlot();
    this.cdr.detectChanges();
  }

  private updateAxisConfig() {
    if (!this._layout.polar) {
      this._layout.polar = {};
    }

    this._layout.polar = { ...this._layout.polar };
    this._layout.polar.radialaxis = {
      ...this._layout.polar.radialaxis,
      visible: true,
      showticklabels: this._showLabelsValue
    };
    this._layout.polar.angularaxis = {
      ...this._layout.polar.angularaxis,
      visible: true,
      showticklabels: this._showLabelsValue
    };

    if (this._minValue !== undefined || this._maxValue !== undefined) {
      this._layout.polar.radialaxis.autorange = false;
      this._layout.polar.radialaxis.range = [
        this._minValue ?? 0,
        this._maxValue ?? ((this._minValue !== undefined ? this._minValue : 0) + 1)
      ];
    } else {
      this._layout.polar.radialaxis.autorange = true;
      this._layout.polar.radialaxis.range = undefined;
    }

    if (this._stepSizeValue !== undefined) {
      this._layout.polar.radialaxis.dtick = this._stepSizeValue;
    } else {
      this._layout.polar.radialaxis.dtick = undefined;
    }

    this._adjustCount = 0;
    this.applyMargins();
    this.updatePlot();
    this.cdr.detectChanges();
  }

  private applyLegendPosition(legendPosition: string) {
    this._layout.showlegend = this.displayLegend;
    this._adjustCount = 0;
    // the legend placement depends on the space the axis labels need, so both are computed together
    this.applyMargins();
  }

  private updatePlot(): void {
    this.plotlyData = [...this.plotlyData];
    this._layout = {
      ...this._layout,
      margin: { ...this._layout.margin },
      legend: this._layout.legend ? { ...this._layout.legend } : this._layout.legend,
      polar: this._layout.polar
        ? {
          ...this._layout.polar,
          radialaxis: { ...this._layout.polar.radialaxis },
          angularaxis: this._layout.polar.angularaxis ? { ...this._layout.polar.angularaxis } : undefined
        }
        : undefined
    };
  }

  private formatLabels(labels: string[]): string[] {
    return labels.map(label => this.wrapLabel(label, this._maxLabelLengthValue));
  }

  private wrapLabel(label: string, maxLength: number): string {
    if (!label || maxLength <= 0) {
      return label;
    }
    const words = label.split(' ');
    const lines: string[] = [];
    let chunk = words[0] || '';
    for (let i = 1; i < words.length; i++) {
      if ((chunk + ' ' + words[i]).length <= maxLength) {
        chunk += ' ' + words[i];
      } else {
        lines.push(chunk);
        chunk = words[i];
      }
    }
    lines.push(chunk);
    return lines.join('<br>');
  }

  private applyOpacity(color: string, opacity: number): string {
    if (typeof color !== 'string') {
      return color;
    }
    if (color.startsWith('#')) {
      const r = parseInt(color.slice(1, 3), 16);
      const g = parseInt(color.slice(3, 5), 16);
      const b = parseInt(color.slice(5, 7), 16);
      return `rgba(${r}, ${g}, ${b}, ${opacity})`;
    } else if (color.startsWith('rgb(')) {
      return color.replace('rgb(', 'rgba(').replace(')', `, ${opacity})`);
    } else if (color.startsWith('rgba(')) {
      return color.replace(/[^,]+(?=\))/, `${opacity}`);
    }
    return color;
  }

  /**
   * Reserves space around the polar area for the angular axis labels and, on the legend's side,
   * additionally for the legend, which is placed beyond the labels so that it cannot cover them.
   * Margins are set explicitly (no autoexpand), otherwise the legend would push the margins again.
   */
  private applyMargins(): void {
    // measured after rendering; until then (and for hidden labels) fall back to the estimate
    const estimate = Math.max(this.getAxisLabelMargin(), 20);
    const ext = this.labelExtents || { t: estimate, b: estimate, l: estimate, r: estimate };
    const pad = 6;
    const margin: any = {
      t: Math.max(ext.t + pad, 20), b: Math.max(ext.b + pad, 20),
      l: Math.max(ext.l + pad, 20), r: Math.max(ext.r + pad, 20),
      autoexpand: false
    };
    const gap = 8;

    if (!this.displayLegend) {
      this._layout.margin = margin;
      return;
    }

    // current size of the host; the last ResizeObserver value can be outdated when the page was not rendered in between
    const hostRect = this.el.nativeElement.getBoundingClientRect();
    const width = hostRect.width > 0 ? hostRect.width : (this._lastWidth > 0 ? this._lastWidth : 500);
    const height = hostRect.height > 0 ? hostRect.height : (this._lastHeight > 0 ? this._lastHeight : 400);
    const horizontal = this.legendPosition === 'top' || this.legendPosition === 'bottom';
    const measured = this.legendSize && this.legendSize.horizontal === horizontal ? this.legendSize : null;

    if (horizontal) {
      const legendHeight = measured ? measured.h : this.getHorizontalLegendHeight(width);
      const offset = this.legendPosition === 'top' ? margin.t : margin.b;
      if (this.legendPosition === 'top') {
        margin.t += legendHeight + gap;
      } else {
        margin.b += legendHeight + gap;
      }
      const plotHeight = Math.max(1, height - margin.t - margin.b);
      this._layout.legend = this.legendPosition === 'top'
        ? { orientation: 'h', xanchor: 'center', x: 0.5, yanchor: 'bottom', y: 1 + offset / plotHeight }
        : { orientation: 'h', xanchor: 'center', x: 0.5, yanchor: 'top', y: -offset / plotHeight };
    } else {
      const legendWidth = Math.min(measured ? measured.w : this.getVerticalLegendWidth(), width * 0.35);
      const offset = this.legendPosition === 'left' ? margin.l : margin.r;
      if (this.legendPosition === 'left') {
        margin.l += legendWidth + gap;
      } else {
        margin.r += legendWidth + gap;
      }
      const plotWidth = Math.max(1, width - margin.l - margin.r);
      this._layout.legend = this.legendPosition === 'left'
        ? { orientation: 'v', yanchor: 'middle', y: 0.5, xanchor: 'right', x: -offset / plotWidth }
        : { orientation: 'v', yanchor: 'middle', y: 0.5, xanchor: 'left', x: 1 + offset / plotWidth };
    }

    this._layout.legend.font = { size: 12 };
    this._layout.margin = margin;
  }

  /**
   * Measures how far the angular labels reach beyond the polar circle and how large the legend really is.
   * Both do not depend on the margins, so a single correction after the first render is enough.
   */
  public onAfterPlot(): void {
    const gd = this.el.nativeElement.querySelector('.js-plotly-plot');
    const circle = gd ? gd.querySelector('.polarsublayer.plotbg') : null;
    if (!circle) {
      return;
    }
    const c = circle.getBoundingClientRect();
    if (c.width === 0) {
      return;
    }

    const labels = Array.from(gd.querySelectorAll('.angularaxistick text')) as Element[];
    const extents = { t: 0, b: 0, l: 0, r: 0 };
    for (const label of labels) {
      const r = label.getBoundingClientRect();
      extents.t = Math.max(extents.t, c.top - r.top);
      extents.b = Math.max(extents.b, r.bottom - c.bottom);
      extents.l = Math.max(extents.l, c.left - r.left);
      extents.r = Math.max(extents.r, r.right - c.right);
    }
    const labelExtents = {
      t: Math.ceil(extents.t), b: Math.ceil(extents.b), l: Math.ceil(extents.l), r: Math.ceil(extents.r)
    };

    const legendEl = gd.querySelector('.legend .bg') || gd.querySelector('.legend');
    const horizontal = this.legendPosition === 'top' || this.legendPosition === 'bottom';
    let legendSize = this.legendSize;
    if (legendEl && this.displayLegend) {
      const l = legendEl.getBoundingClientRect();
      legendSize = { w: Math.ceil(l.width), h: Math.ceil(l.height), horizontal };
    }

    const changed = (a: number, b: number) => Math.abs(a - b) > 2;
    const old = this.labelExtents;
    const extentsChanged = !old || changed(old.t, labelExtents.t) || changed(old.b, labelExtents.b)
      || changed(old.l, labelExtents.l) || changed(old.r, labelExtents.r);
    const legendChanged = !!legendSize && (!this.legendSize || this.legendSize.horizontal !== legendSize.horizontal
      || changed(this.legendSize.w, legendSize.w) || changed(this.legendSize.h, legendSize.h));

    this.labelExtents = labelExtents;
    this.legendSize = legendSize;
    if ((extentsChanged || legendChanged) && this._adjustCount < 3) {
      this._adjustCount++;
      this.applyMargins();
      this.updatePlot();
      this.cdr.detectChanges();
    }
  }

  private getLegendEntryWidths(): number[] {
    // symbol and spacing of a Plotly legend entry plus the text
    return this.plotlyData.map(trace => 40 + this.measureTextWidth(this.stripHtmlTags(String(trace.name ?? '')), 12));
  }

  private getVerticalLegendWidth(): number {
    const widths = this.getLegendEntryWidths();
    return widths.length > 0 ? Math.ceil(Math.max(...widths)) + 10 : 0;
  }

  private getHorizontalLegendHeight(availableWidth: number): number {
    const widths = this.getLegendEntryWidths();
    if (widths.length === 0) {
      return 0;
    }
    let rows = 1;
    let rowWidth = 0;
    for (const w of widths) {
      if (rowWidth > 0 && rowWidth + w > availableWidth) {
        rows++;
        rowWidth = 0;
      }
      rowWidth += w;
    }
    return rows * 20 + 10;
  }

  private getAxisLabelMargin(): number {
    if (!this._showLabelsValue || !this.labels || this.labels.length === 0) {
      return 0;
    }

    const fontSize = this._layout.polar?.angularaxis?.tickfont?.size || 12;
    const lineHeight = fontSize * 1.2;
    const padding = 20;
    let maxLineWidth = 0;
    let maxLines = 0;

    for (const label of this.labels) {
      const lines = (label || '').split('<br>').map(line => this.stripHtmlTags(line));
      maxLines = Math.max(maxLines, lines.length);
      for (const line of lines) {
        if (!line) {
          continue;
        }
        const width = this.measureTextWidth(line, fontSize);
        if (width > maxLineWidth) {
          maxLineWidth = width;
        }
      }
    }

    const halfWidth = maxLineWidth / 2;
    const halfHeight = (maxLines * lineHeight) / 2;
    let required = Math.ceil(Math.max(halfWidth, halfHeight) + padding);

    const maxAllowed = this._lastWidth > 0 && this._lastHeight > 0
      ? Math.min(150, Math.max(80, Math.min(this._lastWidth, this._lastHeight) / 2 - 40))
      : 150;

    return Math.min(required, maxAllowed);
  }

  private static canvasContext: CanvasRenderingContext2D | null = null;

  private measureTextWidth(text: string, fontSize: number): number {
    if (typeof document === 'undefined' || !text) {
      return 0;
    }
    if (!GemRadarChart.canvasContext) {
      GemRadarChart.canvasContext = document.createElement('canvas').getContext('2d');
    }
    GemRadarChart.canvasContext!.font = `${fontSize}px "Open Sans", "Helvetica Neue", Arial, sans-serif`;
    return GemRadarChart.canvasContext!.measureText(text).width;
  }

  private stripHtmlTags(html: string): string {
    return html.replace(/<[^>]+>/g, '');
  }

  private escapeText(value: string): string {
    return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }
}
