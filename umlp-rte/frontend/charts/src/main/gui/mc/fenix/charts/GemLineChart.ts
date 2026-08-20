/* (c) https://github.com/MontiCore/monticore */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { config, GemLineChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemLineChartComponent';
import { NgChartsModule } from 'ng2-charts';
import { ChartDataset, ChartOptions } from 'chart.js';
import { GemLineChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemlinecharttypes/GemLineChartData';
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
export class GemLineChart extends GemLineChartComponent {

  /**labels for which will be shown data from the entries */
  public labels: string[];

  /**contains the data for each entry */
  public dataSets: ChartDataset<'line'>[] = [];

  /**contains the colors for each entry */
  public colors = [];

  /**says whether the line fill colors of the entries will be shown */
  public _enableBackgroundColor = false;

  /**the upper bound of the values that will be shown*/
  public _maxValue = undefined;

  /**the lower bound of the values that will be shown */
  public _minValue = undefined;

  /**can add symbols to the values of the entries */
  public _displayValue: (value: number) => string = (v) => String(v);

  /**labels for chart legend */
  public legendLabels: string[] = [];

  /**colors for chart legend */
  public legendColors: string[] = [];

  /** defines the position of the legend */
  public legendPosition: string;

  /**variable that enables or disables the legend */
  public displayLegend = true;

  /**contains all the settings for the chart */
  public options: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: true,
  };

  /**gives the colors from the constant autoColors */
  public getColors() {
    return autoColors;
  }

  @Input()
  public set displayValue(displayValue: (value: number) => string) {
    this._displayValue = displayValue;
    this.saveAllOptionsChanges();
  }

  @Input()
  public set enableBackgroundColor(enableBackgroundColor: boolean) {
    this._enableBackgroundColor = enableBackgroundColor;
    this.saveAllOptionsChanges();
  }

  /**setter that adjusts the data for the entries and sets colors for them */
  @Input()
  public set data(data: GemLineChartData) {
    if (!data) {
      return;
    }
    this.dataSets = [];
    const colors = [];
    const labels = [];
    (async () => {
      this.labels = await GemListUtil.toArraySync(data.getLabelsList());
      let autocolors = this.getColors();
      let pointerAutoColors = 0;

      for await(const lCEntry of data.getEntriesList()) {
        if (!CSS.supports('color', lCEntry.getBorderColor())) {
          lCEntry.setBorderColor(autocolors[pointerAutoColors++]);
        }
        // TODO: apply alpha value to color string support
        let entry: ChartDataset<'line'> = {
          data: (await GemListUtil.toArraySync(lCEntry.getDataList())),
          label: lCEntry.getLabel(),
          borderColor: lCEntry.getBorderColor(),
          backgroundColor: lCEntry.getBackgroundColor() + "30",
          pointBackgroundColor: lCEntry.getPointBackgroundColor()
        }

        // TODO: option to fill background
        entry.fill = false;
        labels.push(entry.label);
        colors.push(entry.borderColor);
        this.dataSets.push(entry);
      }
      this.legendLabels = labels;
      this.legendColors = colors;
    })();
  }

  @Input()
  public set maxValue(maxValue: number) {
    this._maxValue = maxValue;
    this.saveAllOptionsChanges();
  }

  @Input()
  public set minValue(minValue: number) {
    this._minValue = minValue;
    this.saveAllOptionsChanges();
  }

  ngOnInit(): void {
    this.saveAllOptionsChanges();
  }

  /**
   * The function saves the data for all variables with settings for the variable options
   * that are not reset in the specific setter
   */
  public saveAllOptionsChanges() {
    this.options = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          grid: {
            color: '#888'
          },
          ticks: {
            color: '#888'
          }
        },
        y: {
          max: this._maxValue,
          min: this._minValue,
          grid: {
            color: '#888'
          },
          ticks: {
            color: '#888'
          }
        }
      },
      elements: {
        line: {
          fill: this._enableBackgroundColor,
        }
      },
      plugins: {
        tooltip: {
          callbacks: {
            label: (tooltipItem) => {
              const label = tooltipItem.dataset.label || "";
              let value = tooltipItem.parsed.y;
              let res = "";
              if (this._displayValue != undefined && typeof value === 'number') {
                res = this._displayValue(value);
              }
              return label + ": " + res;
            },
          }
        },
        legend: {
          display: false
        }
      }
    }
  }

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

}
