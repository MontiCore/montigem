/** Configuration options for the {@link GraphTooltipDirective}. */
export interface GraphTooltipOptions {
  /** The text to display. Takes precedence over the directive's main input value if set. */
  tooltipLabel?: string;

  /** The side of the host element on which the tooltip should be displayed. */
  tooltipPosition?: "top" | "bottom" | "left" | "right";

  /** Delay (in ms) before the tooltip is shown after the pointer enters the host element. */
  showDelay?: number;

  /** Delay (in ms) before the tooltip is hidden after the pointer leaves the host element. */
  hideDelay?: number;

  /** Whether the tooltip is disabled. */
  disabled?: boolean;
}
