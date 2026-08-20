import { Directive, ElementRef, HostListener, Input, OnDestroy, Renderer2 } from "@angular/core";
import { GraphTooltipOptions } from "../types/tooltip.types";

/**
 * GraphTooltipDirective is a lightweight, dependency-free replacement for PrimeNG's `pTooltip` directive. It shows a
 * small floating label near the host element on hover/focus.
 *
 * @example
 *   <button [graphTooltip]="'Zoom In'" [graphTooltipOptions]="{ tooltipPosition: 'top' }">...</button>
 */
@Directive({
  selector: "[graphTooltip]",
  standalone: true,
})
export class GraphTooltipDirective implements OnDestroy {
  /** The text to display inside the tooltip. */
  @Input("graphTooltip") label: string | undefined | null;

  /** Additional configuration for the tooltip (position, delays, ...). */
  @Input() graphTooltipOptions: GraphTooltipOptions | undefined;

  /** The floating tooltip element, created lazily on first show. */
  private tooltipElement: HTMLElement | undefined;

  /** Timer handles for the configured show/hide delays. */
  private showTimeout: ReturnType<typeof setTimeout> | undefined;
  private hideTimeout: ReturnType<typeof setTimeout> | undefined;

  private static readonly DEFAULT_SHOW_DELAY: number = 150;
  private static readonly DEFAULT_HIDE_DELAY: number = 0;

  constructor(
    private host: ElementRef<HTMLElement>,
    private renderer: Renderer2
  ) {}

  /** Schedules showing the tooltip after the configured show delay. */
  @HostListener("mouseenter")
  @HostListener("focus")
  protected onEnter(): void {
    if (this.graphTooltipOptions?.disabled || !this.resolvedLabel) return;
    clearTimeout(this.hideTimeout);
    this.showTimeout = setTimeout(() => this.show(), this.graphTooltipOptions?.showDelay ?? GraphTooltipDirective.DEFAULT_SHOW_DELAY);
  }

  /** Schedules hiding the tooltip after the configured hide delay. */
  @HostListener("mouseleave")
  @HostListener("blur")
  @HostListener("click")
  protected onLeave(): void {
    clearTimeout(this.showTimeout);
    this.hideTimeout = setTimeout(() => this.hide(), this.graphTooltipOptions?.hideDelay ?? GraphTooltipDirective.DEFAULT_HIDE_DELAY);
  }

  /** The text that should actually be rendered, preferring the explicit options label if set. */
  private get resolvedLabel(): string | undefined | null {
    return this.graphTooltipOptions?.tooltipLabel ?? this.label;
  }

  /** Creates (if necessary) and positions the tooltip element, then makes it visible. */
  private show(): void {
    if (!this.resolvedLabel) return;

    if (!this.tooltipElement) {
      this.tooltipElement = this.renderer.createElement("div");
      this.renderer.addClass(this.tooltipElement, "graph-tooltip");
      // Directives don't have a stylesheet of their own, and this element is appended directly to <body> (to
      // escape any clipping/stacking-context ancestors), so it needs to be fully styled inline — without an
      // explicit `position`, it would default to static and just flow to the bottom of the page.
      this.renderer.setStyle(this.tooltipElement, "position", "fixed");
      this.renderer.setStyle(this.tooltipElement, "z-index", "10000");
      this.renderer.setStyle(this.tooltipElement, "padding", "0.25rem 0.5rem");
      this.renderer.setStyle(this.tooltipElement, "border-radius", "4px");
      this.renderer.setStyle(this.tooltipElement, "background", "rgba(33, 33, 33, 0.9)");
      this.renderer.setStyle(this.tooltipElement, "color", "#fff");
      this.renderer.setStyle(this.tooltipElement, "font-size", "0.75rem");
      this.renderer.setStyle(this.tooltipElement, "line-height", "1.4");
      this.renderer.setStyle(this.tooltipElement, "white-space", "nowrap");
      this.renderer.setStyle(this.tooltipElement, "pointer-events", "none");
      this.renderer.appendChild(document.body, this.tooltipElement);
    }

    this.tooltipElement!.textContent = this.resolvedLabel;
    this.position();
  }

  /** Positions the tooltip relative to the host element, based on the configured tooltipPosition. */
  private position(): void {
    if (!this.tooltipElement) return;

    const hostRect: DOMRect = this.host.nativeElement.getBoundingClientRect();
    const tooltipRect: DOMRect = this.tooltipElement.getBoundingClientRect();
    const gap: number = 8;
    const position: string = this.graphTooltipOptions?.tooltipPosition ?? "top";

    let top: number;
    let left: number;

    switch (position) {
      case "bottom":
        top = hostRect.bottom + gap;
        left = hostRect.left + hostRect.width / 2 - tooltipRect.width / 2;
        break;
      case "left":
        top = hostRect.top + hostRect.height / 2 - tooltipRect.height / 2;
        left = hostRect.left - tooltipRect.width - gap;
        break;
      case "right":
        top = hostRect.top + hostRect.height / 2 - tooltipRect.height / 2;
        left = hostRect.right + gap;
        break;
      case "top":
      default:
        top = hostRect.top - tooltipRect.height - gap;
        left = hostRect.left + hostRect.width / 2 - tooltipRect.width / 2;
        break;
    }

    this.renderer.setStyle(this.tooltipElement, "top", `${Math.max(top, 0)}px`);
    this.renderer.setStyle(this.tooltipElement, "left", `${Math.max(left, 0)}px`);
  }

  /** Hides and removes the tooltip element from the DOM. */
  private hide(): void {
    if (!this.tooltipElement) return;
    this.renderer.removeChild(document.body, this.tooltipElement);
    this.tooltipElement = undefined;
  }

  /** Cleans up timers and any remaining tooltip element. */
  ngOnDestroy(): void {
    clearTimeout(this.showTimeout);
    clearTimeout(this.hideTimeout);
    this.hide();
  }
}
