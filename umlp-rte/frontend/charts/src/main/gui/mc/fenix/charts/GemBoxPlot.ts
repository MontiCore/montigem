/* (c) https://github.com/MontiCore/monticore */

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  HostBinding,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemBoxPlotComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemBoxPlotComponent';
import { GemBoxPlotData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemboxplottypes/GemBoxPlotData';
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
export class GemBoxPlot extends GemBoxPlotComponent implements OnInit, AfterViewInit, OnDestroy {

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

  public labels: string[] = [];
  public displayLegend = true;

  public legendPosition: string = 'right';
  private resizeObserver!: ResizeObserver;
  private lastWidth: number = 0;
  private lastHeight: number = 0;

  @ViewChild('innerContainer') innerContainer!: ElementRef;
  @ViewChild('outerContainer') outerContainer!: ElementRef;

  public autoMargins: boolean = true;
  public rightMargin: number = 120;
  public topMargin: number = 30;
  public bottomMargin: number = 60;
  public leftMargin: number = 80;

  public plotlyData: any[] = [];

  public _minimum?: number;
  public _maximum?: number;
  public _showDataPoints: boolean = true;
  public _horizontal: boolean = true;
  public _data!: GemBoxPlotData;

  _layout: any = {
    autosize: true,
    margin: {
      r: 120,
      t: 30,
      b: 60,
      l: 80
    },
    showlegend: true,
    xaxis: {
      automargin: true
    },
    yaxis: {
      autorange: true,
      automargin: true
    }
  };

  _config: any = {
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
  public set data(data: GemBoxPlotData) {
    if (!data || !data.getEntriesList()) return;
    this._data = data;
    let autocolors = this.getColors();
    let pointerAutoColors = 0;

    let entriesList = data.getEntriesList();
    let entries = entriesList ? entriesList.__internalArray() : undefined;

    if (!entries || !Array.isArray(entries)) {
      console.warn("GemBoxPlot: Received empty or structurally invalid data payload.", data);
      return;
    }

    let parsedTraces: any[] = [];

    for (let i = 0; i < entries.length; i++) {
      let entry = entries[i];
      let label = entry.getGroup();
      let pointsList = entry.getPointsList();
      let points = pointsList ? pointsList.__internalArray() : undefined;
      let color = entry.getColor();

      if (!color || !CSS.supports('color', color)) {
        color = autocolors[pointerAutoColors++];
        if (typeof entry.setColor === 'function') {
          entry.setColor(color);
        } else {
          (entry as any)._color = color;
        }
      }

      const trace: any = {
        name: label,
        type: 'box',
        marker: { color: color }
      };

      if (this._horizontal) {
        trace.x = points;
      } else {
        trace.y = points;
      }

      if(this._showDataPoints) {
        trace.boxpoints = 'all';
        trace.jitter = 0.5;
        trace.pointpos = -0.8;
      }

      parsedTraces.push(trace);
    }

    this.plotlyData = parsedTraces;
    this.updateAxisBounds();
  }

  @Input()
  public set min(minValue: number) {
    this._minimum = minValue;
    this.updateAxisBounds();
  }

  @Input()
  public set max(maxValue: number) {
    this._maximum = maxValue;
    this.updateAxisBounds();
  }

  private updateAxisBounds(): void {
    const axis = this._horizontal ? 'xaxis' : 'yaxis';

    if (this._minimum !== undefined || this._maximum !== undefined) {
      this._layout[axis].autorange = false;

      let calculatedMin = Infinity;
      let calculatedMax = -Infinity;

      if (this._data && this._data.getEntriesList()) {
        const entriesList = this._data.getEntriesList();
        const entries = entriesList ? entriesList.__internalArray() : undefined;
        if (entries) {
          for (const entry of entries) {
            const pointsList = entry.getPointsList();
            const points = pointsList ? pointsList.__internalArray() : undefined;
          if (points && points.length > 0) {
            for (const point of points) {
              if (point < calculatedMin) calculatedMin = point;
              if (point > calculatedMax) calculatedMax = point;
            }
          }
        }
        }
      }

      if (calculatedMin === Infinity) calculatedMin = 0;
      if (calculatedMax === -Infinity) calculatedMax = 1;

      const padding = (calculatedMax - calculatedMin) * 0.05;

      const finalMin = this._minimum !== undefined ? this._minimum : calculatedMin - padding;
      const finalMax = this._maximum !== undefined ? this._maximum : calculatedMax + padding;

      this._layout[axis].range = [finalMin, finalMax];
    } else {
      this._layout[axis].autorange = true;
      this._layout[axis].range = undefined;
    }

    this.updatePlot();
    this.cdr.detectChanges();
  }

  @Input()
  public set showDataPoints(showDataPoints: boolean) {
    // register the value with the generated setter, otherwise init() overwrites it with the model default
    this.setShowDataPoints(showDataPoints);
    this._showDataPoints = showDataPoints;
    this.rebuildPlot();
  }

  @Input()
  public set horizontal(horizontal: boolean) {
    this.setHorizontal(horizontal);
    this._horizontal = horizontal;
    // value and category axis swap, so settings of the previous orientation must not leak into the new one
    this._layout.xaxis = { automargin: true };
    this._layout.yaxis = { autorange: true, automargin: true };
    this.rebuildPlot();
  }

  public toggleOrientation() {
    this.horizontal = !this._horizontal;
  }

  public toggleDataPoints() {
    this.showDataPoints = !this._showDataPoints;
  }

  private rebuildPlot() {
    if (this._data) {
      this.data = this._data;
    }
    if (this.lastWidth > 0 && this.lastHeight > 0) {
      this.handleResize(this.lastWidth, this.lastHeight);
    }
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
    this.lastHeight = height;

    if (this.autoMargins && this.plotlyData && this.plotlyData.length > 0) {
      const catAxis = this._horizontal ? 'yaxis' : 'xaxis';
      const valAxis = this._horizontal ? 'xaxis' : 'yaxis';

      if (!this._layout[catAxis]) this._layout[catAxis] = {};
      this._layout[catAxis].tickmode = 'array';
      this._layout[catAxis].tickvals = this.plotlyData.map(t => t.name);
      this._layout[catAxis].ticktext = this.plotlyData.map(t => t.name);

      if (this._layout[valAxis]) {
        this._layout[valAxis].tickmode = 'auto';
        delete this._layout[valAxis].tickvals;
        delete this._layout[valAxis].ticktext;
      }

      const isSmall = width < 450 || height < 250;
      const fontSize = 12;

      this._layout.xaxis.tickfont = { size: fontSize };
      this._layout.yaxis.tickfont = { size: fontSize };

      if (!this._horizontal) {
          this._layout.xaxis.tickangle = isSmall ? -30 : 'auto';
      }

      if (this._layout.legend) {
        this._layout.legend.font = { ...this._layout.legend.font, size: fontSize };
      }
    }

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

  public setLegendPosition(legendPosition: any) {
    this.legendPosition = legendPosition;
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

    this.updatePlot();
    this.cdr.detectChanges();
  }

  public changeContainer() {
    return '';
  }
}
