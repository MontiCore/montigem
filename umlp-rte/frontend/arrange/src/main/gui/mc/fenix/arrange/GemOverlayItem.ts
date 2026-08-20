/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, HostBinding, Input, TemplateRef} from "@angular/core";
import { config, GemOverlayItemComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemOverlayItemComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemOverlayItem extends GemOverlayItemComponent {
  @HostBinding('style.left')
  _x: string = '0';

  @HostBinding('style.top')
  _y: string = '0';

  public get component(): TemplateRef<any> {
    return this._component;
  }

  @Input()
  public set component(value: TemplateRef<any>) {
    this._component = value;
  }

  public get x(): string {
    return this._x;
  }

  @Input()
  public set x(value: string) {
    this._x = value;
  }

  public get y(): string {
    return this._y;
  }

  @Input()
  public set y(value: string) {
    this._y = value;
  }

  public ngOnInit(): void { }
}
