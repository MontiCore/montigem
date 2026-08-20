import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from "@angular/core";
import { CommonModule } from "@angular/common";
import { GraphOverlayPositionDirective } from "../../directives/graph-overlay-position.directive";

/**
 * GraphPopoverComponent is a lightweight, dependency-free replacement for PrimeNG's `p-popover`. It shows freely
 * projected content in a floating panel anchored to whichever element triggered {@link toggle}/{@link show}.
 *
 * @example
 *   <button (click)="popover.toggle($event)">Info</button>
 *   <graph-popover #popover (panelShow)="loadContent()">
 *     ...content...
 *   </graph-popover>
 */
@Component({
  selector: "graph-popover",
  standalone: true,
  imports: [CommonModule, GraphOverlayPositionDirective],
  templateUrl: "./graph-popover.component.html",
  styleUrls: [
    ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraphPopoverComponent {
  /** CSS class applied to the floating panel. */
  @Input() styleClass: string = "";

  /** Emits whenever the panel is shown. */
  @Output() panelShow: EventEmitter<void> = new EventEmitter<void>();

  /** Whether the panel is currently visible. */
  protected visible: boolean = false;

  /** The element the panel is currently anchored to. */
  protected anchor: HTMLElement | null = null;

  constructor(private changeDetectorRef: ChangeDetectorRef) {}

  /**
   * Shows the panel if hidden, hides it otherwise.
   *
   * @param {MouseEvent} event - The event that triggered the toggle; its `currentTarget` is used as the anchor.
   */
  public toggle(event: MouseEvent | null): void {
    if (this.visible) this.hide();
    else this.show(event);
  }

  /**
   * Shows the panel, anchored to the event's target.
   *
   * @param {MouseEvent | null} event - The event that triggered the show; its `currentTarget` is used as the anchor.
   */
  public show(event: MouseEvent | null): void {
    this.anchor = (event?.currentTarget as HTMLElement) ?? this.anchor;
    this.visible = true;
    this.panelShow.emit();
    // These calls happen imperatively (e.g. from a sibling component), not from within this component's own
    // template, so OnPush change detection needs an explicit nudge to pick up the resulting state change.
    this.changeDetectorRef.markForCheck();
  }

  /** Hides the panel. */
  public hide(): void {
    this.visible = false;
    this.anchor = null;
    this.changeDetectorRef.markForCheck();
  }
}
