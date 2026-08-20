/* (c) https://github.com/MontiCore/monticore */
import {Component, ChangeDetectionStrategy,ElementRef, ViewChild, AfterViewInit, OnDestroy, HostBinding, Input} from '@angular/core';
import {GemTimeSeriesChartComponent, config} from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemTimeSeriesChartComponent';
import ApexCharts from 'apexcharts';
import { TimeSeriesData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemtimeseriescharttypes/TimeSeriesData';
import { TimeSeries } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemtimeseriescharttypes/TimeSeries';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styles: config.styles,
  styleUrls: [...config.styleUrls]
})
export class GemTimeSeriesChart extends GemTimeSeriesChartComponent implements OnDestroy, AfterViewInit {
  @HostBinding('style.height') get styleHeight() { return this.getHeight(); }
  @HostBinding('style.width') get styleWidth() { return this.getWidth(); }

  options: any;
  chart: ApexCharts;
  @ViewChild('tSChart') targetElement! : ElementRef<HTMLDivElement>;
  private liveData: boolean = true;
  private resizeObserver: ResizeObserver;
  private resizeTimeout: any;
  private chartId: string = 'ts-chart-' + Math.random().toString(36).substr(2, 9);
  private userMinX: number | null = null;
  private userMaxX: number | null = null;
  private lastUpdateTimestamp: number = 0;
  private lastRenderedFingerprint: string = '';

  // min. Abstand zwischen zwei ApexCharts-Updates; im Test überschreibbar
  public static renderMinIntervalMs: number = 500;
  private static warnedPointCap: boolean = false;
  // Fallback-Cap pro Serie, wenn kein viewPort gesetzt ist
  private static readonly MAX_POINTS_PER_SERIES = 10000;
  private lastRenderTime: number = 0;
  private renderTimer: any = null;
  private pendingRender: boolean = false;

  constructor(private hostElement: ElementRef) {
    super();
  }

  private toListArray<T>(list: any): T[] {
    if (!list) return [];
    if (Array.isArray(list)) return list;
    if (typeof list.__internalArray === 'function') return list.__internalArray();
    const count = (typeof list.size === 'function') ? list.size() : (list.length || 0);
    const arr = [];
    for (let i = 0; i < count; i++) {
      arr.push((typeof list.get === 'function') ? list.get(i) : list[i]);
    }
    return arr;
  }

  public async init() {
    await super.init();

    // the generated component re-runs init() on every route paramMap
    // emission; without disposing the previous instance its SVG stays in the
    // DOM as a frozen duplicate of the live chart
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
      this.lastRenderedFingerprint = '';
      if (this.renderTimer) {
        clearTimeout(this.renderTimer);
        this.renderTimer = null;
      }
      this.pendingRender = false;
    }

    // "area" (default) or "line"; with many overlapping series "line" stays readable
    const chartType = ((this as any).getChartType && (this as any).getChartType()) || 'area';

    this.options = {
      noData: { text: "Loading..." },
      dataLabels: { enabled: false },
      // thin stroke + low-opacity fill so several overlapping series remain
      // distinguishable (a full-opacity area fill turns them into one blob)
      stroke: { curve: 'smooth', width: 2 },
      fill: chartType === 'area'
        ? { type: 'gradient', gradient: { opacityFrom: 0.3, opacityTo: 0.03 } }
        : { type: 'solid', opacity: 1 },
      chart: {
        id: this.chartId,
        type: chartType,
        height: '100%',
        width: '100%',
        // no animations: live series update faster than an animation can
        // finish, and ApexCharts leaves ghost paths (a frozen copy of the
        // line) behind when an update interrupts a running animation
        animations: {
            enabled: false
        },
        events: {
          click: (event, chartContext, config) => {
            const target = event.target;
            if (target && (target.classList.contains('gem-clickable-annotation') || target.closest('.apexcharts-xaxis-annotations'))) {
                const textContent = target.textContent;
                if (textContent && this.getEventClick()) {
                    const data = this.getData();
                    if (data) {
                        const eventsList = this.toListArray<any>(data.getEventsList());
                        for (const evt of eventsList) {
                            if (evt.getName().trim() === textContent.trim()) {
                                this.getEventClick()(evt);
                                break;
                            }
                        }
                    }
                }
            }
          },
          zoomed: (chartContext, { xaxis }) => {
            if (Date.now() - this.lastUpdateTimestamp < 1000) return;
            const globals = chartContext.w.globals;
            // Only update if it's a real zoom (range is smaller than full range)
            // or if we explicitly want to clear it (Home button)
            if (globals.isUserZoomed) {
              this.userMinX = xaxis.min || null;
              this.userMaxX = xaxis.max || null;
            } else {
              // Only clear if we are not currently updating and globals say no zoom
              this.userMinX = null;
              this.userMaxX = null;
            }
          },
          scrolled: (chartContext, { xaxis }) => {
            if (Date.now() - this.lastUpdateTimestamp < 1000) return;
            const globals = chartContext.w.globals;
            if (globals.isUserZoomed) {
              this.userMinX = xaxis.min || null;
              this.userMaxX = xaxis.max || null;
            }
          }
        }
      },
      series: [],
      // timestamps come from zone-less LocalDateTime values parsed as local
      // time; rendering them in UTC (ApexCharts default) shifts all labels
      // and tooltips by the timezone offset
      xaxis: { type: 'datetime', labels: { datetimeUTC: false } },
      yaxis: {
        labels: {
          formatter: (val) => (val !== null && val !== undefined) ? val.toFixed(2) : val
        }
      }
    };

    if (this.targetElement) {
      this.chart = new ApexCharts(this.targetElement.nativeElement, this.options);
      await this.chart.render();
      if (this.getData()) {
        this.processNewData(this.getData());
      }
    }
  }

  ngAfterViewInit() {
    this.resizeObserver = new ResizeObserver(() => {
      if (this.resizeTimeout) {
        clearTimeout(this.resizeTimeout);
      }
      this.resizeTimeout = setTimeout(() => {
        if (this.chart) {
          this.lastUpdateTimestamp = Date.now();
          const updateParams: any = { chart: { height: '100%', width: '100%' } };
          if (this.userMinX !== null && this.userMaxX !== null) {
            updateParams.xaxis = { min: this.userMinX, max: this.userMaxX };
          }
          this.chart.updateOptions(updateParams, false, false);
        }
      }, 100);
    });
    this.resizeObserver.observe(this.hostElement.nativeElement);
  }

  ngOnDestroy() {
    if (this.renderTimer) {
      clearTimeout(this.renderTimer);
      this.renderTimer = null;
    }
    if (this.resizeTimeout) clearTimeout(this.resizeTimeout);
    if (this.resizeObserver) this.resizeObserver.disconnect();
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
    // run the generated cleanup (route subscription, component destroy)
    super.ngOnDestroy();
  }

  public setData(val: TimeSeriesData) {
    super.setData(val);
    if (this.liveData) {
      this.processNewData(val);
    }
  }

  private async processNewData(data: TimeSeriesData): Promise<void> {
    if (!this.chart || !data) return;

    // keyed by name (the series identity for ApexCharts): duplicate entries
    // in the bound data are merged instead of rendered as extra series, so
    // the number of SVG paths stays bounded by the number of distinct series
    const seriesByName = new Map<string, any>();
    const seriesList = this.toListArray<TimeSeries>(data.getSeriesList());

    for (const series of seriesList) {
      // series marked hidden (e.g. deselected channels) must not be plotted
      if (typeof (series as any).isHidden === 'function' && (series as any).isHidden()) {
        continue;
      }
      let seriesData = [];
      const dpList = this.toListArray<any>(series.getDatapointsList());

      for (const dp of dpList) {
          const timestamp = dp.getTimestamp();
          if (timestamp) {
            const time = (typeof timestamp.getTime === 'function') ? timestamp.getTime() : new Date(timestamp).getTime();
            seriesData.push([time, dp.getValue()]);
          }
      }

      const name = series.getName();
      const existing = seriesByName.get(name);
      if (existing) {
        for (const p of seriesData) existing.data.push(p);
        existing.merged = true;
        existing.color = existing.color || series.getColor() || undefined;
      } else {
        seriesByName.set(name, {
          name: name,
          data: seriesData,
          color: series.getColor() || undefined
        });
      }
    }

    const newSeries = [];
    for (const s of seriesByName.values()) {
      if (s.merged) {
        s.data.sort((a, b) => a[0] - b[0]);
        delete s.merged;
      }
      newSeries.push(s);
    }

    this.sliceToViewPort(newSeries);

    // Process Events
    const annotationsX = [];
    const eventsList = this.toListArray<any>(data.getEventsList());
    for (const event of eventsList) {
      if (event.isShow()) {
        const timestamp = event.getTimestamp();
        const time = (typeof timestamp.getTime === 'function') ? timestamp.getTime() : new Date(timestamp).getTime();
        annotationsX.push({
          x: time,
          borderColor: event.getBackgroundColor() || '#FEB019',
          label: {
            borderColor: event.getBackgroundColor() || '#FEB019',
            style: {
              color: event.getTextColor() || '#fff',
              background: event.getBackgroundColor() || '#FEB019',
              cssClass: 'gem-clickable-annotation'
            },
            text: event.getName(),
          }
        });
      }
    }

    // data is bound via a compute method, so Angular re-sets it on every
    // change-detection cycle — many times per backend tick. Only touch the
    // chart when the content actually changed.
    const fingerprint = newSeries.map(s => {
      const n = s.data.length;
      const first = n > 0 ? s.data[0][0] : '';
      const last = n > 0 ? s.data[n - 1][0] + ':' + s.data[n - 1][1] : '';
      return s.name + '#' + n + '#' + first + '#' + last;
    }).join('|') + '@' + annotationsX.length;
    if (fingerprint === this.lastRenderedFingerprint) {
      return;
    }
    this.lastRenderedFingerprint = fingerprint;

    this.options.series = newSeries;
    this.options.annotations = { xaxis: annotationsX };

    this.refreshChart();
  }

  // "30s" | "5m" | "2h" (Zeitfenster) | "500p" (letzte N Punkte), sonst null
  private parseViewPort(vp: string): { value: number, unit: string, millis: number } | null {
    if (!vp) return null;
    const match = vp.match(/^(\d+)([shmp])$/);
    if (!match) return null;
    const value = parseInt(match[1]);
    const unit = match[2];
    let millis = value * 1000;
    if (unit === 'm') millis *= 60;
    if (unit === 'h') millis *= 3600;
    return { value, unit, millis };
  }

  private latestTimestamp(series: any[]): number {
    let latestX = -1;
    for (const s of series) {
      if (s.data && s.data.length > 0) {
        const lastPoint = s.data[s.data.length - 1];
        const lx = Array.isArray(lastPoint) ? lastPoint[0] : lastPoint.x;
        if (lx > latestX) latestX = lx;
      }
    }
    return latestX;
  }

  // Schneidet die Daten jeder Serie auf das viewPort-Fenster zu (plus 1
  // Punkt Vorlauf, damit die Linie am linken Rand nicht abreißt). Ohne
  // viewPort: großzügiges Punkte-Cap, damit ApexCharts nie die volle
  // Historie rendern muss.
  private sliceToViewPort(series: any[]): void {
    const win = this.parseViewPort(this.getViewPort());
    if (!win) {
      const cap = GemTimeSeriesChart.MAX_POINTS_PER_SERIES;
      let capped = false;
      for (const s of series) {
        if (s.data.length > cap) {
          s.data = s.data.slice(s.data.length - cap);
          capped = true;
        }
      }
      if (capped && !GemTimeSeriesChart.warnedPointCap) {
        GemTimeSeriesChart.warnedPointCap = true;
        console.warn('GemTimeSeriesChart: no viewPort configured, capping series to the last '
          + cap + ' points; set viewPort (e.g. "5m") to control the window.');
      }
      return;
    }

    if (win.unit === 'p') {
      const keep = win.value + 1; // +1 Vorlaufpunkt
      for (const s of series) {
        if (s.data.length > keep) s.data = s.data.slice(s.data.length - keep);
      }
      return;
    }

    const latestX = this.latestTimestamp(series);
    if (latestX === -1) return;
    const minX = latestX - win.millis;
    for (const s of series) {
      const data = s.data;
      // von hinten scannen: kostet nur O(behaltene Punkte)
      let start = data.length;
      while (start > 0) {
        const p = data[start - 1];
        const px = Array.isArray(p) ? p[0] : p.x;
        if (px < minX) break;
        start--;
      }
      if (start > 0) start--; // 1 Punkt Vorlauf vor der Fenstergrenze
      if (start > 0) s.data = data.slice(start);
    }
  }

  // Throttle: max. ~2 Updates/s an ApexCharts; trailing edge garantiert,
  // dass der letzte Stand nach Ablauf des Intervalls immer gerendert wird
  private refreshChart(): void {
    if (!this.chart) return;
    this.pendingRender = true;
    this.scheduleRender();
  }

  private scheduleRender(): void {
    if (this.renderTimer) return; // laufender Timer rendert den letzten Stand
    const wait = this.lastRenderTime + GemTimeSeriesChart.renderMinIntervalMs - Date.now();
    if (wait <= 0) {
      this.renderNow();
    } else {
      this.renderTimer = setTimeout(() => {
        this.renderTimer = null;
        if (this.pendingRender) this.renderNow();
      }, wait);
    }
  }

  private renderNow(): void {
    if (!this.chart) return;
    this.pendingRender = false;
    this.lastRenderTime = Date.now();
    this.lastUpdateTimestamp = this.lastRenderTime;

    const updateParams: any = {
      series: this.options.series,
      annotations: this.options.annotations
    };

    // Determine x-axis range (ViewPort or manual Zoom)
    let minX = this.userMinX;
    let maxX = this.userMaxX;

    const win = this.parseViewPort(this.getViewPort());
    if (minX === null && win) {
      const latestX = this.latestTimestamp(this.options.series);
      if (latestX !== -1) {
        if (win.unit === 'p') {
          // Points-based window: Find earliest timestamp of the last N points across all series
          let targetMinX = latestX;
          for (const s of this.options.series) {
            if (s.data && s.data.length > 0) {
              const idx = Math.max(0, s.data.length - win.value);
              const p = s.data[idx];
              const px = Array.isArray(p) ? p[0] : p.x;
              if (px < targetMinX) targetMinX = px;
            }
          }
          minX = targetMinX;
          maxX = latestX;
        } else {
          // Time-based window
          maxX = latestX;
          minX = maxX - win.millis;
        }

        updateParams.xaxis = { min: minX, max: maxX };
      }
    } else if (minX !== null && maxX !== null) {
      updateParams.xaxis = { min: minX, max: maxX };
    }

    this.chart.updateOptions(updateParams, false, false, false);

    // Hard override if we have user zoom or viewPort
    if (minX !== null && maxX !== null) {
      this.chart.zoomX(minX, maxX);
    }
  }

  public toggleLiveData(event: any) {
    this.liveData = !this.liveData;
    if (this.liveData && this.getData()) {
      this.processNewData(this.getData());
    }
  }

  public getToggleLiveDataButtonLabel(): string {
    return this.liveData ? "Pause Live Stream" : "Resume Live Stream";
  }

  @Input()
  public set showAfter(val: Date) {
      if (this.chart) {
          const time = (val && typeof (val as any).getTime === 'function') ? (val as any).getTime() : val;
          this.chart.updateOptions({ xaxis: { min: time } }, false, false);
      }
  }

  @Input()
  public set showUntil(val: Date) {
      if (this.chart) {
          const time = (val && typeof (val as any).getTime === 'function') ? (val as any).getTime() : val;
          this.chart.updateOptions({ xaxis: { max: time } }, false, false);
      }
  }
}

// Marker class for JSON deserialization in RTE
GemTimeSeriesChart["__class"] = "gui.viz.TimeSeriesChart2";
