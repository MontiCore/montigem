import { GraphTooltipOptions } from "../../types/tooltip.types";

/**
 * The rendered diameter (in px) of a single speed-dial action button. Kept in sync with `.graph-speed-dial-action`'s
 * `width`/`height` in `graph-speed-dial.component.scss`. Used by `ControlButtonsComponent.getRadius` to compute an
 * "auto" radius without needing to measure a real DOM element (which would require piercing this component's view
 * encapsulation).
 */
export const GRAPH_SPEED_DIAL_ACTION_DIAMETER_PX: number = 40;

/** A single action rendered by {@link GraphSpeedDialComponent}. Replaces PrimeNG's `MenuItem` for this use case. */
export interface GraphSpeedDialItem {
  /** Icon CSS class(es) for the action button (e.g. "pi pi-plus"). */
  icon?: string;

  /** Tooltip shown when hovering the action button. */
  tooltipOptions?: GraphTooltipOptions;

  /** Invoked when the action is selected. */
  command?: () => void;
}
