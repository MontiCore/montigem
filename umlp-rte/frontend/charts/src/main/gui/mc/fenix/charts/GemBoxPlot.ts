/* (c) https://github.com/MontiCore/monticore */

import { Component, ChangeDetectionStrategy, ElementRef, Input } from '@angular/core';
import { config, GemBoxPlotComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemBoxPlotComponent';
import * as d3 from 'd3';
import { NgChartsModule } from 'ng2-charts';
import { GemBoxPlotEntry } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemboxplottypes/GemBoxPlotEntry';
import { autoColors } from './colors';
import { GemChartLegend } from './GemChartLegend';
import { GemChartOptions } from './GemChartOptions';
import { GemBoxPlotData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemboxplottypes/GemBoxPlotData';
import {GemListUtil} from "@umlp/commonj2ts";

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, NgChartsModule, GemChartOptions, GemChartLegend],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemBoxPlot extends GemBoxPlotComponent {

  /**all color of the entries */
  public colors = [];

  /**contains the data entries */
  public _entries: GemBoxPlotEntry[];

  /** Display data points randomized in box width */
  public _showDataPoints: boolean = true;

  /** given general minimum */
  public _minimum: number;

  /** given general maximum */
  public _maximum: number;

  /** SVG Width */
  public _width: number = 400;

  /** SVG Width */
  public _height: number = 200;

  /** Horizontal Boxse */
  public _horizontal: boolean = true;

  /**labels for chart legend */
  public legendLabels: string[] = [];

  /**colors for chart legend */
  public legendColors: string[] = [];

  /** defines the position of the legend */
  public legendPosition: string;

  /**variable that enables or disables the legend */
  public displayLegend = true;

  public colorScheme = {
    domain: this.getColors()
  };

  /**gives the colors from the constant autoColors */
  public getColors() {
    return autoColors;
  }

  @Input()
  public set showDataPoints(showDataPoints: boolean) {
    this._showDataPoints = showDataPoints
    this.updateValues()
  }

  @Input()
  public set min(minimum: number) {
    this._minimum = minimum
    this.updateValues()
  }
  @Input()
  public set max(maximum: number) {
    this._maximum = maximum
    this.updateValues()
  }

  @Input()
  public set width(width: number) {
    // Ensure width is a valid number, fallback to default 400
    this._width = width != null ? width : 400;
    this.updateValues();
  }

  @Input()
  public set height(height: number) {
    // Ensure height is a valid number, fallback to default 200
    this._height = height != null ? height : 200;
    this.updateValues();
  }

  @Input()
  public set horizontal(horizontal: boolean) {
    this._horizontal = horizontal
    this.updateValues()
  }

  @Input()
  public set data(data: GemBoxPlotData) {
    if (!data || !data.getEntriesList()) {
      return;
    }
    GemListUtil.toArraySync(data.getEntriesList()).then(arr => {
      const bgrColor = this.colorScheme.domain.map(x => x + "70");
      this._entries = arr;

      this.legendColors = bgrColor;

      this.updateValues();
    })
  }

  constructor(private componentRef: ElementRef) {
    super();
  }

  public get sets() {
    return this._entries;
  }

  private updateValues() {
    if (!this._entries) {
      return
    }

    const element = this.componentRef.nativeElement
    let div = d3.select(element).select("#boxplot");

    const margin = { top: 10, right: 30, bottom: 30, left: 40 };
    const width = this._width;
    const height = this._height;

    // Remove svg in div to avoid double drawing when an attribute is set
    div.select("svg").remove()

    const outerSvg = div.append("svg")
      .attr("width", width + margin.left + margin.right)
      .attr("height", (height + margin.top + margin.bottom))
    const svg = outerSvg.append("g")

    svg.attr("transform", "translate(" + margin.left + "," + (margin.top) + ")")


    const boxes = this._entries.map(e => {
      const interQuantileRange = e.getBox().getUpperQuantile() - e.getBox().getLowerQuantile()
      const boxColor = e.getColor() ? e.getColor() : "#FFFFFF";
      return ({
        key: e.getGroup(), boxColor: boxColor, value:
          { lowerQuantile: e.getBox().getLowerQuantile(), median: e.getBox().getMedian(), upperQuantile: e.getBox().getUpperQuantile(), interQuantileRange: interQuantileRange, min: e.getBox().getLowerExtreme(), max: e.getBox().getUpperExtreme() }
      })
    })

    if (this._horizontal) {
      this.drawHorizontalBoxDiagram(svg, boxes, margin, width, height)
    } else {
      this.drawVerticalBoxDiagram(svg, boxes, margin, width, height)
    }
  }

  public drawVerticalBoxDiagram(svg, boxes, margin, width, height) {
    // Show the X scale
    var ordinalAxis = d3.scaleBand()
      .range([margin.left, width + margin.left])
      .domain(this._entries.map(e => e.getGroup()))
      .paddingInner(1)
      .paddingOuter(.5)

    const generalMinimum = this._minimum ? this._minimum : Math.min(...boxes.map(e => e.value.min))
    const generalMaximum = this._maximum ? this._maximum : Math.max(...boxes.map(e => e.value.max))

    // Show the Y scale
    var scalarAxis = d3.scaleLinear()
      .domain([generalMinimum, generalMaximum])
      .range([height, 0]);

    svg.append("g")
      .attr("transform", "translate(0," + (height) + ")")
      .call(d3.axisBottom(ordinalAxis));

    svg.append("g")
      .attr("transform", "translate(" + margin.left + ",0)")
      .call(d3.axisLeft(scalarAxis))

    // a few features for the box
    var boxWidth = 100

    // Show the main vertical line
    svg
      .selectAll("vertLines")
      .data(boxes)
      .enter()
      .append("line")
      .attr("x1", (d) => { return (ordinalAxis(d.key) || 0) })
      .attr("x2", (d) => { return (ordinalAxis(d.key) || 0) })
      .attr("y1", (d) => { return (scalarAxis(d.value.min)) })
      .attr("y2", (d) => { return (scalarAxis(d.value.max)) })
      .attr("stroke", "black")
      .style("width", 40)

    // Show the boxes
    svg.selectAll("boxes")
      .data(boxes)
      .enter()
      .append("rect")
      .attr("x", (d) => { return ((ordinalAxis(d.key) || 0) - boxWidth / 2) })
      .attr("y", (d) => { return (scalarAxis(d.value.upperQuantile)) })
      .attr("height", (d) => { return (scalarAxis(d.value.lowerQuantile) - scalarAxis(d.value.upperQuantile)) })
      .attr("width", boxWidth)
      .attr("stroke", "black")
      .style("fill", (d) => { return d.boxColor })

    // show median, min and max horizontal lines
    svg.selectAll("medianLines")
      .data(boxes)
      .enter()
      .append("line")
      .attr("x1", (d) => { return ((ordinalAxis(d.key) || 0) - boxWidth / 2) })
      .attr("x2", (d) => { return ((ordinalAxis(d.key) || 0) + boxWidth / 2) })
      .attr("y1", (d) => { return (scalarAxis(d.value.median)) })
      .attr("y2", (d) => { return (scalarAxis(d.value.median)) })
      .attr("stroke", "black")
      .style("width", 80)

    svg.selectAll("minLines")
      .data(boxes)
      .enter()
      .append("line")
      .attr("x1", (d) => { return ((ordinalAxis(d.key) || 0) - boxWidth / 4) })
      .attr("x2", (d) => { return ((ordinalAxis(d.key) || 0) + boxWidth / 4) })
      .attr("y1", (d) => { return (scalarAxis(d.value.min)) })
      .attr("y2", (d) => { return (scalarAxis(d.value.min)) })
      .attr("stroke", "black")
      .style("width", 80)

    svg.selectAll("maxLines")
      .data(boxes)
      .enter()
      .append("line")
      .attr("x1", (d) => { return ((ordinalAxis(d.key) || 0) - boxWidth / 4) })
      .attr("x2", (d) => { return ((ordinalAxis(d.key) || 0) + boxWidth / 4) })
      .attr("y1", (d) => { return (scalarAxis(d.value.max)) })
      .attr("y2", (d) => { return (scalarAxis(d.value.max)) })
      .attr("stroke", "black")
      .style("width", 80)

    // Add individual points "jitter"
    var jitterWidth = boxWidth * 2 / 3
    if (this.showDataPoints) {
      (async () => {
        const points: [string, number][] = [];

        for (const entry of this._entries) {
          const group = entry.getGroup();

          for await (const value of entry.getPointsList()) {
            points.push([group, value]);
          }
        }

        svg
          .selectAll("indPoints")
          .data(points)
          .enter()
          .append("circle")
          .attr("cx", d =>
            (ordinalAxis(d[0]) || 0) - jitterWidth / 2 + Math.random() * jitterWidth
          )
          .attr("cy", d => scalarAxis(d[1]))
          .attr("r", 4)
          .style("fill", "white")
          .attr("stroke", "black");
      })().catch(error => {
        console.error("Failed to load box plot points:", error);
      });
    }
  }

  public async drawHorizontalBoxDiagram(svg, boxes, margin, width, height) {
    // Show the X scale
    var ordinalAxis = d3.scaleBand()
      .range([margin.top, height + margin.top])
      .domain(this._entries.map(e => e.getGroup()))
      .paddingInner(1)
      .paddingOuter(.5)

    const generalMinimum = this._minimum ? this._minimum : Math.min(...boxes.map(e => e.value.min))
    const generalMaximum = this._maximum ? this._maximum : Math.max(...boxes.map(e => e.value.max))

    // Show the Y scale
    var scalarAxis = d3.scaleLinear()
      .domain([generalMinimum, generalMaximum])
      .range([0, width]);

    svg.append("g")
      .attr("transform", "translate(" + margin.left + "," + (0) + ")")
      .call(d3.axisLeft(ordinalAxis));

    svg.append("g")
      .attr("transform", "translate(" + margin.left + "," + (height + margin.top) + ")")
      .call(d3.axisBottom(scalarAxis))

    // a few features for the box
    var boxWidth = 100

    // Show the main lines
    svg
      .selectAll("mainLines")
      .data(boxes)
      .enter()
      .append("line")
      .attr("x1", (d) => { return (scalarAxis(d.value.min)) })
      .attr("x2", (d) => { return (scalarAxis(d.value.max)) })
      .attr("y1", (d) => { return (ordinalAxis(d.key) || 0) })
      .attr("y2", (d) => { return (ordinalAxis(d.key) || 0) })
      .attr("stroke", "black")
      .style("width", 40)

    // Show the boxes
    svg.selectAll("boxes")
      .data(boxes)
      .enter()
      .append("rect")
      .attr("x", (d) => { return (scalarAxis(d.value.lowerQuantile)) })
      .attr("y", (d) => { return ((ordinalAxis(d.key) || 0) - boxWidth / 2) })
      .attr("width", (d) => { return (scalarAxis(d.value.upperQuantile) - scalarAxis(d.value.lowerQuantile)) })
      .attr("height", boxWidth)
      .attr("stroke", "black")
      .style("fill", (d) => { return d.boxColor })

    // show median, min and max horizontal lines
    svg.selectAll("medianLines")
      .data(boxes)
      .enter()
      .append("line")
      .attr("x1", (d) => { return (scalarAxis(d.value.median)) })
      .attr("x2", (d) => { return (scalarAxis(d.value.median)) })
      .attr("y1", (d) => { return ((ordinalAxis(d.key) || 0) - boxWidth / 2) })
      .attr("y2", (d) => { return ((ordinalAxis(d.key) || 0) + boxWidth / 2) })
      .attr("stroke", "black")
      .style("width", 80)

    svg.selectAll("minLines")
      .data(boxes)
      .enter()
      .append("line")
      .attr("x1", (d) => { return (scalarAxis(d.value.min)) })
      .attr("x2", (d) => { return (scalarAxis(d.value.min)) })
      .attr("y1", (d) => { return ((ordinalAxis(d.key) || 0) - boxWidth / 4) })
      .attr("y2", (d) => { return ((ordinalAxis(d.key) || 0) + boxWidth / 4) })
      .attr("stroke", "black")
      .style("width", 80)

    svg.selectAll("maxLines")
      .data(boxes)
      .enter()
      .append("line")
      .attr("x1", (d) => { return (scalarAxis(d.value.max)) })
      .attr("x2", (d) => { return (scalarAxis(d.value.max)) })
      .attr("y1", (d) => { return ((ordinalAxis(d.key) || 0) + boxWidth / 4) })
      .attr("y2", (d) => { return ((ordinalAxis(d.key) || 0) - boxWidth / 4) })
      .attr("stroke", "black")
      .style("width", 80)

    // Add individual points "jitter"
    var jitterWidth = boxWidth * 2 / 3
    if (this._showDataPoints) {
      const pointData: [string, number][] = [];

      for (const currentValue of this._entries) {
        for await (const v of currentValue.getPointsList()) {
          pointData.push([currentValue.getGroup(), v]);
        }
      }

      svg
        .selectAll("indPoints")
        .data(pointData)
        .enter()
        .append("circle")
        .attr("cx", (d) => scalarAxis(d[1]))
        .attr(
          "cy",
          (d) =>
            (ordinalAxis(d[0]) || 0) -
            jitterWidth / 2 +
            Math.random() * jitterWidth
        )
        .attr("r", 4)
        .style("fill", "white")
        .attr("stroke", "black");
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
        return 'wholeChartWithLegendRightContainer';
    }
  }

}
