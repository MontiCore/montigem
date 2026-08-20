/* (c) https://github.com/MontiCore/monticore */

import { Component, ChangeDetectionStrategy,Input, SimpleChanges } from '@angular/core';
import { config, GemHeatmapChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemHeatmapChartComponent';
import { NgxChartsModule, ScaleType } from "@swimlane/ngx-charts";
import { GemChartOptions } from './GemChartOptions';
import { GemHeatmapChartData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemheatmapcharttypes/GemHeatmapChartData';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, GemChartOptions, NgxChartsModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemHeatmapChart extends GemHeatmapChartComponent {

  public _data: GemHeatmapChartData;
  public _xLabel: string;
  public _yLabel: string;
  public _xBins: number;
  public _yBins: number;

  @Input() defaultColor?: string = "#303C46";
  @Input() defaultXBins?: number = 6;
  @Input() defaultYBins?: number = 6;


  /**
   * Updates internal parameters based on Inputs
   */
  ngOnChanges(changes: SimpleChanges) {
    for (const propName in changes) {
      if (changes.hasOwnProperty(propName)) {
        switch (propName) {
          case 'data': {
            if (this.data) this.rearrange_array(this.data.getEntriesList()).then(arr => this.computed_data = arr);
            break;
          }
          case 'xBins': {
            if (this.xBins) this._xBins = this.xBins;
            if (this.data) this.rearrange_array(this.data.getEntriesList()).then(arr => this.computed_data = arr);
            break;
          }
          case 'yBins': {
            if (this.yBins) this._yBins = this.yBins;
            if (this.data) this.rearrange_array(this.data.getEntriesList()).then(arr => this.computed_data = arr);
            break;
          }
          case 'defaultColor': {
            if (/^#[0-9A-F]{6}$/i.test(this.defaultColor)) this.enter_color(this.defaultColor);
            break;
          }
        }
      }
    }
  }



  /**
   * @param min_num_seperations The lower bound for the number of columns in the chart
   */
  private min_num_seperations = 1;
  /**
   * @param max_num_seperations The upper bound for the number of columns in the chart
   */
  private max_num_seperations = 32;

  /**
   * @param computed_data Data used as input for [NGX-Charts Heatmap]{@link https://swimlane.gitbook.io/ngx-charts/v/docs-test/examples/heat-map-chart}
   */
  computed_data;

  /**
   * Sets some Input values for [NGX-Charts Heatmap]{@link https://swimlane.gitbook.io/ngx-charts/v/docs-test/examples/heat-map-chart}
   */
  showXAxis = true;
  showYAxis = true;
  gradient = false;
  displayLegend = true;
  showXAxisLabel = true;
  showYAxisLabel = true;




  /**
   * @param colorScheme The default color-scheme
   */
  colorScheme = {
    name: 'default',
    selectable: true,
    group: ScaleType.Linear,
    domain: ['#A3CAEC', '#98BCDB', '#8CAECB', '#819FBA', '#7591AA', '#6A8399', '#5E7588', '#536778', '#536778', '#3C4A57', '#303C46']
  };
  /**
   * @param colorscalefactor The secondary color is calculated as being colorscalefactor away from white/black when starting at the selected color
   */
  colorscalefactor = 0.3;
  /**
   * @param color_domain_mid_steps How many mid-steps there should in the colorScheme between selected and secondary color.
   */
  color_domain_mid_steps = 10;

  /**
   * Creates a new @param colorScheme based on @param value
   * @param value A Hex Color String
   */
  enter_color(value: string) {
    var color = parseInt(value.substr(1), 16);
    var blue = (color % 256);
    var green = ((color - blue) / 256) % 256;
    var red = (color - blue - (256 * green)) / 65536;

    if ((blue + green + red) > 381) {
      var secondary_red = red * this.colorscalefactor;
      var secondary_green = green * this.colorscalefactor;
      var secondary_blue = blue * this.colorscalefactor;
    }
    else {
      var secondary_red = 255 - (255 - red) * this.colorscalefactor;
      var secondary_green = 255 - (255 - green) * this.colorscalefactor;
      var secondary_blue = 255 - (255 - blue) * this.colorscalefactor;
    }

    var new_domain = [];
    for (var i = 0; i < this.color_domain_mid_steps; i++) {
      new_domain.push(this.color_to_html_hex(((this.color_domain_mid_steps - i) * secondary_red + i * red) / this.color_domain_mid_steps, ((this.color_domain_mid_steps - i) * secondary_green + i * green) / this.color_domain_mid_steps, ((this.color_domain_mid_steps - i) * secondary_blue + i * blue) / this.color_domain_mid_steps));
    }
    new_domain.push(value);
    new_domain.push(value);
    this.colorScheme = {
      name: 'default',
      selectable: true,
      group: ScaleType.Linear,
      domain: new_domain
    };
  }

  /**
   * @param redv Red value (0-255)
   * @param greenv Green value (0-255)
   * @param bluev  Blue value (0-255)
   * @returns Hex Color String from the given color values
   */
  color_to_html_hex(redv, greenv, bluev) {
    return ('#' + ('0' + Math.floor(redv).toString(16)).slice(-2) + ('0' + Math.floor(greenv).toString(16)).slice(-2) + ('0' + Math.floor(bluev).toString(16)).slice(-2));
  }




  /**
   * Updates internal parameters @param _xBins and  @param computed_data based on User-Inputs
   */
  enter_x_bins(value: string | number) {
    if (typeof value === "number") {
      if (value < this.min_num_seperations) { this._xBins = this.min_num_seperations; }
      else if (value > this.max_num_seperations) { this._xBins = this.max_num_seperations; }
      else this._xBins = value;
    } else { this._xBins = +value; }
    if (this.data) this.rearrange_array(this.data.getEntriesList()).then(arr => this.computed_data = arr);
  }

  /**
   * Updates internal parameters @param _yBins and  @param computed_data based on User-Inputs
   */
  enter_y_bins(value: string | number) {
    if (typeof value === "number") {
      if (value < this.min_num_seperations) { this._yBins = this.min_num_seperations; }
      else if (value > this.max_num_seperations) { this._yBins = this.max_num_seperations; }
      else this._yBins = value;
    } else { this._yBins = +value; }
    if (this.data) this.rearrange_array(this.data.getEntriesList()).then(arr => this.computed_data = arr);
  }


  /**
   * @param somedata A list of entries.
   * @returns a matrix of entries.
   */
  async rearrange_array(somedata) : Promise<Array<any>>{
    if (!somedata || (await somedata.size()) === 0) return [];

    let minX = (await somedata.get(0)).getXValue();
    let maxX = (await somedata.get(0)).getXValue();
    let minY = (await somedata.get(0)).getYValue();
    let maxY = (await somedata.get(0)).getYValue();

    for await (const entry of somedata) {
      if (entry.getXValue() < minX) minX = entry.getXValue();
      if (entry.getXValue() > maxX) maxX = entry.getXValue();
      if (entry.getYValue() < minY) minY = entry.getYValue();
      if (entry.getYValue() > maxY) maxY = entry.getYValue();
    }

    if (minX === maxX) maxX += 1;
    if (minY === maxY) maxY += 1;

    const xBins = this._xBins || this.xBins || this.defaultXBins || 6;
    const yBins = this._yBins || this.yBins || this.defaultYBins || 6;

    const stepX = (maxX - minX) / xBins;
    const stepY = (maxY - minY) / yBins;

    const grid = new Array(xBins);
    for (let i = 0; i < xBins; i++) {
      grid[i] = new Array(yBins).fill(0);
    }

    for await(const entry of somedata) {
      let xIdx = Math.floor((entry.getXValue() - minX) / stepX);
      let yIdx = Math.floor((entry.getYValue() - minY) / stepY);
      if (xIdx >= xBins) xIdx = xBins - 1;
      if (yIdx >= yBins) yIdx = yBins - 1;
      if (xIdx < 0) xIdx = 0;
      if (yIdx < 0) yIdx = 0;
      grid[xIdx][yIdx]++;
    }

    const computed_d = [];
    for (let i = 0; i < xBins; i++) {
      const xLabel = Math.floor(minX + i * stepX) + "-" + Math.floor(minX + (i + 1) * stepX);
      const series = [];
      for (let j = 0; j < yBins; j++) {
        const yLabel = Math.floor(minY + j * stepY) + "-" + Math.floor(minY + (j + 1) * stepY);
        series.push({ name: yLabel, value: grid[i][j] });
      }
      computed_d.push({ name: xLabel, series: series });
    }

    return computed_d;
  };


  public ngOnInit(): void { }
}
