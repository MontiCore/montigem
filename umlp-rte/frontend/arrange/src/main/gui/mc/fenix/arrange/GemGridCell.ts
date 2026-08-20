/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, HostBinding, Input, TemplateRef} from "@angular/core";
import { config, GemGridCellComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemGridCellComponent';
import {GemListUtil, IUMLPList} from "@umlp/commonj2ts";

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemGridCell extends GemGridCellComponent {
  @HostBinding('style.grid-column-start')
  _x1: string = "auto";

  @HostBinding('style.grid-column-end')
  _x2: string = "span 1";

  @HostBinding('style.grid-row-start')
  _y1: string = "auto";

  @HostBinding('style.grid-row-end')
  _y2: string = "span 1";

  _a: number[] = [];
  _b: number[] = [];

  public override setA(val : Array<number>) {
    super.setA(val)
    if (!val) {
      return;
    }
    this._a = val;
    this.update();
  }

  public override setB(val : Array<number>) {
    super.setB(val)
    if (!val) {
      return;
    }
    this._b = val;
    this.update();
  }

  @Input()
  _content: TemplateRef<any>;

  update() {
    if (this._a[0]) {
      this._x1 = this._a[0] + '';
    }
    if (this._a[1]) {
      this._y1 = this._a[1] + '';
    }

    if (this._b[0] && this._a[0]) {
      this._x2 = 'span ' + (this._b[0] - this._a[0] + 1);
    }
    if (this._b[1] && this._a[1]) {
      this._y2 = 'span ' + (this._b[1] - this._a[1] + 1);
    }
  }

  public ngOnInit(): void { }
}
