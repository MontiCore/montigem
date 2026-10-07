/* (c) https://github.com/MontiCore/monticore */

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  HostBinding,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { config, GemSunburstChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemSunburstChartComponent';
import { GemSunburstData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemsunburstcharttypes/GemSunburstData';
import { GemSunburstDataBuilder } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemsunburstcharttypes/GemSunburstDataBuilder';
import { GemSunburstEntry } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemsunburstcharttypes/GemSunburstEntry';
import { GemSunburstEntryBuilder } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemsunburstcharttypes/GemSunburstEntryBuilder';
import { autoColors } from './colors';
import { GemChartOptions } from './GemChartOptions';
import { GemChartLegend } from './GemChartLegend';
import { GemListUtil, AsyncList } from "@umlp/commonj2ts";
import { PlotlyModule } from 'angular-plotly.js';
import * as Plotly from 'plotly.js-dist-min';
import { setupResizeObserver } from './ResizeObserverUtil';

PlotlyModule.plotlyjs = Plotly;

@Component({
  standalone: true,
  imports: [...config.imports, CommonModule, PlotlyModule, GemChartOptions, GemChartLegend],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GemSunburstChart extends GemSunburstChartComponent implements OnInit, OnDestroy {

  @HostBinding('style.display')
  get hostDisplay() { return 'block'; }

  @HostBinding('style.width')
  get hostWidth() { return '100%'; }

  @HostBinding('style.height')
  get hostHeight() { return '100%'; }

  public newData: GemSunburstData;
  public entry: GemSunburstEntry = null;
  public subEntries: GemSunburstData[] = [];
  public scaler = 1;

  public legendPosition: string = 'bottom';
  public displayLegend = true;
  @Input() public showInChartLabels: boolean = false;
  @Input() public showMainPageInChartLabels: boolean = false;
  public errorMessage: String = "";
  public hasData: boolean = false;
  private ignoreErrorInput: boolean = false;

  private userColors: string[] = [];

  public plotlyData: any[] = [];

  _layout: any = {
    autosize: true,
    margin: { t: 60, l: 30, r: 30, b: 30 },
    sunburstcolorway: autoColors,
    extendsunburstcolorway: true,
    uniformtext: {minsize: 6, mode: 'hide'},
    font: {
      family: '"Open Sans", verdana, arial, sans-serif'
    }
  };

  public legendLabels: string[] = [];
  public legendColors: string[] = [];
  private legendColorIndex: number = 0;

  _config: any = {
    displaylogo: false,
    displayModeBar: false,
    responsive: true
  };

  private resizeObserver!: ResizeObserver;

  // the plot only gets the space next to the custom legend, so its container is observed instead of the host;
  // it lives inside *ngIf and appears only once there is data
  @ViewChild('plotContainer')
  set plotContainer(container: ElementRef) {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
      this.resizeObserver = null;
    }
    if (container) {
      this.resizeObserver = setupResizeObserver(container.nativeElement, (w, h) => this.handleResize(w, h));
    }
  }

  private ids: string[] = [];
  private labels: string[] = [];
  private parents: string[] = [];
  private values: number[] = [];
  private markerColors: string[] = [];
  private nodeCounter: number = 0;

  constructor(private el: ElementRef, private cdr: ChangeDetectorRef) {
    super();
  }

  public getColors() {
    return autoColors;
  }

  public ngOnInit(): void {
    super.ngOnInit();
  }

  ngOnDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
  }

  private handleResize(width: number, height: number) {
    if (width === 0 || height === 0) return;

    this._layout.width = width;
    this._layout.height = height;

    this.updatePlot();
  }

  private updatePlot(): void {
    this.plotlyData = [...this.plotlyData];
    this._layout = {
      ...this._layout,
      margin: { ...this._layout.margin }
    };
    this.cdr.detectChanges();
  }

  @Input()
  public set ignoreError(iEI: boolean) {
    if (iEI == null){
      return
    }
    (async () => {
      this.ignoreErrorInput = iEI;
      if (this.newData != null) {
        this.hasData = true;
        if (this.newData.getSubEntriesList() == null
          || (await this.newData.getSubEntriesList().size() == 0
            && this.newData.getEntry().getLabel() == ""
            && this.newData.getEntry().getValue() == 0)) {
          this.hasData = false;
        }
        this.updateData();
      }
    })();
  }

  @Input()
  public set colors(colorArray: string[]) {
    if (colorArray != null) {
      this.userColors = colorArray;
    }
  }

  @Input()
  public set data(data: any) {
    if (data == null) {
      return
    }
    (async () => {
      this.newData = (await (await new GemSunburstDataBuilder().__GemSunburstDataBuilderConstructor()).entry(this.entry).subEntries(
        await new AsyncList<GemSunburstData>().__AsyncListConstructor(this.subEntries))).build()

      this.newData = data as GemSunburstData;

      this.entry = this.newData.getEntry();
      this.subEntries = await GemListUtil.toArraySync(this.newData.getSubEntriesList());

      this.hasData = true;
      if (this.newData.getSubEntriesList() == null
        || (await this.newData.getSubEntriesList().size() == 0
          && this.newData.getEntry().getLabel() == ""
          && this.newData.getEntry().getValue() == 0)) {
        this.hasData = false;
      }

      this.updateData();
    })()
  }

  async updateData() {
    if (this.ignoreErrorInput) {
      let total = 0;
      for (let i = 0; this.newData.getSubEntriesList() != null && i < (await this.newData.getSubEntriesList().size()); i++) {
        total += (await this.newData.getSubEntries(i)).getEntry().getValue();
      }
      this.newData.getEntry().setValue(total);
    }
    setTimeout(async () => {
      await this.check(this.newData);
      if (this.hasData) {
        await this.dataNodesToChart(this.newData);
      } else {
        this.cdr.detectChanges();
      }
    }, 0)
  }

  public async check(node: GemSunburstData): Promise<void> {
    if (this.hasData && node.getSubEntriesList() != null && (await node.getSubEntriesList().size()) > 0) {
      let cur = node.getEntry().getValue();
      for (let i = 0; i < await node.getSubEntriesList().size(); i++) {
        cur -= (await node.getSubEntries(i)).getEntry().getValue();
        await this.check((await node.getSubEntries(i)));
      }
      if (this.ignoreErrorInput) {
        if (cur > 0) {
          await node.getSubEntriesList().add(
            new GemSunburstDataBuilder().entry(new GemSunburstEntryBuilder().label("").value(cur).build()).build());
        }
        if (cur < 0) {
          this.errorMessage = `Data doesn't sum up to the total value of a node. (subEntries of: ${node.getEntry().getLabel()}, ${node.getEntry().getValue()})`;
          console.error("GemSunburstChart error: ", this.errorMessage, "cur is", cur);
          this.hasData = false;
        }
      }
      else {
        if (cur !== 0) {
          this.errorMessage = `Data doesn't sum up to the total value of a node. (subEntries of: ${node.getEntry().getLabel()}, ${node.getEntry().getValue()}). Option: "[ignoreError] = true" if this should be ignored.`;
          console.error("GemSunburstChart error: ", this.errorMessage, "cur is", cur);
          this.hasData = false;
        }
      }
    }
  }

  private async dataNodesToChart(data: GemSunburstData): Promise<void> {
    this.ids = [];
    this.labels = [];
    this.parents = [];
    this.values = [];
    this.markerColors = [];
    this.nodeCounter = 0;
    this.legendColorIndex = 0;
    this.legendLabels = [];
    this.legendColors = [];

    await this.flattenData(data, "", 0);

    const trace: any = {
      type: "sunburst",
      ids: this.ids,
      labels: this.labels,
      parents: this.parents,
      values: this.values,
      branchvalues: "total",
      textinfo: this.showInChartLabels ? "label" : "none",
      insidetextorientation: "auto",
      hoverinfo: "label+value+percent root",
      marker: { colors: this.markerColors }
    };

    this.plotlyData = [trace];
    this.updatePlot();
  }

  public toggleInChartLabels() {
    this.showInChartLabels = !this.showInChartLabels;
    this.updateData();
  }

  public toggleMainPageLabels() {
    this.showMainPageInChartLabels = !this.showMainPageInChartLabels;
    this.updateData();
  }

  // Colors are only assigned explicitly for the level directly below the root (depthIndex === 1).
  // Deeper levels are left undefined so Plotly still auto-shades them via extendsunburstcolorway,
  // while the explicit level-1 colors let the custom legend match the rendered chart.
  private async flattenData(node: GemSunburstData, parentId: string, depthIndex: number) {
    if (!node) return;

    const label = node.getEntry().getLabel() || "";
    const value = node.getEntry().getValue() || 0;

    this.nodeCounter++;
    const currentId = parentId ? `${parentId}/${label}-${this.nodeCounter}` : `${label}-${this.nodeCounter}`;

    this.ids.push(currentId);

    let inChartLabel = label === "" ? " " : label;
    if (depthIndex === 0) {
      inChartLabel = " ";
    } else if (depthIndex === 1 && !this.showMainPageInChartLabels) {
      inChartLabel = " ";
    }

    this.labels.push(inChartLabel);
    this.parents.push(parentId);
    this.values.push(value);

    if (depthIndex === 1) {
      const colorArray = this.userColors && this.userColors.length > 0 ? this.userColors : autoColors;
      const color = colorArray[this.legendColorIndex % colorArray.length];
      this.legendColorIndex++;
      this.markerColors.push(color);
      this.legendLabels.push(label === "" ? " " : label);
      this.legendColors.push(color);
    } else {
      this.markerColors.push(undefined as any);
    }

    const subEntriesList = node.getSubEntriesList();
    if (subEntriesList) {
      const subEntries = await GemListUtil.toArraySync(subEntriesList);
      for (const subNode of subEntries) {
        await this.flattenData(subNode, currentId, depthIndex + 1);
      }
    }
  }

  public setLegendPosition(legendPosition: string) {
    this.legendPosition = legendPosition;
    this.updatePlot();
    this.cdr.detectChanges();
  }

  public changeContainer() {
    switch (this.legendPosition) {
      case 'left':
        return 'wholeChartWithLegendLeftContainer';
      case 'top':
        return 'wholeChartWithLegendTopContainer';
      case 'right':
        return 'wholeChartWithLegendRightContainer';
      case 'bottom':
      default:
        return 'wholeChartWithLegendBottomContainer';
    }
  }
}

GemSunburstChart["__class"] = "gui.charts.SunburstChart";
