/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, HostBinding, Input} from '@angular/core';
import { config, GemColumnComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemColumnComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemColumn extends GemColumnComponent {
  @HostBinding('style.height')
  override _height: string = 'auto';

  @HostBinding('style.width')
  override _width: string = 'auto';

  @HostBinding('style.align-content')
  @HostBinding('style.align-items')
  override _hAlign: string = "stretch";

  @HostBinding('style.justify-content')
  override _vAlign: string = "stretch";

  @HostBinding('style.row-gap')
  override _rowGap: string = "0";

  @HostBinding('style.column-gap')
  override _colGap: string = "0";

  @HostBinding('style.flex-wrap')
  public _wrap: string = 'nowrap';

  public override get hAlign(): string {
    return this._hAlign;
  }

  @Input()
  public override set hAlign(hAlign: string) {
    if (hAlign === 'left') {
      this._hAlign = 'flex-start'
    }
    else if (hAlign === 'right') {
      this._hAlign = 'flex-end'
    }
    else {
      this._hAlign = hAlign;
    }
  }

  public override get vAlign(): string {
    return this._vAlign;
  }

  @Input()
  public override set vAlign(vAlign: string) {
    if (vAlign === 'top') {
      this._vAlign = 'flex-start'
    }
    else if (vAlign === 'bottom') {
      this._vAlign = 'flex-end'
    }
    else {
      this._vAlign = vAlign;
    }
  }

  public ngOnInit(): void { }
}
