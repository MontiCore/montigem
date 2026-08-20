/* (c) https://github.com/MontiCore/monticore */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { config, GemChartLegendComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemChartLegendComponent';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemChartLegend extends GemChartLegendComponent {
  /**contains the entries of the legend */
  _entries: { label: string, color: string }[] = [];

  /** setter that processes the color data to be shown correctly by the legend. */
  public override setColors(colors: string[]) {
    super.setColors(colors)
    this.updateOnInputChange()
  }

  public override setLabels(labels: string[]) {
    super.setLabels(labels)
    this.updateOnInputChange()
  }

  public updateOnInputChange() {
    if(!this.getLabels() || !this.getColors()) {
      return;
    }
    this._entries = [];

    for (let i = 0; i < this.getLabels().length; i++) {
      let legendEntry = {
        label: this.getLabels()[i],
        color: this.getColors()[i]
      };
      this._entries.push(legendEntry);
    }
  }

  public ngOnInit(): void { }
}
