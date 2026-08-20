/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, Input, TemplateRef} from "@angular/core";
import { config, GemOverlayComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemOverlayComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemOverlay extends GemOverlayComponent {
  _components: TemplateRef<any>[] = [];

  public get background(): TemplateRef<any> {
    return this._background;
  }

  @Input()
  public set background(background: TemplateRef<any>) {
    this._background = background;
  }

  public get components(): TemplateRef<any>[] {
    return this._components;
  }

  @Input()
  public set components(components: TemplateRef<any>[]) {
    this._components = components;
  }

  public ngOnInit(): void { }
}
