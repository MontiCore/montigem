import { AfterViewInit, Directive, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, Renderer2 } from "@angular/core";

/**
 * GraphOverlayPositionDirective anchors its host element (a floating panel, e.g. a dropdown or popover) next to a given
 * anchor element using fixed positioning, and notifies when the user clicks outside of both. This replaces the
 * combination of PrimeNG's `appendTo="body"` positioning and its outside-click-to-close behavior, without needing to
 * actually reparent DOM nodes: `position: fixed` already escapes any `overflow: hidden`/clipped ancestor as long as
 * none of them establish a new containing block (e.g. via `transform`).
 *
 * @example
 *   <div class="panel" [graphOverlayPosition]="triggerElement" (graphOutsideClick)="close()">...</div>
 */
@Directive({
  selector: "[graphOverlayPosition]",
  standalone: true,
})
export class GraphOverlayPositionDirective implements AfterViewInit, OnChanges, OnDestroy {
  /** The element that the host panel should be positioned relative to. */
  @Input("graphOverlayPosition") anchor: HTMLElement | undefined | null;

  /** Horizontal alignment of the panel relative to the anchor. */
  @Input() align: "start" | "end" = "start";

  /** Whether the panel should receive a `min-width` matching the anchor's width. */
  @Input() matchAnchorWidth: boolean = true;

  /** Emits when the user clicks/touches outside of both the panel and its anchor. */
  @Output() graphOutsideClick: EventEmitter<void> = new EventEmitter<void>();

  private removeDocumentListener: (() => void) | undefined;
  private removeResizeListener: (() => void) | undefined;
  private removeScrollListener: (() => void) | undefined;

  constructor(
    private host: ElementRef<HTMLElement>,
    private renderer: Renderer2
  ) {}

  ngAfterViewInit(): void {
    this.reposition();
    this.removeDocumentListener = this.renderer.listen("document", "mousedown", (event: MouseEvent) => this.onDocumentMouseDown(event));
    this.removeResizeListener = this.renderer.listen("window", "resize", () => this.reposition());
    this.removeScrollListener = this.renderer.listen("window", "scroll", () => this.reposition(), { capture: true } as any);
  }

  ngOnChanges(): void {
    this.reposition();
  }

  ngOnDestroy(): void {
    this.removeDocumentListener?.();
    this.removeResizeListener?.();
    this.removeScrollListener?.();
  }

  /** Computes and applies the fixed position of the panel relative to the anchor, flipping upward if necessary. */
  public reposition(): void {
    if (!this.anchor) return;

    const anchorRect: DOMRect = this.anchor.getBoundingClientRect();
    const panel: HTMLElement = this.host.nativeElement;
    const panelRect: DOMRect = panel.getBoundingClientRect();
    const gap: number = 4;

    let top: number = anchorRect.bottom + gap;
    const overflowsBottom: boolean = top + panelRect.height > window.innerHeight;
    const fitsAbove: boolean = anchorRect.top - panelRect.height - gap > 0;
    if (overflowsBottom && fitsAbove) {
      top = anchorRect.top - panelRect.height - gap;
    }

    let left: number = this.align === "end" ? anchorRect.right - panelRect.width : anchorRect.left;
    left = Math.min(Math.max(left, gap), Math.max(window.innerWidth - panelRect.width - gap, gap));

    this.renderer.setStyle(panel, "position", "fixed");
    this.renderer.setStyle(panel, "top", `${top}px`);
    this.renderer.setStyle(panel, "left", `${left}px`);
    if (this.matchAnchorWidth) {
      this.renderer.setStyle(panel, "min-width", `${anchorRect.width}px`);
    }
  }

  /** Emits {@link graphOutsideClick} if the click target is outside both the panel and its anchor. */
  private onDocumentMouseDown(event: MouseEvent): void {
    const target: Node | null = event.target as Node;
    if (!target) return;
    const insidePanel: boolean = this.host.nativeElement.contains(target);
    const insideAnchor: boolean = !!this.anchor?.contains(target);
    if (!insidePanel && !insideAnchor) this.graphOutsideClick.emit();
  }
}
