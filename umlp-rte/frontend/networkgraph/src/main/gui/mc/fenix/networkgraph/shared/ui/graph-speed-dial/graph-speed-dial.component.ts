import { ChangeDetectionStrategy, Component, ElementRef, HostListener, Input, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { GraphTooltipDirective } from "../../directives/graph-tooltip.directive";
import { GraphTooltipOptions } from "../../types/tooltip.types";
import { GraphSpeedDialItem } from "./graph-speed-dial.types";

/** Direction in which speed-dial actions fan out from the main button. */
export type GraphSpeedDialDirection = "up" | "down" | "left" | "right" | "up-left" | "up-right" | "down-left" | "down-right";

/** Arrangement of speed-dial actions around the main button. */
export type GraphSpeedDialType = "linear" | "semi-circle" | "quarter-circle";

/**
 * GraphSpeedDialComponent is a lightweight, dependency-free replacement for PrimeNG's `p-speeddial`. Clicking the main
 * button fans a list of actions out along an arc (or a straight line), each shown with its own icon and tooltip.
 *
 * @remarks
 *   - The distance-from-center ({@link radius}) is intentionally left for the consumer to compute/provide (see
 *       `ControlButtonsComponent.getRadius`), since it depends on the rendered size of the action buttons.
 *
 * @example
 *   <graph-speed-dial [model]="items" [radius]="60" direction="up-right" type="quarter-circle"></graph-speed-dial>
 */
@Component({
  selector: "graph-speed-dial",
  standalone: true,
  imports: [CommonModule, GraphTooltipDirective],
  templateUrl: "./graph-speed-dial.component.html",
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraphSpeedDialComponent implements OnInit {
  /** The actions shown when the dial is open. */
  @Input() model: GraphSpeedDialItem[] = [];

  /** Initial open/closed state. Only read once on init; the dial then manages its own open state internally. */
  @Input() visible: boolean = false;

  /** Distance (in px) between the main button and each action button. */
  @Input() radius: number = 60;

  /** Direction in which the actions fan out. */
  @Input() direction: GraphSpeedDialDirection = "up-right";

  /** Arrangement of the actions. */
  @Input() type: GraphSpeedDialType = "quarter-circle";

  /**
   * @deprecated No longer used — all actions now animate simultaneously instead of staggering in one by one. Kept
   *   only so existing `SpeedDialConfig.transitionDelay` bindings don't break.
   */
  @Input() transitionDelay: number = 30;

  /** Whether clicking outside of the dial closes it. */
  @Input() hideOnClickOutside: boolean = false;

  /** Whether the main button's icon rotates when the dial opens (only applies when no {@link hideIcon} is given). */
  @Input() rotateAnimation: boolean = true;

  /** Icon shown on the main button while closed. */
  @Input() showIcon: string | undefined;

  /** Icon shown on the main button while open. If omitted, {@link showIcon} is reused (and optionally rotated). */
  @Input() hideIcon: string | undefined;

  /** CSS class applied to the main button. */
  @Input() buttonClassName: string = "";

  /** CSS class applied to the host element. */
  @Input() className: string = "";

  /** Accessible label for the main button. */
  @Input() ariaLabel: string = "";

  /** Whether the dial is currently open. */
  protected open: boolean = false;

  private static readonly DEFAULT_ICON: string = "pi pi-plus";

  private static readonly DIRECTION_ANGLES: Record<GraphSpeedDialDirection, number> = {
    up: 0,
    right: 90,
    down: 180,
    left: 270,
    "up-right": 45,
    "down-right": 135,
    "down-left": 225,
    "up-left": 315,
  };

  constructor(private element: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.open = this.visible;
  }

  /** The icon class currently shown on the main button. */
  protected get mainIcon(): string {
    if (this.open) return this.hideIcon ?? this.showIcon ?? GraphSpeedDialComponent.DEFAULT_ICON;
    return this.showIcon ?? GraphSpeedDialComponent.DEFAULT_ICON;
  }

  /** Whether the main icon should be rotated to visually morph into a "close" icon. */
  protected get shouldRotateIcon(): boolean {
    return this.open && this.rotateAnimation && !this.hideIcon;
  }

  /** Toggles the dial open/closed. */
  protected toggle(): void {
    this.open = !this.open;
  }

  /**
   * Runs the selected action's command and closes the dial.
   *
   * @param {GraphSpeedDialItem} item - The action that was selected.
   */
  protected selectItem(item: GraphSpeedDialItem): void {
    item.command?.();
    this.open = false;
  }

  /**
   * Resolves the tooltip options for an action, defaulting to showing the tooltip next to the button (to the right)
   * rather than above/below it, unless the action explicitly configures its own position.
   *
   * @param {GraphSpeedDialItem} item - The action to resolve tooltip options for.
   * @returns {GraphTooltipOptions} - The resolved tooltip options.
   */
  protected getTooltipOptions(item: GraphSpeedDialItem): GraphTooltipOptions {
    return { tooltipPosition: "right", ...item.tooltipOptions };
  }

  /**
   * Computes the inline style (transform/opacity/transition-delay) for a single action button, placing it along an
   * arc (semi-circle/quarter-circle) or a straight line (linear) around the main button.
   *
   * @param {number} index - The index of the action within {@link model}.
   * @returns {Record<string, string>} - The style to apply to the action button.
   */
  protected getItemStyle(index: number): Record<string, string> {
    const count: number = this.model.length;
    const baseAngle: number = GraphSpeedDialComponent.DIRECTION_ANGLES[this.direction] ?? 45;

    // Reversed so the first item in `model` ends up furthest along the arc/line, closest to the last-opened position.
    const positionIndex: number = count - 1 - index;

    let dx: number;
    let dy: number;

    if (this.type === "linear") {
      const distance: number = this.radius * (positionIndex + 1);
      const rad: number = (baseAngle * Math.PI) / 180;
      dx = distance * Math.sin(rad);
      dy = -distance * Math.cos(rad);
    } else {
      const sweep: number = this.type === "semi-circle" ? 180 : 90;
      const angle: number = count <= 1 ? baseAngle : baseAngle - sweep / 2 + (sweep * positionIndex) / (count - 1);
      const rad: number = (angle * Math.PI) / 180;
      dx = this.radius * Math.sin(rad);
      dy = -this.radius * Math.cos(rad);
    }

    return {
      transform: this.open ? `translate(${dx}px, ${dy}px) scale(1)` : "translate(0, 0) scale(0.4)",
      opacity: this.open ? "1" : "0",
      "pointer-events": this.open ? "auto" : "none",
    };
  }

  /**
   * Angular `trackBy` function to optimize `*ngFor` rendering of actions.
   *
   * @param {number} index - The index of the item.
   * @returns {number} - The index.
   */
  protected trackByIdx(index: number): number {
    return index;
  }

  /** Closes the dial when clicking outside of it, if {@link hideOnClickOutside} is enabled. */
  @HostListener("document:mousedown", ["$event"])
  protected onDocumentMouseDown(event: MouseEvent): void {
    if (!this.hideOnClickOutside || !this.open) return;
    if (!this.element.nativeElement.contains(event.target as Node)) this.open = false;
  }
}
