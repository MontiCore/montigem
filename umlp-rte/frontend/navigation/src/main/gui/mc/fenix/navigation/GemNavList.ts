/* (c) https://github.com/MontiCore/monticore */

import { AfterViewInit, Component, ElementRef, HostBinding, inject, NgZone, OnDestroy } from '@angular/core';
import { config, GemNavListComponent } from '../../../../../../build/gui2java2ts/mc/fenix/navigation/GemNavListComponent';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  imports: [...config.imports, MatIconModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemNavList extends GemNavListComponent implements AfterViewInit, OnDestroy {
  private readonly ngZone = inject(NgZone);
  private readonly elementRef = inject(ElementRef);
  private resizeObserver: ResizeObserver;

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

  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver((entries) => {
      const width = entries[0]?.contentRect.width ?? 0;
      this.ngZone.run(() => this.onResize(width));
    });
    this.resizeObserver.observe(this.elementRef.nativeElement);
  }

  override ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    super.ngOnDestroy();
  }

  private onResize(width: number): void {
    if (width < this.autoCollapseThreshold && !this.collapsed) {
      this.collapsed = true;
      this.collapsedChange.emit();
    }
  }

  private get autoCollapseThreshold(): number {
    const w = this.width || '';
    if (w.endsWith('px')) {
      return parseInt(w, 10);
    }
    return 120;
  }
}
