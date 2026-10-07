import { ChangeDetectorRef, Component, Input } from '@angular/core';
import { config, GemChartLegendComponent } from '../../../../../../build/gui2java2ts/mc/fenix/charts/GemChartLegendComponent';

@Component({
  standalone: true,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemChartLegend extends GemChartLegendComponent {
  _entries: { label: string, color: string }[] = [];

  constructor(private cdr: ChangeDetectorRef) {
    super();
  }

  public override setLabels(val: Array<string>) {
    super.setLabels(val);
    this.updateEntries();
  }

  public override setColors(val: Array<string>) {
    super.setColors(val);
    this.updateEntries();
  }

  @Input()
  public override set colors(colors: any) {
    super.colors = colors;
    this.updateEntries();
  }

  @Input()
  public override set labels(labels: any) {
    super.labels = labels;
    this.updateEntries();
  }

  public updateEntries() {
    this._entries = [];
    const labels = this.labels || this._labels || [];
    const colors = this.colors || this._colors || [];
    for (let i = 0; i < labels.length; i++) {
      this._entries.push({
        label: labels[i],
        color: colors[i] || '#000000'
      });
    }
    if (this.cdr) {
      this.cdr.markForCheck();
    }
  }

  public override ngOnInit(): void {
    super.ngOnInit();
    this.updateEntries();
  }
}
