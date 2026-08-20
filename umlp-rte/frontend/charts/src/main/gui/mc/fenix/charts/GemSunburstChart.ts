/* (c) https://github.com/MontiCore/monticore */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { config, GemSunburstChartComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemSunburstChartComponent';
import { ChartDataset, ChartOptions } from "chart.js";
import { NgChartsModule } from 'ng2-charts';
import { GemSunburstData } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemsunburstcharttypes/GemSunburstData';
import { GemSunburstDataBuilder } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemsunburstcharttypes/GemSunburstDataBuilder';
import { GemSunburstEntry } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemsunburstcharttypes/GemSunburstEntry';
import { GemSunburstEntryBuilder } from '../../../../../../build/gui2java2ts/mc/fenix/charts/gemsunburstcharttypes/GemSunburstEntryBuilder';
import { autoColors } from './colors';
import { GemChartLegend } from './GemChartLegend';
import { GemChartOptions } from './GemChartOptions';
import {GemListUtil, AsyncList} from "@umlp/commonj2ts";

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, NgChartsModule, GemChartOptions, GemChartLegend],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemSunburstChart extends GemSunburstChartComponent {

  public newData: GemSunburstData;

  // Placeholder values
  public entry: GemSunburstEntry = null;
  public subEntries: GemSunburstData[] = [];

  // Standard value scale
  public scaler = 1;

  // Standard values for legend are empty
  public legendColors: string[] = [];
  public legendPosition: string;
  public legendLabels = []

  /**variable that enables or disables the legend */
  public displayLegend = true;

  public errorMessage: String = "";

  // Standard options for ng2-charts
  public options: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: true,
  };

  private userColors = [];

  public colorsD = [{ backgroundColor: ["#eeeeee"], }];

  public hasData: boolean = false;

  private ignoreErrorInput: boolean = false;

  private depth = 0;

  public labels: string[] = [''];
  public dataSets: ChartDataset<'doughnut'>[] = [];

  public getColors() {
    return autoColors;
  }

  // User can decide if charts in which parent Nodes can have a higher value than the sum of the values of all subEntries are allowed
  @Input()
  public set ignoreError(iEI: boolean) {
    if (iEI == null){
      return
    }
    (async () => {
      this.ignoreErrorInput = iEI;
      // update hasData if there is data, because hasData is set to false when the error is reported
      if (this.newData != null) {
        this.hasData = true;
        // If newData equals a placeholder set in set data instead of real data update hasData to false
        if (this.newData.getSubEntriesList() == null
          || (await this.newData.getSubEntriesList().size() == 0
            && this.newData.getEntry().getLabel() == ""
            && this.newData.getEntry().getValue() == 0)) {
          this.hasData = false;
        }
        // call updateData to finish data calculations that were interrupted by hasData == false
        this.updateData();
      }
    })();
  }

  // User can have own colors as input
  @Input()
  public set colors(colorArray: string[]) {
    if (colorArray != null) {
      // Colors have to be reversed in calculations
      this.userColors = colorArray.reverse();
    }
  }

  // Input of data; starts calculation for the whole chart component
  @Input()
  public set data(data: any) {
    if (data == null) {
      return
    }
    (async () => {
      // Initialize newData as placeholder
      this.newData = (await (await new GemSunburstDataBuilder().__GemSunburstDataBuilderConstructor()).entry(this.entry).subEntries(
        await new AsyncList<GemSunburstData>().__AsyncListConstructor(this.subEntries))).build()
      // Check if data is an instance of GemSDNshort
      /*      if (gemInstanceOfSDNshort(data)) {
              // Set newData as the GemSunburstData that is equal to data in GemSDNshort notation
              this.newData = gemSdnShortToSDN(data as GemSDNshort);
            }
            else {*/
      // Set newData as data which is in GemSunburstData notation
      this.newData = data as GemSunburstData;
      /*      }*/
      // Update placeholders
      this.entry = this.newData.getEntry();
      this.subEntries = await GemListUtil.toArraySync(this.newData.getSubEntriesList());

      // There is data
      this.hasData = true;
      // If data has no subEntries or is just the placeholder -> No data for chart
      if (this.newData.getSubEntriesList() == null
        || (await this.newData.getSubEntriesList().size() == 0
          && this.newData.getEntry().getLabel() == ""
          && this.newData.getEntry().getValue() == 0)) {
        this.hasData = false;
      }

      // Start calculations for component
      this.updateData();
    })()
  }

  // Comprises calculations of this component
  async updateData() {
    // If sums get ignored
    if (this.ignoreErrorInput) {
      // Count the values of the subEntries and update the total value of the parentNode accordingly
      let total = 0;
      for (let i = 0; this.newData.getSubEntriesList() != null && i < (await this.newData.getSubEntriesList().size()); i++) {
        total += (await this.newData.getSubEntries(i)).getEntry().getValue();
      }
      this.newData.getEntry().setValue(total);
    }
    setTimeout(async () => {
      // Check if data has right syntax
      this.check(this.newData);
      // If there is data
      if (this.hasData) {
        // Calculate depth of data tree
        this.depth = await this.getDepth(this.newData);
        // Start calculation of chart entrys and labels
        await this.dataNodesToChart(this.newData);
        // Set ng2-charts options
        this.options = { cutout: 0, plugins: { legend: { display: false, } } };
      }
    }, 0)
  }


  // Check the data for correct format
  public async check(node: GemSunburstData): Promise<void> {
    // If data has no subEntries or hasData is already false there is nothing to check
    if (this.hasData && node.getSubEntriesList() != null && (await node.getSubEntriesList().size()) > 0) {
      // cur is value of current node
      let cur = node.getEntry().getValue();
      for (let i = 0; i < await node.getSubEntriesList().size(); i++) {
        // Substract value of every subNode from cur
        cur -= (await node.getSubEntries(i)).getEntry().getValue();
        // Go into every subNode of cur to check it recursively
        this.check((await node.getSubEntries(i)));
      }
      // If values of subEntries does not have to sum up to value of parentNode
      if (this.ignoreErrorInput) {
        // If there is a part of the parentNode that is not filled by the subEntries
        if (cur > 0) {
          // Add empty node that fills the gap
          await node.getSubEntriesList().add(
            new GemSunburstDataBuilder().entry(new GemSunburstEntryBuilder().label("").value(cur).build()).build());
        }
        // If the sum of the values of the subEntries is higher than the parent node's value -> Error and has no Data
        if (cur < 0) {
          this.errorMessage = `
            Data doesn't sum up to the total value of a node.
            (subEntries of: ${node.getEntry().getLabel()}, ${node.getEntry().getValue()})
          `;
          this.hasData = false;
        }
      }
      // If values of subEntries have to sum up to value of parentNode
      else {
        // If the sum of the values of the subEntries does not equal the parent node's value -> Error and has no Data
        if (cur !== 0) {
          this.errorMessage = `
            Data doesn't sum up to the total value of a node.
            (subEntries of: ${node.getEntry().getLabel()}, ${node.getEntry().getValue()}).
            Option: "[ignoreError] = true" if this should be ignored.
          `;
          this.hasData = false;
        }
      }
    }
  }

  // Calculates the depth of the data tree from given node
  public async getDepth(node: GemSunburstData): Promise<number> {
    // If node has subEntries
    if (node.getSubEntriesList() != null && (await node.getSubEntriesList().size()) > 0) {
      // First get depth of first child node
      let cur = await this.getDepth(await node.getSubEntries(0));
      // For every other child node
      for (let i = 1; i < (await node.getSubEntriesList().size()); i++) {
        // Get depth of current child node
        let newCur = await this.getDepth(await node.getSubEntries(i));
        // If depth of current child node is greater than the current maximum -> update maximum
        if (newCur > cur) cur = newCur;
      }
      // Depth of this node is equal to the depth of maximum plus one
      return cur + 1;
    }
    // If node has no subEntries depth is 0
    return 0;
  }

  // Converts the data of the GemSunburstData into data for the used doughnut chart
  private async dataNodesToChart(data: GemSunburstData): Promise<void> {
    // Initialized as empty
    this.labels = [];
    this.dataSets = [];

    let definedColors: string[];
    // If user did not define own colors use predefined colors
    if (this.userColors.length == 0) {
      definedColors = this.getColors();
    } else {
      definedColors = this.userColors;
    }
    this.legendColors = definedColors;

    // curNodes are all subEntries of this data
    let curNodes = [];
    for (let i = 0; i < (await data.getSubEntriesList().size()); ++i) {
      console.warn("Entry is", (await data.getSubEntries(i)).getEntry())
      curNodes.push({
        entry: (await data.getSubEntries(i)).getEntry(),
        subEntries: (await GemListUtil.toArraySync((await data.getSubEntries(i)).getSubEntriesList())),
        color: definedColors[i]
      })
    }
    // Initialize as empty
    let nextNodes = [];
    let curDatas: { value: number, color: string }[] = [];
    let curLabels: string[] = [];
    let sunburstDatasTemp: { value: number, color: string }[][] = [];
    let sunburstLabelsTemp: string[][] = [];


    // There are as many doughnuts and data arrays as the depth of the tree is
    for (let j = 0; j < this.depth; j++) {
      // For every node in curNodes (for all current nodes)
      for (let i = 0; curNodes != null && i < curNodes.length; i++) {
        // Push value and label of current node to current data and current labels
        console.warn("Current node:", curNodes[i])
        console.warn("Current node Entry:", curNodes[i].entry)
        curDatas.push({
          value: curNodes[i].entry.getValue(),
          color: curNodes[i].color
        });
        curLabels.push(curNodes[i].entry.getLabel());
        // If current node has no subEntries
        if (curNodes[i].subEntries == null || curNodes[i].subEntries.length == 0) {
          // Push an empty node into the nextNodes (-> next cycle will deal with this subEntries as one empty node)
          nextNodes.push({ entry: { label: "", value: curNodes[i].entry.getValue(), }, subEntries: [], color: '#00000000' });
        }
        // If current node has subEntries
        else {
          // Push every subNode of current node into the nextNodes (-> next cycle will deal with every subNode)
          for (let k = 0; k < curNodes[i].subEntries.length; k++) {
            nextNodes.push({
              entry: curNodes[i].subEntries[k].getEntry(),
              subEntries: curNodes[i].subEntries[k].getSubEntriesList(),
              color: curNodes[i].color
            });
          }
        }
      }
      // curNodes = nextNodes
      curNodes = [];
      curNodes = curNodes.concat(nextNodes);
      // nextNodes initialized new as empty
      nextNodes = [];
      // Push the current datas and labels into the temporary containers for all datas and labels
      sunburstDatasTemp.push(curDatas);
      sunburstLabelsTemp.push(curLabels);
      // Empty the current datas and current labels
      curDatas = [];
      curLabels = [];
    }

    // Data has to be reversed (doughnut goes counter-clockwise)
    sunburstDatasTemp.reverse();

    // Call methods for label refinement and color refinement
    this.changeDataNodesForLabels(sunburstDatasTemp, sunburstLabelsTemp);
  }

  // Adjusts the labels of the nodes to fit
  private changeDataNodesForLabels(
    sunburstDatas: { value: number, color: string }[][], sunburstLabels: string[][]
  ): void {
    // Initialize empty
    let sunburstDatasTemp: { value: number, color: string }[][] = [[]];
    let sunburstLabelsTemp: string[][] = [[]];

    // Insert 0-entrys: If tree on depth x has n nodes -> Insert n 0-entrys (empty nodes) into tree on depth x+1
    sunburstDatasTemp[0] = sunburstDatas[0];
    for (let j = 1; j < sunburstDatas[0].length; j++) {
      sunburstDatasTemp[j] = [{ value: 0, color: '#00000000' }];
      for (let i = 1; i < sunburstDatasTemp[j - 1].length; i++) {
        sunburstDatasTemp[j].push({ value: 0, color: '#00000000' });
      }
      sunburstDatasTemp[j] = sunburstDatasTemp[j].concat(sunburstDatas[j]);
    }
    // Concat all label arrays into one label array
    for (let i = sunburstLabels.length; i >= 0; i--) {
      sunburstLabelsTemp[0] = sunburstLabelsTemp[0].concat(sunburstLabels[i]);
    }
    // Pop first element (empty placeholder)
    sunburstLabelsTemp[0].shift();

    // Set datas and labels for this component
    this.dataSets = sunburstDatasTemp.map(d => {
      return {
        data: d.filter(e => !!e).map(e => e.value),
        backgroundColor: d.filter(e => !!e).map(e => e.color)
      };
    });
    this.labels = sunburstLabelsTemp[0];
    this.legendLabels = this.labels.slice(this.labels.length - this.subEntries.length);
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
