/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, HostBinding} from '@angular/core';
import { config, GemDividerComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemDividerComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemDivider extends GemDividerComponent {
  @HostBinding('style.display')
  _display: string = 'block';

  @HostBinding('style.height')
  override _height: string = '1px';

  @HostBinding('style.width')
  override _width: string = '100%';

  @HostBinding('style.background-color')
  override _color: string = '#e0e0e0';

  @HostBinding('style.margin')
  _margin: string = '10px 0';

  public ngOnInit(): void { }
}
