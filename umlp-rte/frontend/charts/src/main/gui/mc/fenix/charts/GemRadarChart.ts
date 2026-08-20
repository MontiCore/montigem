/* (c) https://github.com/MontiCore/monticore */

import { Component,ChangeDetectionStrategy, Input, HostBinding } from '@angular/core';
import { config, GemRadarChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemRadarChartComponent';
import { ChartEvent, ChartOptions } from "chart.js";
import { NgChartsModule } from 'ng2-charts';
import { GemRadarChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemradarcharttypes/GemRadarChartData';
import { GemRadarChartDataSet } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemradarcharttypes/GemRadarChartDataSet';
import { autoColors } from './colors';
import { GemChartLegend } from './GemChartLegend';
import { GemChartOptions } from './GemChartOptions';
import {GemListUtil} from "@umlp/commonj2ts";

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, NgChartsModule, GemChartOptions, GemChartLegend],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemRadarChart extends GemRadarChartComponent {

  @HostBinding('style.display') display = 'block';
  @HostBinding('style.width') get hostWidth() { return this.width; }
  @HostBinding('style.height') get hostHeight() { return this.height; }

  public _dataSets: any; // {data: number[], label: string}
  public _labels: (string | string[])[];

  public _maxLabelLength: number = 20;
  public _opacity: number = 0.4; // Default opacity
  private _rawData: GemRadarChartData; // Store raw data to re-process on opacity change

  public options: ChartOptions<'radar'> = {
    responsive: true,
    maintainAspectRatio: false,
    elements: {
      line: {
        borderWidth: 3
      }
    },
    plugins: {
      legend: {
        display: false
      }
    },
    scales: {
      r: {
        ticks: {
          display: true // Default show labels
        },
        pointLabels: {
          display: true // Default show point labels
        }
      }
    }
  };

  @Input()
  public set data(data: GemRadarChartData) {
    if (!data) {
      return;
    }
    this._rawData = data;
    this.processData();
  }

  private processData() {
    if (!this._rawData) return;

    let autocolors = this.getColors();
    this.legendLabels = [];
    this.legendColors = [];
    console.warn("Process Data");
    (async () => {
      this._dataSets = (await GemListUtil.toArraySync(this._rawData.getDataSetsList())).map((ds: GemRadarChartDataSet, index: number) => {
        let label = ds.getLabel();
        let backgroundColor = autoColors[index];
        if (ds.getBackgroundColor() !== null) {
          backgroundColor = ds.getBackgroundColor();
        }

        // Apply opacity if backgroundColor is a hex string or rgb string without alpha
        if (this._opacity !== undefined && this._opacity !== null) {
          backgroundColor = this.applyOpacity(backgroundColor, this._opacity);
        }


        let res = {
          data: ds.getDataList().__internalArray(),
          label: label,
          fill: ds.getFill(),
          backgroundColor: backgroundColor,
        }

        if (ds.getBorderColor() !== null) {
          res["borderColor"] = ds.getBorderColor();
        }
        if (ds.getBorderWidth() !== null) {
          res["borderWidth"] = ds.getBorderWidth();
        }

        this.legendLabels.push(label);
        this.legendColors.push(backgroundColor);
        return res;
      });
      const overall_labels = [];

      for await (const label of this._rawData.getLabelsList()) {
        // split the label into maximum 15 character long chunks
        let words = label.split(' ');
        let labels = [];
        let chunk = words[0];
        for (let i = 1; i < words.length; i++) {
          if ((chunk + ' ' + words[i]).length <= this._maxLabelLength) {
            chunk += ' ' + words[i];
          } else {
            labels.push(chunk);
            chunk = words[i];
          }
        }
        labels.push(chunk);
        overall_labels.push(labels);
      }
      this._labels = overall_labels;
      console.warn("Process Data finished");
    })();
  }

  @Input()
  public set borderWidth(boarderWidth: number) {
    this.options.elements.line.borderWidth = boarderWidth;
  }

  @Input()
  public set fill(fill: boolean) {
    this.options.elements.line.fill = fill;
  }

  @Input()
  public set min(min: number) {
    this.options.scales['r'].min = min;
  }

  @Input()
  public set max(max: number) {
    this.options.scales['r'].max = max;
  }

  @Input()
  public set stepSize(stepSize: number) {
    this.options.scales['r'].ticks.stepSize = stepSize;
  }

  @Input()
  public set maxLabelLength(maxLabelLength: number) {
    this._maxLabelLength = maxLabelLength;
  }

  @Input()
  public set showLegend(show: boolean) {
    this.displayLegend = show;
  }

  @Input()
  public set showLabels(show: boolean) {
    if (this.options.scales['r']) {
        this.options.scales['r'].ticks.display = show;
        this.options.scales['r'].pointLabels = { display: show };
        // Force chart update by creating a new options object reference
        this.options = { ...this.options };
    }
  }

  @Input()
  public set opacity(opacity: number) {
    this._opacity = opacity;
    this.processData();
  }

  private applyOpacity(color: string, opacity: number): string {
      if (color.startsWith('#')) {
          let r = parseInt(color.slice(1, 3), 16);
          let g = parseInt(color.slice(3, 5), 16);
          let b = parseInt(color.slice(5, 7), 16);
          return `rgba(${r}, ${g}, ${b}, ${opacity})`;
      } else if (color.startsWith('rgb(')) {
          return color.replace('rgb(', 'rgba(').replace(')', `, ${opacity})`);
      } else if (color.startsWith('rgba(')) {
          // Replace the alpha value
          return color.replace(/[^,]+(?=\))/, `${opacity}`);
      }
      return color;
  }


  chartHovered($event: { event: ChartEvent; active: {}[] }) {

  }

  chartClicked($event: { event?: ChartEvent; active?: {}[] }) {

  }

  // <------------------- Legend ------------------->

  /**gives the colors from the constant autoColors */
  public getColors() {
    return autoColors;
  }

  /**labels for chart legend */
  public legendLabels: string[] = [];

  /**colors for chart legend */
  public legendColors: string[] = [];

  /** defines the position of the legend */
  public legendPosition: string;

  /**variable that enables or disables the legend */
  public displayLegend = true;

  public setLegendPosition(legendPosition: string) {
    this.legendPosition = legendPosition;
  }

  /** changes container style depending on legend position */
  public changeContainer() {
    switch (this.legendPosition) {
      case 'left':
        return 'wholeChartWithLegendLeftContainer';
      case 'top':
        return 'wholeChartWithLegendTopContainer';
      case 'bottom':
        return 'wholeChartWithLegendBottomContainer';
      case 'right':
        return 'wholeChartWithLegendRightContainer';
      default:
        return 'wholeChartWithLegendBottomContainer';
    }
  }

  public ngOnInit(): void { }
}
