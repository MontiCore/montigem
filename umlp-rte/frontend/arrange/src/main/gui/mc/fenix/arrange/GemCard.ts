/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, HostBinding, Input} from '@angular/core';
import { config, GemCardComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemCardComponent';
import { MatIcon } from '@angular/material/icon';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, MatIcon],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: [...config.styles, `:host { padding:2px; }`],
  templateUrl: config.templateUrl,
})
export class GemCard extends GemCardComponent {
  @HostBinding("style.width")
  _width: string = 'auto';

  @HostBinding("style.height")
  get computedHeight(): string {
    return (this.collapsible && this.isCollapsed) ? 'auto' : this._height;
  }

  _height: string = 'auto';

  @Input()
  public isCollapsed: boolean = false;

  @Input()
  public set collapsed(val: boolean) {
    this.isCollapsed = val;
  }
  public get collapsed(): boolean {
    return this.isCollapsed;
  }

  public toggleCollapse(): void {
    if (this.collapsible) {
      this.isCollapsed = !this.isCollapsed;
    }
  }

  public ngOnInit(): void { }
}
