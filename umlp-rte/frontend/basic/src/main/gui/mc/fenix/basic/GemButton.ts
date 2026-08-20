/* (c) https://github.com/MontiCore/monticore */

import { ChangeDetectionStrategy, Component, HostBinding } from '@angular/core';
import { config, GemButtonComponent } from '../../../../../../build/gui2java2ts/mc/fenix/basic/GemButtonComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: config.styles,
  templateUrl: config.templateUrl,
})
export class GemButton extends GemButtonComponent {
  @HostBinding("style.width")
  _width: string = "fit-content";

  @HostBinding("style.height")
  _height: string = "fit-content";

  @HostBinding("style.margin")
  _margin: string = "0.5ch";

  public ngOnInit(): void { }
}
