/* (c) https://github.com/MontiCore/monticore */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { config, GemChatSourceComponent } from '../../../../../../build/gui2java2ts/mc/fenix/chat/GemChatSourceComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: config.styles,
  templateUrl: config.templateUrl,
})
export class GemChatSource extends GemChatSourceComponent {
}
