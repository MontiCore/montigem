/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, HostBinding} from '@angular/core';
import { config, GemIconComponent } from '../../../../../../build/gui2java2ts/mc/fenix/basic/GemIconComponent';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, MatIconModule],
  selector: config.selector,
  styles: [...config.styles, '.gem-icon { width: 100%; height: 100%; }'],
  templateUrl: config.templateUrl,
})
export class GemIcon extends GemIconComponent {
}
