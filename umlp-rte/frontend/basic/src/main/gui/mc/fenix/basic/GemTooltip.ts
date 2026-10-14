/* (c) https://github.com/MontiCore/monticore */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { config, GemTooltipComponent } from '../../../../../../build/gui2java2ts/mc/fenix/basic/GemTooltipComponent';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, MatTooltipModule],
  selector: config.selector,
  styles: [...config.styles, ':host { width: fit-content; height: fit-content; } .gem-tooltip { width: 100%; height: 100%; }'],
  templateUrl: config.templateUrl,
})
export class GemTooltip extends GemTooltipComponent {
}
