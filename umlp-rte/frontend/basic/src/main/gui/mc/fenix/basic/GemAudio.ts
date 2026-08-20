/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, HostBinding} from '@angular/core';
import { config, GemAudioComponent } from '../../../../../../build/gui2java2ts/mc/fenix/basic/GemAudioComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: config.styles,
  templateUrl: config.templateUrl,
})
export class GemAudio extends GemAudioComponent {
  @HostBinding("style.width")
  _width: string = "auto";

  public ngOnInit(): void { }
}
