/* (c) https://github.com/MontiCore/monticore */

import { Component, ChangeDetectionStrategy,EventEmitter, Input, Output } from '@angular/core';
import { config, GemVennDiagramComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemVennDiagramComponent';
import * as d3 from 'd3';
import * as venn from 'venn.js';
import { GemVennDiagramData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemvenndiagramtypes/GemVennDiagramData';
import { autoColors } from './colors';
import { GemChartLegend } from './GemChartLegend';
import { GemChartOptions } from './GemChartOptions';
import {GemListUtil} from "@umlp/commonj2ts";

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, GemChartOptions, GemChartLegend],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemVennDiagram extends GemVennDiagramComponent {
  /**all labels of the entries in the chart*/
  public labels: string[];

  /**all color of the entries */
  public colors = [];

  /**contains the data for each entry */
  public _data: GemVennDiagramData;

  public _chart = venn.VennDiagram()

  /**labels for chart legend */
  public legendLabels: string[];

  /**colors for chart legend */
  public legendColors: string[] = [];

  /** defines the position of the legend */
  public legendPosition: string;

  /**variable that enables or disables the legend */
  public displayLegend = true;

  /**gives the colors from the constant autoColors */
  public getColors() {
    return autoColors;
  }

  @Input()
  public set data(data: GemVennDiagramData) {
    if (!data) {
      return;
    }
    (async () => {
      const entries = await GemListUtil.toArraySync(data.getEntriesList());
      this.labels = entries.map(entry => entry.getLabel());
      this._data = data;

      // only the sets themselves belong into the legend, not their intersections; the circles are
      // colored in the order of the single sets, so the legend colors follow the same order
      const legendLabels = [];
      for (const entry of entries) {
        const sets = await GemListUtil.toArraySync(entry.getSetsList());
        if (sets.length === 1) {
          legendLabels.push(entry.getLabel() || String(sets[0]));
        }
      }
      this.legendLabels = legendLabels;
      this.legendColors = legendLabels.map((_, i) => this.colorScheme.domain[i % this.colorScheme.domain.length] + "70");

      this.updateValues();
    })();
  }

  public colorScheme = {
    domain: this.getColors()
  };

  constructor() {
    super();
  }

  init() {
    this.colors = this.getColors();
    this.updateValues()
      .then(_ =>
        super.init()
      )
  }

  public get sets() {
    return this._data;
  }

  private getColor(index: number) {
    return this.colors[index % this.colors.length];
  }

  private async updateValues() {
    if(!this._data) {
      return;
    }
    let div = d3.select("#venn");
    const entries = [];
    for await (const e of this._data.getEntriesList()) {
      const sets = (await GemListUtil.toArraySync(e.getSetsList()))
      entries.push({
        sets: sets,
        label: e.getLabel(),
        size: e.getSize()
      })
    }
    let transformedData = {
      entries: entries
    };
    div.datum(transformedData.entries).call(this._chart);

    let tooltip = d3.select("body").append("div").attr("class", "tooltip");

    div.selectAll("g")
      .on("mouseover", (event, d) => {
        venn.sortAreas(div, d);

        tooltip.transition().duration(400).style("opacity", .9);
        tooltip.text(d.size + " publications");

        let selection = d3.select(event.currentTarget).transition("tooltip").duration(400);
        selection.select("path")
          .style("stroke-widht", 3)
          .style("fill-opacity", d.sets.length == 1 ? .4 : .1)
          .style("stroke-opacity", 1);
      })
      .on("mousemove", (event) => {
        tooltip.style("left", (event.pageX) + "px")
          .style("top", (event.pageY - 28) + "px");
      })
      .on("mouseout", (event, d) => {
        tooltip.transition().duration(400).style("opacity", 0);
        var selection = d3.select(event.currentTarget).transition("tooltip").duration(400);
        selection.select("path")
          .style("stroke-width", 0)
          .style("fill-opacity", d.sets.length == 1 ? .25 : .0)
          .style("stroke-opacity", 0);
      });
    d3.selectAll(".venn-circle path").style("fill", (event, d) => {
      return this.getColor(d);
    });
    d3.selectAll("#venn text").style("fill", (event, d) => {
      return this.getColor(d);
    });
    d3.selectAll("#venn .venn-circle").on("click", (event, d) => { console.log(d); this.emitClick(d); })
    d3.selectAll("#venn .venn-intersection").on("click", (event, d) => { console.log(d); this.emitClick(d); })
  }

  @Output() public onClick = new EventEmitter<string[]>();

  private emitClick(intersection: any) {
    this.onClick.emit(intersection.sets);
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
