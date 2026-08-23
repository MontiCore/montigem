/* (c) https://github.com/MontiCore/monticore */

import { Component, HostBinding } from '@angular/core';
import { config, GemNavListComponent } from '../../../../../../build/gui2java2ts/mc/fenix/navigation/GemNavListComponent';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  imports: [...config.imports, MatIconModule],
  selector: config.selector,
  styles: [
    `
      :host {
        width: 100%;
        display: flex;
        position: sticky;
        overflow-y: auto;
        top: 0;
        right: 0;
        left: auto;
      }
    `
  ],
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemNavList extends GemNavListComponent {
  @HostBinding("style.width")
  get _hostWidth(): string {
    return this.collapsed ? (this.collapsedWidth || "4rem") : (this.width || "auto");
  }

  @HostBinding("style.height")
  get _hostHeight(): string {
    return this.height || "100%";
  }

  @HostBinding("class.gem-nav-list-collapsed")
  get _hostCollapsed(): boolean {
    return this.collapsed;
  }

  toggleCollapsed(): void {
    this.collapsed = !this.collapsed;
    this.collapsedChange.emit();
  }
}
