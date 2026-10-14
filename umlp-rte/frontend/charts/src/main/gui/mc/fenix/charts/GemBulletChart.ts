/* (c) https://github.com/MontiCore/monticore */
import { ChangeDetectionStrategy, Component, Input, ElementRef, AfterViewInit, OnDestroy, ChangeDetectorRef, ViewChild, HostBinding } from '@angular/core';

import { CommonModule } from '@angular/common';
import { config, GemBulletChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemBulletChartComponent';
import { PlotlyModule } from 'angular-plotly.js';
import * as Plotly from 'plotly.js-dist-min';
import { setupResizeObserver } from './ResizeObserverUtil';
import { GemChartOptions } from './GemChartOptions';

PlotlyModule.plotlyjs = Plotly;

@Component({
  standalone: true,
  imports: [CommonModule, GemChartOptions, PlotlyModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GemBulletChart extends GemBulletChartComponent implements AfterViewInit, OnDestroy {
  @HostBinding('style.display') get hostDisplay() { return 'block'; }
  @HostBinding('style.width') get hostWidth() { return '100%'; }
  @HostBinding('style.height') get hostHeight() { return '100%'; }

  private resizeObserver!: ResizeObserver;

  @ViewChild('innerContainer') innerContainer!: ElementRef;
  @ViewChild('outerContainer') outerContainer!: ElementRef;

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) { super(); }

  topMargin: number = 15;
  bottomMargin: number = 35;
  leftMargin: number = 150;
  rightMargin: number = 50;

  autoMargins: boolean = true;
  stepColors: string[] = ["#8EBAE5", "#fdd48f", "#b8d698", "#e69679", "#BCB5D7", "#FFF59B", "#7DA4A7", "#89CCCF", "#CD8B87", "#A8859E"];
  private readonly COLOR_SUCCESS = "#11750e";
  private readonly COLOR_DANGER = "#cc071e";

  private readonly STEP_PALETTES: { [key: number]: string[] } = {
    2: ["#e69679", "#b8d698"],
    3: ["#e69679", "#fdd48f", "#b8d698"],
    4: ["#e69679", "#fdd48f", "#FFF59B", "#b8d698"],
    5: ["#e69679", "#fdd48f", "#FFF59B", "#b8d698", "#8EBAE5"],
    6: ["#e69679", "#fdd48f", "#FFF59B", "#b8d698", "#8EBAE5", "#BCB5D7"],
    7: ["#e69679", "#fdd48f", "#FFF59B", "#b8d698", "#8EBAE5", "#BCB5D7", "#A8859E"]
  };

  arrowPosition: string = "top";
  fontSize: number = 13;
  thresholdWidth: number = 3;
  defaultBackgroundColor: string = "white";
  defaultBarColor: string = "#00549F";

  // toggled in the settings menu
  public showNumber: boolean = true;
  public showDelta: boolean = true;
  public showThreshold: boolean = true;

  private _rawTitle: string = "";
  private _rawSubtitle: string = "";
  private _lastWidth: number = 0;
  private _renderAdjustCount: number = 0;

  _data: any[] = [
    {
      type: "indicator",
      mode: "number+gauge+delta",
      value: 0,
      number: { font: { size: 15 } },
      delta: {
        reference: 0,
        position: this.arrowPosition,
        increasing: { color: this.COLOR_SUCCESS },
        decreasing: { color: this.COLOR_DANGER },
        font: { size: 13 }
      },
      title: {
        text: "",
        align: "left",
        font: { size: this.fontSize }
      },
      gauge: {
        shape: "bullet",
        axis: {
          range: [0, 0],
          tickfont: { size: 11 },
          tickformat: "s"
        },
        threshold: {
          line: { color: "", width: this.thresholdWidth },
          value: 0
        },
        backgroundColor: this.defaultBackgroundColor,
        steps: [],
        bar: { color: this.defaultBarColor }
      }
    }
  ];

  _layout: any = {
    margin: {
      r: this.rightMargin,
      t: this.topMargin,
      b: this.bottomMargin,
      l: this.leftMargin
    }
  };

  _config: any = {
    displaylogo: false,
    responsive: true
  };

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
    this._lastWidth = width;
    this._renderAdjustCount = 0;

    if (this.autoMargins) {
      const maxAllowedLeftMargin = Math.min(width * 0.45, 450);
      const targetLeftMargin = Math.max(160, Math.min(220, width * 0.25));
      this.leftMargin = Math.min(maxAllowedLeftMargin, targetLeftMargin);
      this.rightMargin = Math.max(100, Math.min(150, width * 0.25));

      if (height < 100) {
        const crampedPadding = height * 0.35;
        this.topMargin = crampedPadding;
        this.bottomMargin = crampedPadding;
      } else {
        const idealBarHeight = 35;
        const totalPadding = Math.max(20, (height - idealBarHeight) / 2);
        this.topMargin = totalPadding;
        this.bottomMargin = totalPadding;
      }

      this._layout.margin.l = this.leftMargin;
      this._layout.margin.r = this.rightMargin;
      this._layout.margin.b = this.bottomMargin;
      this._layout.margin.t = this.topMargin;
    }

    this.fontSize = Math.max(10, Math.min(14, width * 0.02));
    this._data[0].title.font.size = this.fontSize;
    this._data[0].number.font.size = Math.max(12, Math.min(16, width * 0.025));
    this._data[0].delta.font.size = Math.max(11, Math.min(14, width * 0.02));

    this._data[0].gauge.axis.tickfont.size = Math.max(9, Math.min(11, height * 0.08));

    this.adjustLeftMargin(width);
    this.updateTitleText();

    // the plot lives in #innerContainer, which has a left padding (see template); sizing the plot to the
    // whole host width would push its right edge out of the component
    this._layout.width = Math.max(0, width - GemBulletChart.PLOT_PADDING_LEFT);
    this._layout.height = height;

    this.updatePlot();
    this.cdr.detectChanges();
  }

  private static readonly PLOT_PADDING_LEFT = 12;

  private updatePlot(): void {
    this._data = [...this._data];
    this._layout = { ...this._layout };
  }

  private static canvasContext: CanvasRenderingContext2D | null = null;

  private measureTextWidth(text: string, fontSize: number, fontWeight: string = 'normal'): number {
    if (typeof document === 'undefined' || text.length === 0) {
      return 0;
    }
    if (!GemBulletChart.canvasContext) {
      GemBulletChart.canvasContext = document.createElement('canvas').getContext('2d');
    }
    GemBulletChart.canvasContext!.font = `${fontWeight} ${fontSize}px "Open Sans", "Helvetica Neue", Arial, sans-serif`;
    return GemBulletChart.canvasContext!.measureText(text).width;
  }

  private stripHtmlTags(html: string): string {
    return html.replace(/<[^>]+>/g, '');
  }

  private adjustLeftMargin(width: number): void {
    if (!this.autoMargins) {
      return;
    }
    const padding = 35;
    const maxAllowed = Math.min(width * 0.45, 450);
    const targetLeftMargin = Math.max(160, Math.min(220, width * 0.25));
    let maxWidth = 0;

    const avgCharWidth = this.fontSize * 0.8;
    const maxCharsInMargin = Math.max(10, Math.floor((targetLeftMargin - 25) / avgCharWidth));

    if (this._rawTitle) {
      const wrappedTitle = this.wrapText(this._rawTitle, maxCharsInMargin);
      const lines = wrappedTitle.split('<br>');
      for (const text of lines) {
        const line = this.stripHtmlTags(text);
        if (!line) continue;
        const lineWidth = this.measureTextWidth(line, this.fontSize, 'bold');
        if (lineWidth > maxWidth) {
          maxWidth = lineWidth;
        }
      }
    }
    if (this._rawSubtitle) {
      const subtitleFontSize = this.fontSize * 0.8;
      const subtitleAvgCharWidth = subtitleFontSize * 0.8;
      const maxCharsSubtitle = Math.max(10, Math.floor((targetLeftMargin - 25) / subtitleAvgCharWidth));
      const wrappedSubtitle = this.wrapText(this._rawSubtitle, maxCharsSubtitle);
      const lines = wrappedSubtitle.split('<br>');
      for (const text of lines) {
        const line = this.stripHtmlTags(text);
        if (!line) continue;
        const lineWidth = this.measureTextWidth(line, subtitleFontSize, 'normal');
        if (lineWidth > maxWidth) {
          maxWidth = lineWidth;
        }
      }
    }

    const required = maxWidth + padding;
    this.leftMargin = Math.max(targetLeftMargin, Math.min(maxAllowed, required));
    this._layout.margin.l = this.leftMargin;
  }

  onAfterPlot(event: any): void {
    setTimeout(() => {
      console.log('[GemBulletChart] afterPlot fired');
      this.bringBulletBarToFront();
      if (this.autoMargins) {
        this.adjustLeftMarginFromRenderedText();
      }
    }, 0);
  }

  private adjustLeftMarginFromRenderedText(): void {
    if (!this.innerContainer) return;
    const svg = this.innerContainer.nativeElement.querySelector('svg');
    if (!svg) return;
    const textElements = Array.from(svg.querySelectorAll('text')) as SVGTextElement[];
    const titleWords = `${this._rawTitle} ${this._rawSubtitle}`.split(/\s+/).filter(w => w.length > 0);
    if (titleWords.length === 0) return;
    const titleTextElements = textElements.filter(el => {
      const text = el.textContent || '';
      return titleWords.some(word => text.includes(word));
    });
    if (titleTextElements.length === 0) return;

    const containerRect = this.el.nativeElement.getBoundingClientRect();
    const titleRects = titleTextElements.map(el => el.getBoundingClientRect());
    const titleRight = Math.max(...titleRects.map(r => r.right - containerRect.left));
    const titleLeft = Math.min(...titleRects.map(r => r.left - containerRect.left));
    const padding = 15;
    const targetLeftMargin = Math.max(160, Math.min(220, this._lastWidth * 0.25));

    let needsAdjustment = false;
    let required = Math.max(targetLeftMargin, this.leftMargin);

    if (titleRight > this.leftMargin - padding) {
      required = Math.max(required, titleRight + padding);
      needsAdjustment = true;
    }
    if (titleLeft < 15) {
      const deficiency = 15 - titleLeft;
      required = Math.max(required, this.leftMargin + deficiency);
      needsAdjustment = true;
    }

    if (needsAdjustment) {
      const maxAllowed = Math.max(targetLeftMargin, Math.min(this._lastWidth * 0.45, 450));
      const newLeftMargin = Math.min(maxAllowed, required);
      if (newLeftMargin > this.leftMargin + 2 && this._renderAdjustCount < 5) {
        this._renderAdjustCount++;
        this.leftMargin = newLeftMargin;
        this._layout.margin.l = this.leftMargin;
        this.updateTitleText();
        this.updatePlot();
        this.cdr.detectChanges();
      }
    }
  }

  private bringBulletBarToFront(): void {
    if (!this.innerContainer) return;

    // Plotly creates multiple <svg class="main-svg"> siblings (background, chart, hover).
    // The first <svg> is only the background layer, so we query g.bullet across the
    // entire component and re-append the value-bar group to the end of its bullet group.
    const bulletGroups = Array.from(this.innerContainer.nativeElement.querySelectorAll('g.bullet')) as SVGGElement[];
    console.log('[GemBulletChart] bullet groups found:', bulletGroups.length);

    for (const bullet of bulletGroups) {
      const valueBullets = Array.from(bullet.querySelectorAll('g.value-bullet')) as SVGGElement[];
      console.log('[GemBulletChart] value-bullet groups in bullet:', valueBullets.length);

      for (const valueBullet of valueBullets) {
        bullet.appendChild(valueBullet);
      }
    }
  }

  private wrapText(text: string, maxChars: number): string {
    if (!text) return "";
    const words = text.split(" ");
    let lines = [];
    let currentLine = "";

    for (const word of words) {
      if ((currentLine + word).length > maxChars && currentLine.length > 0) {
        lines.push(currentLine.trim());
        currentLine = word + " ";
      } else {
        currentLine += word + " ";
      }
    }
    if (currentLine.trim().length > 0) {
      lines.push(currentLine.trim());
    }
    return lines.join("<br>");
  }

  private updateTitleText() {
    let combinedText = "";
    const avgCharWidth = this.fontSize * 0.8;
    const maxCharsInMargin = Math.max(10, Math.floor((this.leftMargin - 20) / avgCharWidth));
    const subtitleFontSize = this.fontSize * 0.8;
    const subtitleAvgCharWidth = subtitleFontSize * 0.8;
    const maxCharsSubtitle = Math.max(10, Math.floor((this.leftMargin - 20) / subtitleAvgCharWidth));

    if (this._rawTitle) {
      combinedText = "<b>" + this.wrapText(this._rawTitle, maxCharsInMargin) + "</b>";
    }
    if (this._rawSubtitle && this._rawTitle) {
      combinedText += "<br><span style='color: gray; font-size:0.8em'>" + this.wrapText(this._rawSubtitle, maxCharsSubtitle) + "</span>";
    }

    this._data[0].title.text = combinedText;
  }

  public toggleNumber() {
    this.showNumber = !this.showNumber;
    this.updateMode();
  }

  public toggleDelta() {
    this.showDelta = !this.showDelta;
    this.updateMode();
  }

  public toggleThreshold() {
    this.showThreshold = !this.showThreshold;
    this._data[0].gauge.threshold.line.width = this.showThreshold ? this.thresholdWidth : 0;
    this.updatePlot();
    this.cdr.detectChanges();
  }

  private updateMode() {
    const parts = [];
    if (this.showNumber) parts.push('number');
    parts.push('gauge');
    if (this.showDelta) parts.push('delta');
    this._data[0].mode = parts.join('+');
    this.updatePlot();
    this.cdr.detectChanges();
  }

  @Input()
  public set value(value: number) {
    if (value != null) {
      this._data[0].value = value;
      this._data[0].gauge.threshold.line.color = (value >= this._data[0].gauge.threshold.value) ? this.COLOR_SUCCESS : this.COLOR_DANGER;
      this.updatePlot();
    }
  }

  @Input()
  public set reference(referenceValue: number) {
    this._data[0].delta.reference = referenceValue;
    this.updatePlot();
  }

  @Input()
  public set thresholdValue(thresholdValue: number) {
    this._data[0].gauge.threshold.value = thresholdValue;
    this._data[0].gauge.threshold.line.color = (this._data[0].value >= thresholdValue) ? this.COLOR_SUCCESS : this.COLOR_DANGER;
    this.updatePlot();
  }

  @Input()
  public set title(title: string) {
    if (title != null) {
      this._rawTitle = title;
      this.updateTitleText();
      if (this._lastWidth > 0) {
        this.adjustLeftMargin(this._lastWidth);
      }
      this.updatePlot();
    }
  }

  @Input()
  public set subtitle(subtitle: string) {
    if (subtitle != null) {
      this._rawSubtitle = subtitle;
      this.updateTitleText();
      if (this._lastWidth > 0) {
        this.adjustLeftMargin(this._lastWidth);
      }
      this.updatePlot();
    }
  }

  @Input()
  public set range(range: any) {
    if (!range) return;
    const arr: number[] = Array.isArray(range) ? range : range.__internalArray();
    this._data[0].gauge.axis.range = arr;
    this.updatePlot();
  }

  @Input()
  public set backgroundColor(backgroundColor: string) {
    this._data[0].gauge.backgroundColor = backgroundColor != null ? backgroundColor : this.defaultBackgroundColor;
    this.updatePlot();
  }

  @Input()
  public set steps(stepsInput: any) {
    if (!stepsInput) return;
    const steps: number[] = Array.isArray(stepsInput) ? stepsInput : stepsInput.__internalArray();
    if (!steps || steps.length === 0) return;
    this._data[0].gauge.steps = [];
    let modifiedsteps = [...steps];
    if (steps[0] != this._data[0].gauge.axis.range[0]) {
      modifiedsteps.unshift(this._data[0].gauge.axis.range[0]);
    }
    if (modifiedsteps[modifiedsteps.length - 1] != this._data[0].gauge.axis.range[1]) {
      modifiedsteps.push(this._data[0].gauge.axis.range[1]);
    }
    for (let index = 0; index < modifiedsteps.length - 1; index++) {
      this._data[0].gauge.steps.push({
        range: [modifiedsteps[index], modifiedsteps[index + 1]]
      });
    }
    let count = modifiedsteps.length - 1;
    if (count === 1) {
      this._data[0].gauge.steps[0].color = this.defaultBackgroundColor;
    } else {
      const baseColors = this.STEP_PALETTES[Math.min(count, 7)];
      for (let index = 0; index < count; index++) {
        if (index < baseColors.length) {
          this._data[0].gauge.steps[index].color = baseColors[index];
        } else {
          this._data[0].gauge.steps[index].color = this.stepColors[index % 10];
        }
      }
    }
    this.updatePlot();
  }

  @Input()
  public set barColor(barColor: string) {
    this._data[0].gauge.bar.color = barColor != null ? barColor : this.defaultBarColor;
    this.updatePlot();
  }

  @Input()
  public set margins(margins: number[] | number) {
    if (margins != null) {
      this.autoMargins = false;
      if (typeof margins == "number") {
        this._layout.margin = { t: margins, b: margins, l: margins, r: margins };
      } else {
        if (margins.length == 1) {
          this._layout.margin = { t: margins[0], b: margins[0], l: margins[0], r: margins[0] };
        } else if (margins.length == 2) {
          this._layout.margin = { t: margins[0], b: margins[0], l: margins[1], r: margins[1] };
        } else if (margins.length == 3) {
          this._layout.margin = { t: margins[0], b: margins[2], l: margins[1], r: margins[1] };
        } else if (margins.length >= 4) {
          this._layout.margin = { t: margins[0], r: margins[1], b: margins[2], l: margins[3] };
        }
      }
      this.leftMargin = this._layout.margin.l;
    } else {
      this.autoMargins = true;
      this._layout.margin = { t: this.topMargin, b: this.bottomMargin, l: this.leftMargin, r: this.rightMargin };
      this.updateTitleText();
    }
    this.updatePlot();
  }
}
