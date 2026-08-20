/* (c) https://github.com/MontiCore/monticore */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { config, GemScatterPlotComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemScatterPlotComponent';
import { ChartDataset, ChartOptions, TooltipItem } from 'chart.js';
import { NgChartsModule } from 'ng2-charts';
import { GemScatterPlotAxis } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemscatterplottypes/GemScatterPlotAxis';
import { GemScatterPlotData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemscatterplottypes/GemScatterPlotData';
import { GemScatterPlotShape } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemscatterplottypes/GemScatterPlotShape';
import { autoColors } from './colors';
import { GemChartLegend } from './GemChartLegend';
import { GemChartOptions } from './GemChartOptions';

interface CustomTooltipItemPointWithLabel extends TooltipItem<'scatter'> {
  raw: {
    x: number,
    y: number,
    label: string,
  }
}

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, NgChartsModule, GemChartOptions, GemChartLegend],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemScatterPlot extends GemScatterPlotComponent {

  /** contains the data for all entries that will be displayed in the chart*/
  public dataSets: ChartDataset<'scatter'>[] = [];

  //stuff that has to be used when changing the options
  public _xAxis: GemScatterPlotAxis = new GemScatterPlotAxis();
  public _yAxis: GemScatterPlotAxis = new GemScatterPlotAxis();

  /**colors for chart legend */
  public legendColors: string[] = [];

  /** defines the position of the legend */
  public legendPosition: string;

  /**variable that enables or disables the legend */
  public displayLegend = true;

  /** variable that contains the settings for the chart */
  //will be overridden on init
  public options: ChartOptions = {
    responsive: true,
    maintainAspectRatio: true,
  };

  @Input()
  public showConfig: boolean = true;

  /**gives the colors from the constant autoColors */
  public getColors() {
    return autoColors;
  }

  /** setter that processes the data to be shown correctly by the chart and the legend.
   * Also sets the colors for each entry when no color is given as it uses the provided colors service
   */
  @Input()
  public set data(data: GemScatterPlotData) {
    if (!data) {
      return;
    }
    (async () => {
      this.dataSets = [];
      const colors = [];
      const labels = [];
      let pointerAutoColors = 0;
      let autocolors = this.getColors();

      for await(const set of data.getSetsList()) {
        if (!CSS.supports('color', set.getColor())) {
          set.setColor(autocolors[pointerAutoColors++]);
        }
        if (!CSS.supports('color', set.getHoverColor())) {
          set.setHoverColor(autocolors[pointerAutoColors++]);
        }

        const points = [];
        const pointRadiusArr = [];
        set.getPointsList().forEach((point) => {
          points.push({
            x: point.getX(),
            y: point.getY(),
            label: point.getLabel()
          });
          if (!point.getPointRadius()) {
            //default radius
            pointRadiusArr.push(3);
          } else {
            pointRadiusArr.push(point.getPointRadius());
          }
        });


        let entry: ChartDataset<'scatter'> = {
          label: set.getLabel(),
          pointStyle: this.getPointStyle(set.getShape()),
          data: points,
          backgroundColor: set.getColor(),
          hoverBackgroundColor: set.getHoverColor(),
          hoverBorderWidth: 2,
          pointRadius: pointRadiusArr
        };

        this.dataSets.push(entry);
      }
      this.legendColors = colors;
    })()

  }

  public getPointStyle(shape?: GemScatterPlotShape): string {
    //https://www.chartjs.org/docs/latest/configuration/elements.html#point-styles
    let name: string = "circle";
    switch (shape) {
      case GemScatterPlotShape.SQUARE:
        name = "rect";
        break;
      case GemScatterPlotShape.ROUNDED_SQUARE:
        name = "rectRounded";
        break;
      case GemScatterPlotShape.ROTATED_SQUARE:
        name = "rectRot";
        break;
      case GemScatterPlotShape.CROSS:
        name = "cross";
        break;
      case GemScatterPlotShape.ROTATED_CROSS:
        name = "crossRot";
        break;
      case GemScatterPlotShape.DASH:
        name = "dash";
        break;
      case GemScatterPlotShape.LINE:
        name = "line";
        break;
      case GemScatterPlotShape.STAR:
        name = "star";
        break;
      case GemScatterPlotShape.TRIANGLE:
        name = "triangle";
        break;
    }
    return name;
  }

  @Input()
  public set xAxis(axis: GemScatterPlotAxis) {
    this._xAxis = axis;
    this.saveAllOptionsChanges();
  }

  @Input()
  public set yAxis(axis: GemScatterPlotAxis) {
    this._yAxis = axis;
    this.saveAllOptionsChanges();
  }

  constructor() {
    super();
  }

  ngOnInit(): void {
    this.saveAllOptionsChanges();
  }

  /** function that saves all settings of the variables that are not reset in the new input */
  public saveAllOptionsChanges(): void {
    this.options = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          title: {
            display: this._xAxis?.getLabel() !== null,
            text: this._xAxis?.getLabel(),
          },
          min: this._xAxis?.getMinValue(),
          max: this._xAxis?.getMaxValue(),
          grid: {
            color: '#888',
          },
          ticks: {
            color: '#888',
            stepSize: this._xAxis?.getStepWidth(),
          }
        },
        y: {
          title: {
            display: this._yAxis?.getLabel() !== null,
            text: this._yAxis?.getLabel(),
          },
          min: this._yAxis?.getMinValue(),
          max: this._yAxis?.getMaxValue(),
          grid: {
            color: '#888',
          },
          ticks: {
            color: '#888',
            stepSize: this._yAxis?.getStepWidth(),
          }
        }
      },
      plugins: {
        tooltip: {
          callbacks: {
            label: function (context) {
              let newContext = context as CustomTooltipItemPointWithLabel;
              let label = newContext.raw.label;
              return "coordinates: (" + newContext.raw.x + ", " + newContext.raw.y + ")" + ", label: " + label;
            }
          }
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
