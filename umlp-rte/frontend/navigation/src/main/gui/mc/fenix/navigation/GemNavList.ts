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
  // true while the list is collapsed because it was too narrow, false if the user collapsed it
  private autoCollapsed = false;

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
    this.autoCollapsed = false;
    this.collapsed = !this.collapsed;
    this.collapsedChange.emit();
  }

  ngAfterViewInit(): void {
    // observe the space available to the list
    const container = this.elementRef.nativeElement.parentElement ?? this.elementRef.nativeElement;
    this.resizeObserver = new ResizeObserver((entries) => {
      const width = entries[0]?.contentRect.width ?? 0;
      this.ngZone.run(() => this.onResize(width));
    });
    this.resizeObserver.observe(container);
  }

  override ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    super.ngOnDestroy();
  }

  private onResize(availableWidth: number): void {
    // the list is not laid out yet, e.g. hidden parent or during a route change
    if (availableWidth === 0) {
      return;
    }
    const tooNarrow = availableWidth < this.autoCollapseThreshold;
    if (tooNarrow && !this.collapsed) {
      this.autoCollapsed = true;
      this.collapsed = true;
      this.collapsedChange.emit();
    } else if (!tooNarrow && this.collapsed && this.autoCollapsed) {
      this.autoCollapsed = false;
      this.collapsed = false;
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
