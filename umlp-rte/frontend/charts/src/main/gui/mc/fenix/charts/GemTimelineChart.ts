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
import { config, GemTimelineChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemTimelineChartComponent';
import { TimelineData } from "../../../../../../build/gui2java2ts/mc/fenix/charts/gemtimelinecharttypes/TimelineData";
import { Timeline } from "../../../../../../build/gui2java2ts/mc/fenix/charts/gemtimelinecharttypes/Timeline";
import { GemListUtil } from '@umlp/commonj2ts';
import { autoColors } from './colors';
import { GemChartOptions } from './GemChartOptions';
import { fitsSideLegend, topLegendFallback } from './LegendLayout';
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
export class GemTimelineChart extends GemTimelineChartComponent implements OnInit, AfterViewInit, OnDestroy {

  // height is a CSS length in the model (e.g. "350px", "100%"), so no unit must be appended
  @HostBinding('style.height')
  get hostHeight() {
    const height: any = this.height;
    if (height === null || height === undefined || height === '') {
      return '500px';
    }
    return typeof height === 'number' ? height + 'px' : height;
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

  private resizeObserver!: ResizeObserver;

  @ViewChild('innerContainer') innerContainer!: ElementRef;
  @ViewChild('outerContainer') outerContainer!: ElementRef;

  public plotlyData: any[] = [];

  _layout: any = {
    autosize: true,
    margin: {
      r: 120,
      t: 30,
      b: 60,
      l: 80
    },
    showlegend: true,
    barmode: 'group',
    xaxis: {
      type: 'date',
      automargin: true
    },
    yaxis: {
      autorange: 'reversed',
      automargin: true,
      type: 'category'
    }
  };

  _config: any = {
    displaylogo: false,
    responsive: true
  };

  private traceBlockMap: Map<number, any[]> = new Map();

  public displayLegend: boolean = true;
  public legendPosition: string = 'right';
  private lastWidth: number = 0;
  // true while the right legend is shown above the plot because the chart is too narrow
  private legendFallback: boolean = false;

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  public getColors() {
    return autoColors;
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

    const fontSize = 12;

    if (this._layout.xaxis) {
      if (!this._layout.xaxis.tickfont) this._layout.xaxis.tickfont = {};
      this._layout.xaxis.tickfont.size = fontSize;
    }
    if (this._layout.yaxis) {
      if (!this._layout.yaxis.tickfont) this._layout.yaxis.tickfont = {};
      this._layout.yaxis.tickfont.size = fontSize;
    }

    // the fallback legend above the plot brings its own, possibly reduced font size
    if (this._layout.legend && !this.legendFallback) {
      this._layout.legend.font = { ...this._layout.legend.font, size: fontSize };
    }

    this.updatePlot();
  }

  private updatePlot(): void {
    this.plotlyData = [...this.plotlyData];
    this._layout = {
      ...this._layout,
      margin: { ...this._layout.margin },
      xaxis: { ...this._layout.xaxis },
      yaxis: { ...this._layout.yaxis }
    };
    this.cdr.detectChanges();
  }

  public setData(val: TimelineData) {
    super.setData(val);
    this.processNewData(val);
  }

  private async processNewData(data: TimelineData): Promise<void> {
    if (!data) {
      return;
    }

    let autocolors = this.getColors();
    let pointerAutoColors = 0;

    let parsedTraces: any[] = [];
    this.traceBlockMap.clear();

    let traceIndex = 0;

    (async () => {
      const timelineList = data.getTimeLineList();
      const timelines = timelineList ? await GemListUtil.toArraySync(timelineList) : undefined;
      if (!timelines) return;
      for (const timeline of timelines) {

      let xArray: number[] = [];
      let yArray: string[] = [];
      let baseArray: number[] = [];
      let hoverTextArray: string[] = [];
      let blockArray: any[] = [];

        const blockList = timeline.getBlockList();
        const blocks = blockList ? await GemListUtil.toArraySync(blockList) : undefined;
        if (!blocks) continue;
        for (const dataBlock of blocks) {
        const startMs = dataBlock.getStart().getTime();
        const endMs = dataBlock.getEnd().getTime();

        baseArray.push(startMs);
        xArray.push(endMs - startMs);
        yArray.push(dataBlock.getLane());

        const startDateStr = dataBlock.getStart().toLocaleDateString("de-DE", {timeZone: "Europe/Berlin", hour: "2-digit", minute:"2-digit", second:"2-digit"});
        const endDateStr = dataBlock.getEnd().toLocaleDateString("de-DE", {timeZone: "Europe/Berlin", hour: "2-digit", minute:"2-digit", second:"2-digit"});

        hoverTextArray.push(`${dataBlock.getLane()}<br>Start: ${startDateStr}<br>End: ${endDateStr}`);
        blockArray.push(dataBlock);
      }

      let color = timeline.getColor();
      if (!color || color == null) {
        color = autocolors[pointerAutoColors % autocolors.length];
        pointerAutoColors++;
      }

      const trace: any = {
        name: timeline.getName(),
        type: 'bar',
        orientation: 'h',
        base: baseArray,
        x: xArray,
        y: yArray,
        hovertext: hoverTextArray,
        hoverinfo: 'text+name',
        marker: { color: color },
        visible: timeline.isHidden() ? 'legendonly' : true
      };

      parsedTraces.push(trace);
      this.traceBlockMap.set(traceIndex, blockArray);
      traceIndex++;
    }

    this.plotlyData = parsedTraces;
    if (this.lastWidth > 0) {
      this.applyLegendFallback(this.lastWidth);
    }
    this.updatePlot();
  })();
  }

  @Input()
  public set hideLegend(hideLegend: boolean) {
    this.setHideLegend(hideLegend);
    this.displayLegend = hideLegend !== true;
    this.applyLegendPosition(this.legendPosition);
    this.updatePlot();
  }

  public toggleLegend() {
    this.hideLegend = this.displayLegend;
  }

  public setLegendPosition(legendPosition: string) {
    this.legendPosition = legendPosition;
    this.legendFallback = false;
    this.applyLegendPosition(legendPosition);
    if (this.lastWidth > 0) {
      this.applyLegendFallback(this.lastWidth);
    }
    this.updatePlot();
  }

  /**
   * A legend right of the plot does not fit into narrow charts, it would stick out of the component.
   * Then the legend is placed above the plot until the chart is wide enough again.
   */
  private applyLegendFallback(width: number) {
    const names = this.plotlyData.map(trace => trace.name);
    if (this.displayLegend && this.legendPosition === 'right' && names.length > 0 && !fitsSideLegend(names, width)) {
      const margin = { t: 30, b: 60, l: 80, r: 30 };
      const fallback = topLegendFallback(names, width, margin.l, margin.r);
      margin.t += fallback.height;
      this._layout.showlegend = true;
      this._layout.legend = fallback.legend;
      this._layout.margin = margin;
      this.legendFallback = true;
    } else if (this.legendFallback) {
      this.legendFallback = false;
      this.applyLegendPosition(this.legendPosition);
    }
  }

  // no 'left' position: the lane labels of the y-axis occupy that side
  private applyLegendPosition(legendPosition: string) {
    this._layout.showlegend = this.displayLegend;
    const baseMargin = { t: 30, b: 60, l: 80, r: 30 };

    if (!this.displayLegend) {
      this._layout.margin = baseMargin;
    } else if (legendPosition === 'top') {
      this._layout.legend = { orientation: 'h', yanchor: 'bottom', y: 1.02, xanchor: 'center', x: 0.5 };
      this._layout.margin = { ...baseMargin, t: 70 };
    } else if (legendPosition === 'bottom') {
      this._layout.legend = { orientation: 'h', yanchor: 'top', y: -0.2, xanchor: 'center', x: 0.5 };
      this._layout.margin = { ...baseMargin, b: 100 };
    } else {
      this._layout.legend = { orientation: 'v', yanchor: 'auto', y: 0.5, xanchor: 'left', x: 1.02 };
      this._layout.margin = { ...baseMargin, r: 120 };
    }
  }

  public setShowAfter(event: any) {
    if (event && event instanceof Date) {
      const date: Date = event;
      this._layout.xaxis.range = [date.getTime(), undefined];
      this._layout.xaxis.autorange = false;
    } else {
      this._layout.xaxis.autorange = true;
      this._layout.xaxis.range = undefined;
    }
    this.updatePlot();
  }

  public handlePlotClick(event: any) {
    if (event && event.points && event.points.length > 0) {
      const point = event.points[0];
      const traceIndex = point.curveNumber;
      const dataPointIndex = point.pointIndex;

      if (this.traceBlockMap.has(traceIndex)) {
        const blocks = this.traceBlockMap.get(traceIndex);
        if (blocks && dataPointIndex < blocks.length) {
          const tlBlock = blocks[dataPointIndex];
          if (this.getBlockClick()) {
            this.getBlockClick()(tlBlock);
          }
        }
      }
    }
  }
}

GemTimelineChart["__class"] = "gui.charts.TimeLineChart";
