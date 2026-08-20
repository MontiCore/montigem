/* (c) https://github.com/MontiCore/monticore */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { config, GemNetworkGraphComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemNetworkGraphComponent';
import { GemNetworkData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemnetworkgraphtypes/GemNetworkData';
import { GemNetworkDataBuilder } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemnetworkgraphtypes/GemNetworkDataBuilder';
import { GemNetworkNodeBuilder } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemnetworkgraphtypes/GemNetworkNodeBuilder';
import { Observable } from 'rxjs';
import { DataSet, DataView, Network, Options } from 'vis-network/standalone';
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
export class GemNetworkGraph extends GemNetworkGraphComponent {
  /**contains the data for each entry */
  public _data: GemNetworkData = new GemNetworkDataBuilder();

  /**labels for chart legend */
  public legendLabels: string[];

  /**colors for chart legend */
  public legendColors: string[] = [];

  /** defines the position of the legend */
  public legendPosition: string;

  /**variable that enables or disables the legend */
  public displayLegend = true;

  private _network = null;
  private nodesView;
  public _nodes;

  private _labelEqualsID = false;
  private _filter;

  constructor() {
    super();
  }

  @Input()
  public set labelEqualsID(input: boolean) {
    this._labelEqualsID = input;
  }

  @Input()
  public set data(data: GemNetworkData) {
    if (!data) {
      return;
    }
    this._data.setEdgesList(data.getEdgesList());
    this._nodes = new DataSet();

    (async () => {
    if (!this._labelEqualsID) {
      const newNodes = [];
      for await (let n of data.getNodesList()) {
        newNodes.push({
          id: n.getId(),
          value: n.getValue(),
          label: n.getLabel(),
          mass: n.getMass(),
          color: n.getColor(),
          group: n.getGroup(),
          year: n.getYear()
        })
      }
      this._nodes.add(newNodes);
    } else {
      for await (let elem of data.getNodesList()) {
        if (typeof elem.getId() === "string") {
          elem.setLabel(elem.getId());
        }
        this._nodes.add({
          id: elem.getId(),
          value: elem.getValue(),
          label: elem.getLabel(),
          mass: elem.getMass(),
          color: elem.getColor(),
          group: elem.getGroup(),
          year: elem.getYear()
        });
      }
    }

    let nodeColors = (await GemListUtil.toArraySync(data.getNodesList())).map(entry => entry.getColor());
    const bgrColor = [...new Set(nodeColors)];

    this.legendColors = bgrColor;
    this.legendLabels = ["acylic", "cylic"]

    this.prepareData();
    await this.redrawGraph();
    })();
  }

  @Input()
  public set filter(func: any) {
    if (typeof func === "function") {
      this._filter = func;
      this.prepareData();
      this.redrawGraph();
    }
  }

  @Input()
  public set improvedLayout(value: boolean) {
    if (!this._options.layout) {
      this._options.layout = {};
    }
    this._options.layout.improvedLayout = value;
    this.redrawGraph();
  }

  @Input() filterEvents: Observable<void>;

  @Input()
  public set options(options: Options) {
    if (!!options) {
      this._options = options;
      this.redrawGraph();
    }
  }

  private _options: Options = {
    nodes: {
      shape: "circle",
      size: 30,
      font: {
        size: 32,
        color: "#ffffff",
      },
      borderWidth: 2,
    },
    edges: {
      width: 2,
    }
  }

  private prepareData() {
    if (!!this._nodes && this._nodes.length != 0) {
      if (this._filter) {
        this.nodesView = new DataView(this._nodes, { filter: this._filter });
        this.filterEvents.subscribe(() => {
          this.nodesView.refresh();
        });
        this._data.setNodesList(this.nodesView);
      } else {
        this._data.setNodesList(this._nodes.map(n => {
            const builder = new GemNetworkNodeBuilder();
            builder.realBuilder = builder;
            return builder
              .id(n.id)
              .year(n.year)
              .value(n.value)
              .label(n.label)
              .mass(n.mass)
              .color(n.color)
              .group(n.group)
              .year(n.year)
              .build();
          }
        ));
      }
    }
  }

  private async redrawGraph() {
    const container = document.getElementById("network");
    const nodes = (await GemListUtil.toArraySync(this._data.getNodesList())).map(n => ({
      id: n.getId(),
      value: n.getValue(),
      label: n.getLabel(),
      mass: n.getMass(),
      color: n.getColor(),
      group: n.getGroup(),
      year: n.getYear(),
    }));
    const edges = (await GemListUtil.toArraySync(this._data.getEdgesList())).map(e => ({
      from: e.getFrom(),
      to: e.getTo(),
      length: e.getLength()
    }));
    let data = {
      nodes: nodes,
      edges: edges
    };
    this._network = new Network(container, data, this._options);
    if (data.nodes && data.nodes.length !=0) {
      this._network.on("stabilizationProgress", (params) => {
        let networkheight = container.offsetHeight;
        let networkwidth = container.offsetWidth;
        document.getElementById("loadingBar").style.display = "inline";
        document.getElementById("outerBorder").style.marginLeft = (networkwidth / 2 - 300) + "px";
        document.getElementById("outerBorder").style.marginTop = "-" + (networkheight / 2 - 22) + "px";

        document.getElementById("loadingBar").style.opacity = "100";
        let maxWidth = 496;
        let minWidth = 20;
        let widthFactor = params.iterations / params.total;
        let width = Math.max(minWidth, maxWidth * widthFactor);

        document.getElementById("bar").style.width = width + "px";
        document.getElementById("text").innerText =
          Math.round(widthFactor * 100) + "%";
      });
      this._network.once("stabilizationIterationsDone", () => {
        document.getElementById("text").innerText = "100%";
        document.getElementById("bar").style.width = "496px";
        document.getElementById("loadingBar").style.opacity = "0";
        // really clean the dom element
        setTimeout(function () {
          document.getElementById("loadingBar").style.display = "none";
        }, 500);
      });
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

  public ngOnInit(): void { }
}
