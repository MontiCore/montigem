/* (c) https://github.com/MontiCore/monticore */

import { ChangeDetectionStrategy, Component, Input, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemBulletChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemBulletChartComponent';
import {GemListUtil, IUMLPList} from "@umlp/commonj2ts";

@Component({
  imports: [CommonModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GemBulletChart extends GemBulletChartComponent implements AfterViewInit {
  @ViewChild('chartContainer') chartContainer!: ElementRef;

  private Plotly: any;
  private plotInitialized = false;

  constructor() { super(); }

  topMargin: number = 20;
  bottomMargin: number = 25;
  leftMargin: number = 50; // Increased base margin slightly
  rightMargin: number = 0;

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
  fontSize: number = 15;
  thresholdWidth: number = 3;
  defaultBackgroundColor: string = "white";
  defaultBarColor: string = "#00549F";

  private _rawTitle: string = "";
  private _rawSubtitle: string = "";

  _data: any[] = [
    {
      type: "indicator",
      mode: "number+gauge+delta",
      value: 0,
      delta: { reference: 0, position: this.arrowPosition, increasing: { color: this.COLOR_SUCCESS }, decreasing: { color: this.COLOR_DANGER } },
      title: {
        text: "",
        align: "right",
        font: { size: this.fontSize }
      },
      gauge: {
        shape: "bullet",
        axis: { range: [0, 0] },
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

  _layout = {
    margin: {
      r: this.rightMargin,
      t: this.topMargin,
      b: this.bottomMargin,
      l: this.leftMargin
    }
  };

  ngAfterViewInit(): void {
    if (this.chartContainer) {
      // Small timeout to ensure the container has its final dimensions
      setTimeout(() => {
        this.loadPlotlyAndCreatePlot();
      }, 100);
    }
  }

  private async loadPlotlyAndCreatePlot() {
    try {
      const plotlyModule = await import('plotly.js-dist-min');
      this.Plotly = plotlyModule.default || plotlyModule;
      this.createPlot();
    } catch (error) {
      console.error("Failed to load Plotly lazily:", error);
    }
  }

  private createPlot(): void {
    if (this.chartContainer && this._data && this._layout && this.Plotly) {
      this.Plotly.newPlot(this.chartContainer.nativeElement, this._data, this._layout, { responsive: true });
      this.plotInitialized = true;
    }
  }

  private updatePlot(): void {
    if (this.plotInitialized && this.chartContainer && this._data && this._layout && this.Plotly) {
      this.Plotly.react(this.chartContainer.nativeElement, this._data, this._layout);
    }
  }

  private calculateExactTextWidth(htmlText: string): number {
    if (!htmlText) return 0;

    // Create a hidden span element to measure the actual rendered width
    const dummyElement = document.createElement('span');
    dummyElement.style.position = 'absolute';
    dummyElement.style.visibility = 'hidden';
    dummyElement.style.whiteSpace = 'nowrap';

    dummyElement.style.fontSize = this.fontSize + 'px';
    dummyElement.style.fontFamily = '"Open Sans", verdana, arial, sans-serif';

    dummyElement.innerHTML = htmlText;

    document.body.appendChild(dummyElement);
    const width = dummyElement.offsetWidth;
    document.body.removeChild(dummyElement);

    return width;
  }

  private updateTitleText() {
    let combinedText = "";

    if (this._rawTitle) {
      combinedText = "<b>" + this._rawTitle + "</b>";
    }
    if (this._rawSubtitle && this._rawTitle) {
      combinedText += "<br><span style='color: gray; font-size:0.8em'>" + this._rawSubtitle + "</span>";
    }

    this._data[0].title.text = combinedText;

    if (this.autoMargins) {
      const exactTextWidth = this.calculateExactTextWidth(combinedText);

      this._layout.margin.l = exactTextWidth + this.leftMargin;
    }
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
      this.updatePlot();
    }
  }

  @Input()
  public set subtitle(subtitle: string) {
    if (subtitle != null) {
      this._rawSubtitle = subtitle;
      this.updateTitleText();
      this.updatePlot();
    }
  }

  public setRange(range: number[]) {
    this._data[0].gauge.axis.range = range;
    this.updatePlot();
  }

  @Input()
  public set backgroundColor(backgroundColor: string) {
    this._data[0].gauge.backgroundColor = backgroundColor != null ? backgroundColor : this.defaultBackgroundColor;
    this.updatePlot();
  }

  public setSteps(steps: number[]) {
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

  // TODO can this ever be used? Is not part of GUI-DSL Model
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
    } else {
      this.autoMargins = true;
      this._layout.margin = { t: this.topMargin, b: this.bottomMargin, l: this.leftMargin, r: this.rightMargin };
      // Recalculate dynamic left margin based on the current title
      this.updateTitleText();
    }
    this.updatePlot();
  }
}
