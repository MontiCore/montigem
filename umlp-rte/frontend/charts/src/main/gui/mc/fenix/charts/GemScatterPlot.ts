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
import { config, GemScatterPlotComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemScatterPlotComponent';
import { GemScatterPlotAxis } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemscatterplottypes/GemScatterPlotAxis';
import { GemScatterPlotData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemscatterplottypes/GemScatterPlotData';
import { GemScatterPlotShape } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemscatterplottypes/GemScatterPlotShape';
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
export class GemScatterPlot extends GemScatterPlotComponent implements OnInit, AfterViewInit, OnDestroy {

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

  /** Plotly trace data. */
  public plotlyData: any[] = [];

  /** If true, the legend is displayed. */
  public displayLegend = true;

  /** Current legend position. */
  public legendPosition: string = 'bottom';

  private _rawData?: GemScatterPlotData;
  private _xAxisValue?: GemScatterPlotAxis;
  private _yAxisValue?: GemScatterPlotAxis;
  private resizeObserver!: ResizeObserver;

  public _layout: any = {
    autosize: true,
    margin: { t: 30, b: 30, l: 30, r: 30 },
    showlegend: true,
    legend: { orientation: 'h', yanchor: 'top', y: -0.25, xanchor: 'center', x: 0.5 },
    xaxis: { zeroline: false, automargin: true },
    yaxis: { zeroline: false, automargin: true }
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
  public set data(data: GemScatterPlotData) {
    if (!data) {
      return;
    }
    this._rawData = data;
    this.processData();
  }

  @Input()
  public set xAxis(axis: GemScatterPlotAxis) {
    this._xAxisValue = axis;
    this.updateAxisConfig();
  }

  @Input()
  public set yAxis(axis: GemScatterPlotAxis) {
    this._yAxisValue = axis;
    this.updateAxisConfig();
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

  private processData() {
    if (!this._rawData) {
      return;
    }

    (async () => {
      const data = this._rawData!;
      const sets = typeof data.getSetsList === 'function'
        ? await GemListUtil.toArraySync(data.getSetsList())
        : (data as any).sets;

      const autocolors = this.getColors();
      let pointerAutoColors = 0;
      const traces: any[] = [];

      if (Array.isArray(sets)) {
        for (const ds of sets) {
          const rawDs: any = ds;
          const label = typeof rawDs.getLabel === 'function' ? rawDs.getLabel() : rawDs.label;

          let color = typeof rawDs.getColor === 'function' ? rawDs.getColor() : rawDs.color;
          if (!color || !CSS.supports('color', color)) {
            color = autocolors[pointerAutoColors % autocolors.length];
            pointerAutoColors++;
            if (typeof rawDs.setColor === 'function') {
              rawDs.setColor(color);
            } else {
              rawDs.color = color;
            }
          }

          const shape = typeof rawDs.getShape === 'function' ? rawDs.getShape() : rawDs.shape;

          const points = typeof rawDs.getPointsList === 'function'
            ? await GemListUtil.toArraySync(rawDs.getPointsList())
            : rawDs.points;
          const xValues: number[] = [];
          const yValues: number[] = [];
          const pointLabels: string[] = [];
          const sizes: number[] = [];

          if (Array.isArray(points)) {
            for (const point of points) {
              const rawPoint: any = point;
              xValues.push(rawPoint.getX());
              yValues.push(rawPoint.getY());
              pointLabels.push(rawPoint.getLabel() || '');
              const radius = rawPoint.getPointRadius ? rawPoint.getPointRadius() : rawPoint.pointRadius;
              sizes.push(typeof radius === 'number' ? radius : 6);
            }
          }

          traces.push({
            type: 'scatter',
            mode: 'markers',
            name: label,
            x: xValues,
            y: yValues,
            customdata: pointLabels,
            hovertemplate: `coordinates: (%{x}, %{y}), label: %{customdata}<extra></extra>`,
            marker: {
              color: color,
              symbol: this.getPointSymbol(shape),
              size: sizes
            }
          });
        }
      }

      this.plotlyData = traces;
      this.updatePlot();
      this.cdr.detectChanges();
    })();
  }

  private updateAxisConfig() {
    const xAxis = this._xAxisValue;
    const yAxis = this._yAxisValue;

    const xTitle = xAxis?.getLabel();
    const yTitle = yAxis?.getLabel();

    this._layout.xaxis = {
      ...this._layout.xaxis,
      title: xTitle ? { text: xTitle, standoff: 12 } : undefined,
      showticklabels: true,
      autorange: true,
      automargin: true,
      zeroline: false
    };

    this._layout.yaxis = {
      ...this._layout.yaxis,
      title: yTitle ? { text: yTitle, standoff: 12 } : undefined,
      showticklabels: true,
      autorange: true,
      automargin: true,
      zeroline: false
    };

    if (xAxis && (xAxis.getMinValue() !== null || xAxis.getMaxValue() !== null)) {
      this._layout.xaxis.autorange = false;
      this._layout.xaxis.range = [
        xAxis.getMinValue() ?? 0,
        xAxis.getMaxValue() ?? ((xAxis.getMinValue() ?? 0) + 1)
      ];
    }

    if (yAxis && (yAxis.getMinValue() !== null || yAxis.getMaxValue() !== null)) {
      this._layout.yaxis.autorange = false;
      this._layout.yaxis.range = [
        yAxis.getMinValue() ?? 0,
        yAxis.getMaxValue() ?? ((yAxis.getMinValue() ?? 0) + 1)
      ];
    }

    if (xAxis && xAxis.getStepWidth() !== null && xAxis.getStepWidth() !== undefined) {
      this._layout.xaxis.dtick = xAxis.getStepWidth();
    }

    if (yAxis && yAxis.getStepWidth() !== null && yAxis.getStepWidth() !== undefined) {
      this._layout.yaxis.dtick = yAxis.getStepWidth();
    }

    this.updatePlot();
    this.cdr.detectChanges();
  }

  private handleResize(width: number, height: number) {
    if (width === 0 || height === 0) {
      return;
    }

    const fontSize = 12;

    this._layout.xaxis = { ...this._layout.xaxis, tickfont: { size: fontSize } };
    this._layout.yaxis = { ...this._layout.yaxis, tickfont: { size: fontSize } };

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
      this._layout.margin = { ...baseMargin, l: 80 };
    } else {
      this._layout.legend = { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'left', x: 1.02 };
      this._layout.margin = { ...baseMargin, r: 80 };
    }
  }

  private updatePlot(): void {
    this.plotlyData = [...this.plotlyData];
    this._layout = {
      ...this._layout,
      margin: { ...this._layout.margin },
      legend: this._layout.legend ? { ...this._layout.legend } : this._layout.legend,
      xaxis: this._layout.xaxis ? { ...this._layout.xaxis } : undefined,
      yaxis: this._layout.yaxis ? { ...this._layout.yaxis } : undefined
    };
  }

  private getPointSymbol(shape?: GemScatterPlotShape): string {
    switch (shape) {
      case GemScatterPlotShape.SQUARE:
        return 'square';
      case GemScatterPlotShape.ROUNDED_SQUARE:
        return 'square';
      case GemScatterPlotShape.ROTATED_SQUARE:
        return 'diamond';
      case GemScatterPlotShape.CROSS:
        return 'cross';
      case GemScatterPlotShape.ROTATED_CROSS:
        return 'x';
      case GemScatterPlotShape.DASH:
        return 'line-ns';
      case GemScatterPlotShape.LINE:
        return 'line-ns';
      case GemScatterPlotShape.STAR:
        return 'star';
      case GemScatterPlotShape.TRIANGLE:
        return 'triangle-up';
      default:
        return 'circle';
    }
  }
}
