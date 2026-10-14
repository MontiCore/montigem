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
import { config, GemPieChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemPieChartComponent';
import { GemPieChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gempiecharttypes/GemPieChartData';
import { autoColors } from './colors';
import { GemChartOptions } from './GemChartOptions';
import {GemListUtil} from "@umlp/commonj2ts";
import { PlotlyModule } from 'angular-plotly.js';
import * as Plotly from 'plotly.js-dist-min';
import { setupResizeObserver } from './ResizeObserverUtil';

PlotlyModule.plotlyjs = Plotly;

@Component({
  imports: [...config.imports, CommonModule, GemChartOptions, PlotlyModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GemPieChart extends GemPieChartComponent implements OnInit, AfterViewInit, OnDestroy {

  @HostBinding('style.display')
  get hostDisplay() { return 'block'; }

  @HostBinding('style.width')
  get hostWidth() { return '100%'; }

  @HostBinding('style.height')
  get hostHeight() { return '100%'; }

  /** variable that enables or disables the legend */
  public displayLegend = true;

  /** defines the position of the legend */
  public legendPosition: string = 'bottom';

  /** variable with the size of the inner radius (0-100) */
  public _innerRadius: number = 0;

  /** Plotly trace data */
  public plotlyData: any[] = [];

  /** Plotly layout configuration */
  public _layout: any = {
    autosize: true,
    margin: { t: 30, b: 30, l: 30, r: 30 },
    showlegend: true,
    legend: { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'left', x: 1.02 }
  };

  /** Plotly display configuration */
  public _config: any = {
    displaylogo: false,
    responsive: true
  };

  private resizeObserver!: ResizeObserver;

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  /** gives the colors from the constant autoColors */
  public getColors() {
    return autoColors;
  }

  /** sets the data of the entries and sets the colors of the entries where the color is not explicitly given */
  @Input()
  public set data(data: GemPieChartData) {
    if (!data) {
      return;
    }
    (async () => {
      const entries = typeof data.getEntriesList === 'function' ? await GemListUtil.toArraySync(data.getEntriesList()) : (data as any).entries;
      if (!entries || !Array.isArray(entries)) {
        console.warn("GemPieChart: Received empty or structurally invalid data payload.", data);
        this.plotlyData = [];
        this.updatePlot();
        this.cdr.detectChanges();
        return;
      }

      const labels: string[] = [];
      const values: number[] = [];
      const colors: string[] = [];
      const autocolors = this.getColors();
      let pointerAutoColors = 0;

      for (const entry of entries) {
        const rawEntry: any = entry;
        const label = typeof rawEntry.getLabel === 'function' ? rawEntry.getLabel() : rawEntry.label;
        const value = typeof rawEntry.getValue === 'function' ? rawEntry.getValue() : rawEntry.value;
        let color = typeof rawEntry.getBackgroundColor === 'function' ? rawEntry.getBackgroundColor() : rawEntry.backgroundColor;

        if (!color || !CSS.supports('color', color)) {
          color = autocolors[pointerAutoColors % autocolors.length];
          pointerAutoColors++;
          if (typeof rawEntry.setBackgroundColor === 'function') {
            rawEntry.setBackgroundColor(color);
          } else {
            rawEntry.backgroundColor = color;
          }
        }

        labels.push(label);
        values.push(value);
        colors.push(color);
      }

      this.plotlyData = [{
        type: 'pie',
        labels: labels,
        values: values,
        marker: { colors: colors },
        hole: this._innerRadius / 100,
        hoverinfo: 'label+value+percent',
        textinfo: 'none'
      }];

      this.updatePlot();
      this.cdr.detectChanges();
    })();
  }

  @Input()
  public set innerRadius(innerRadius: number) {
    this._innerRadius = innerRadius !== undefined ? innerRadius : 0;
    if (this.plotlyData && this.plotlyData.length > 0) {
      this.plotlyData = [{
        ...this.plotlyData[0],
        hole: this._innerRadius / 100
      }];
      this.updatePlot();
      this.cdr.detectChanges();
    }
  }

  /** updates the inner radius from the slider input */
  public changeInnerRadius(innerRadius: number) {
    this.innerRadius = innerRadius;
  }

  /** sets the legend position and updates the Plotly layout */
  public setLegendPosition(legendPosition: string) {
    this.legendPosition = legendPosition;
    this.applyLegendPosition(legendPosition);
    this.updatePlot();
    this.cdr.detectChanges();
  }

  /** changes container style depending on legend position (kept for compatibility) */
  public changeContainer() {
    return '';
  }

  public ngOnInit(): void {
    super.ngOnInit();
    this.applyLegendPosition(this.legendPosition);
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
    if (width === 0 || height === 0) {
      return;
    }

    const fontSize = 12;

    if (this._layout.legend) {
      this._layout.legend = { ...this._layout.legend, font: { ...(this._layout.legend.font || {}), size: fontSize } };
    }

    this.updatePlot();
    this.cdr.detectChanges();
  }

  private applyLegendPosition(legendPosition: string) {
    this._layout.showlegend = this.displayLegend;
    const baseMargin = { t: 30, b: 30, l: 30, r: 30 };

    if (legendPosition === 'top') {
      this._layout.legend = { orientation: 'h', yanchor: 'bottom', y: 1.02, xanchor: 'center', x: 0.5 };
      this._layout.margin = { ...baseMargin, t: 50 };
    } else if (legendPosition === 'bottom') {
      this._layout.legend = { orientation: 'h', yanchor: 'top', y: -0.25, xanchor: 'center', x: 0.5 };
      this._layout.margin = { ...baseMargin, b: 50 };
    } else if (legendPosition === 'left') {
      this._layout.legend = { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'right', x: -0.1 };
      this._layout.margin = { ...baseMargin, l: 50 };
    } else {
      this._layout.legend = { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'left', x: 1.02 };
      this._layout.margin = { ...baseMargin, r: 50 };
    }

    this.updatePlot();
  }

  private updatePlot(): void {
    this.plotlyData = [...this.plotlyData];
    this._layout = {
      ...this._layout,
      margin: { ...this._layout.margin },
      legend: this._layout.legend ? { ...this._layout.legend } : this._layout.legend
    };
  }
}
