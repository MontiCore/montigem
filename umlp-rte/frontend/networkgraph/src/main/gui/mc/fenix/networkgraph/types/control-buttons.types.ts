import { GraphManagerService } from "../data/graph-manager.service";
import { GraphTooltipOptions } from "../shared/types/tooltip.types";

/** Defines the possible layouts for control buttons. */
export type ControlButtonsLayout = "vertical" | "horizontal" | "speedDial";

/** Represents a regular control button configuration. */
export interface RegularControlButton {
  /** Unique identifier for the button */
  key: string;

  /** Tooltip text displayed when hovering over the button */
  tooltip?: string;

  /** Icon class for the button */
  icon?: string;

  /**
   * Function executed when the button is clicked.
   *
   * @param {ControlButton} button - The clicked button
   * @param {GraphManagerService} graphManager - The graph manager service
   */
  actionFn?: (button: ControlButton, graphManager: GraphManagerService) => void;

  /** Specifies whether the button is toggleable or not */
  isToggleable?: boolean;

  /** Custom options for the tooltip */
  tooltipOptions?: GraphTooltipOptions;
}

/** Represents a toggleable control button configuration. */
export interface ToggleableControlButton extends RegularControlButton {
  /** Tooltip text when the button is in the 'on' state */
  onTooltip: string;

  /** Tooltip text when the button is in the 'off' state */
  offTooltip: string;

  /** Icon when the button is in the 'on' state */
  onIcon: string;

  /** Icon when the button is in the 'off' state */
  offIcon: string;

  /** Indicates whether the button is in the 'on' or 'off' state */
  toggled?: boolean;
}

/** A ControlButton can either be a RegularControlButton or a ToggleableControlButton. */
export type ControlButton = RegularControlButton | ToggleableControlButton;

/** Configuration options for the speed dial layout. */
export interface SpeedDialConfig {
  /** Whether the speed dial is visible or not */
  visible?: boolean;

  /** Whether to display a mask behind the speed dial */
  mask?: boolean;

  /** Transition delay between speed dial items in milliseconds */
  transitionDelay?: number;

  /** Radius of the speed dial circle. Can be a number or 'auto' for automatic calculation */
  radius?: number | "auto";

  /** Direction of the speed dial items */
  direction?: "up" | "down" | "left" | "right" | "up-left" | "up-right" | "down-left" | "down-right";

  /** Shape of the speed dial layout */
  type?: "linear" | "semi-circle" | "quarter-circle";

  /** Whether to hide the speed dial when clicking outside of it */
  hideOnClickOutside?: boolean;

  /** Custom CSS class for the speed dial */
  className?: string;

  /** Whether to rotate the speed dial button during opening/closing */
  rotateAnimation?: boolean;

  /** Custom CSS class for the speed dial button */
  buttonClassName?: string;

  /** Custom CSS class for the speed dial mask */
  maskClassName?: string;

  /** Icon class for the 'show' state of the speed dial button */
  showIcon?: string;

  /** Icon class for the 'hide' state of the speed dial button */
  hideIcon?: string;
}
