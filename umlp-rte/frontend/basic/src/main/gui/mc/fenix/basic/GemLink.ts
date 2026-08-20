/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component} from '@angular/core';
import { config, GemLinkComponent } from '../../../../../../build/gui2java2ts/mc/fenix/basic/GemLinkComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: config.styles,
  templateUrl: config.templateUrl,
})
export class GemLink extends GemLinkComponent {
  ngOnInit() {}
}
