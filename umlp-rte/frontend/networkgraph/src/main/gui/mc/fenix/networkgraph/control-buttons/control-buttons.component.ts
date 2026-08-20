import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { CommonModule } from "@angular/common";
import { GraphTooltipDirective } from "../shared/directives/graph-tooltip.directive";
import { GraphTooltipOptions } from "../shared/types/tooltip.types";
import { GraphSpeedDialComponent } from "../shared/ui/graph-speed-dial/graph-speed-dial.component";
import { GRAPH_SPEED_DIAL_ACTION_DIAMETER_PX, GraphSpeedDialItem } from "../shared/ui/graph-speed-dial/graph-speed-dial.types";
import { GraphManagerService } from "../data/graph-manager.service";
import {
  ControlButton,
  ControlButtonsLayout,
  RegularControlButton,
  SpeedDialConfig,
  ToggleableControlButton,
} from "../types/control-buttons.types";

/**
 * Component that offers a collection of control buttons for the graph visualization. It provides multiple layouts,
 * including vertical, horizontal, and speed dial. The component also supports adding and removing buttons dynamically,
 * along with specifying different actions for them. It uses the `GraphManagerService` to perform actions such as layout
 * controls and zooming functionalities.
 */
@Component({
  selector: "graph-control-buttons",
  templateUrl: "./control-buttons.component.html",
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [CommonModule, GraphTooltipDirective, GraphSpeedDialComponent],
})
export class ControlButtonsComponent implements OnInit {
  /** Event emitter for group selection change. */
  @Output() controlButtonClicked: EventEmitter<ControlButton> = new EventEmitter<ControlButton>();

  /** Computed radius for the speed dial items (when using speed dial layout). */
  public computedRadius: number = 40;

  /** Configuration for control buttons */
  protected controlButtons: ControlButton[] = [];

  /** Items specifically for speed dial layout */
  protected speedDialItems: GraphSpeedDialItem[] = [];

  /** Fallback radius when computation is not possible */
  private fallbackRadius: number = 50;

  constructor(private graphManager: GraphManagerService) {}

  /** Additional buttons that should be added to the control buttons */
  private _extendControlButtons: ControlButton[] = [];

  /**
   * Getter for extendControlButtons
   *
   * @returns {ControlButton[]} - The extended control buttons.
   */
  public get extendControlButtons(): ControlButton[] | undefined {
    return this._extendControlButtons;
  }

  /**
   * Setter for extendControlButtons
   *
   * @param {ControlButton[] | undefined} value - New value to set
   */
  @Input()
  public set extendControlButtons(value: ControlButton[] | undefined) {
    this._extendControlButtons = value || [];
  }

  /** List of keys for buttons that should be removed from the control buttons */
  private _removeControlButtons: string[] = [];

  /**
   * Getter for removeControlButtons
   *
   * @returns {string[]} - The keys of the buttons that should be removed.
   */
  public get removeControlButtons(): string[] | undefined {
    return this._removeControlButtons;
  }

  /**
   * Setter for removeControlButtons
   *
   * @param {string[] | undefined} value - New value to set
   */
  @Input()
  public set removeControlButtons(value: string[] | undefined) {
    this._removeControlButtons = value || [];
  }

  /** Layout type for displaying control buttons */
  private _layout: ControlButtonsLayout = "speedDial";

  /**
   * Getter for layout
   *
   * @returns {ControlButtonsLayout} - The layout type.
   */
  public get layout(): ControlButtonsLayout | undefined {
    return this._layout;
  }

  /**
   * Setter for layout
   *
   * @param {ControlButtonsLayout | undefined} value - New value to set
   */
  @Input()
  public set layout(value: ControlButtonsLayout | undefined) {
    if (value !== undefined) this._layout = value;
  }

  /**
   * Default Configuration for Speed Dial
   *
   * @type {SpeedDialConfig}
   */
  private _speedDialConfig: SpeedDialConfig = {
    visible: false,
    mask: false,
    transitionDelay: 30,
    radius: "auto",
    direction: "up-right",
    type: "quarter-circle",
    rotateAnimation: true,
    hideOnClickOutside: false,
  };

  /**
   * Getter for speedDialConfig
   *
   * @returns {SpeedDialConfig | undefined} - The speed dial configuration.
   */
  public get speedDialConfig(): SpeedDialConfig | undefined {
    return this._speedDialConfig ?? {};
  }

  /**
   * Setter for speedDialConfig
   *
   * @param {SpeedDialConfig | undefined} value - New value to set
   */
  @Input()
  public set speedDialConfig(value: SpeedDialConfig | undefined) {
    if (value !== undefined) this._speedDialConfig = { ...this._speedDialConfig, ...value };
  }

  /** Default options for tooltip */
  private _tooltipOptions: GraphTooltipOptions = {};

  /**
   * Getter for defaultTooltipOptions
   *
   * @returns {GraphTooltipOptions} - The tooltip options.
   */
  public get tooltipOptions(): GraphTooltipOptions | undefined {
    return this._tooltipOptions;
  }

  /**
   * Setter for tooltipOptions
   *
   * @param {GraphTooltipOptions | undefined} value - New value to set
   */
  @Input()
  public set tooltipOptions(value: Partial<GraphTooltipOptions> | undefined) {
    this._tooltipOptions = { ...this._tooltipOptions, ...value };
  }

  /**
   * Checks if the layout is either 'vertical' or 'horizontal'
   *
   * @returns {boolean} - True if the layout is vertical or horizontal, otherwise false
   */
  public get isVerticalOrHorizontalLayout(): boolean {
    return this.layout === "horizontal" || this.layout === "vertical";
  }

  /**
   * Checks if the layout is 'speedDial'
   *
   * @returns {boolean} - True if the layout is speedDial, otherwise false
   */
  public get isSpeedDialLayout(): boolean {
    return this.layout === "speedDial";
  }

  /**
   * Computes and returns the radius of the speed dial. This is to ensure that all speed dial items fit properly within
   * the specified layout. If the radius is set to "auto" in SpeedDialConfig, it uses the SpeedDialSizeReference to
   * compute the optimal radius.
   *
   * @returns {number} - The computed radius based on the layout and number of speed dial items, or the fallback radius
   *   if computation is not possible.
   */
  public getRadius(): number {
    const configuredRadius = this._speedDialConfig.radius;

    if (configuredRadius !== "auto") {
      return configuredRadius ?? this.fallbackRadius;
    }

    const itemDiameter = GRAPH_SPEED_DIAL_ACTION_DIAMETER_PX;
    const itemCount = this.speedDialItems.length;

    if (itemCount <= 1) {
      return itemDiameter;
    }

    let totalAngle: number;

    switch (this._speedDialConfig.type) {
      case "linear":
        return this.fallbackRadius;

      case "quarter-circle":
        totalAngle = Math.PI / 2;
        break;

      case "semi-circle":
        totalAngle = Math.PI;
        break;

      default:
        console.warn(
          `Unsupported layout type: ${this._speedDialConfig.type}`
        );
        return this.fallbackRadius;
    }

    // Angle between neighbouring items.
    const angle = totalAngle / Math.max(itemCount - 1, 1);

    // Chord length between neighbouring buttons:
    // chord = 2 * r * sin(angle / 2)
    //
    // Require chord >= button diameter.
    const radius = itemDiameter / (2 * Math.sin(angle / 2));

    return Math.max(radius, itemDiameter);
  }

  /** Initialize the control buttons on component initialization */
  public ngOnInit(): void {
    this.initControlButtons();
  }

  /** Handles click events and updates the computed radius */
  protected handleClick(): void {
    this.computedRadius = this.getRadius();
  }

  /**
   * Handles button clicks and performs actions based on the type of button
   *
   * @param {ControlButton} button - The button that was clicked
   */
  protected handleButtonClick(button: ControlButton): void {
    if (button.isToggleable) {
      this.toggleAction(button as ToggleableControlButton);
    } else {
      this.performAction(button);
    }
  }

  /**
   * Function to track the control buttons by their index (for `ngFor` inside the template)
   *
   * @param {number} index - The index of the item
   * @returns {number} - The index
   */
  protected trackByIdx(index: number): number {
    return index;
  }

  /** Updates speed dial items based on control buttons */
  private updateSpeedDialItems(): void {
    this.speedDialItems = this.transformToSpeedDialItems(this.controlButtons);
  }

  /**
   * Transforms control buttons into speed dial items
   *
   * @param {ControlButton[]} controlButtons - The control buttons to transform.
   * @returns {GraphSpeedDialItem[]} - The resulting array of speed dial items.
   */
  private transformToSpeedDialItems(controlButtons: ControlButton[]): GraphSpeedDialItem[] {
    return controlButtons.map((button: ControlButton): GraphSpeedDialItem => {
      const updatedAttributes: Partial<ControlButton> = this.getUpdatedButtonAttributes(button);
      return {
        tooltipOptions: {
          tooltipLabel: updatedAttributes.tooltip,
          ...this.getTooltipOptions(button),
        },
        icon: updatedAttributes.icon,
        command: () => this.handleButtonClick(button),
      };
    });
  }

  /** Updates control buttons based on `extendControlButtons` and `removeControlButtons` */
  private updateControlButtons(): void {
    this.controlButtons = [
      ...this.controlButtons.filter((button: ControlButton) => !this.removeControlButtons?.includes(button.key)),
      ...(this.extendControlButtons ?? []),
    ].map((button: ControlButton): ControlButton => {
      return { ...button, ...this.getUpdatedButtonAttributes(button) };
    });
  }

  /**
   * Retrieves tooltip options for a given button
   *
   * @param {ControlButton} button - The button for which to get tooltip options
   * @returns {GraphTooltipOptions} - The tooltip options
   */
  private getTooltipOptions(button: ControlButton): GraphTooltipOptions {
    return button.tooltipOptions || this.tooltipOptions || {};
  }

  /**
   * Toggles the state of a toggleable control button
   *
   * @param {ToggleableControlButton} button - The button to toggle
   */
  private toggleAction(button: ToggleableControlButton): void {
    button.toggled = !button.toggled;
    button.icon = button.toggled ? button.onIcon : button.offIcon;
    button.tooltip = button.toggled ? button.onTooltip : button.offTooltip;
    this.performAction(button);
  }

  /**
   * Performs the action associated with a regular control button
   *
   * @param {RegularControlButton} button - The button for which to perform the action
   */
  private performAction(button: RegularControlButton): void {
    button.actionFn?.(button, this.graphManager);
  }

  /**
   * Checks if a button is toggleable
   *
   * @param {ControlButton} button - The button to check
   * @returns {button is ToggleableControlButton} - True if the button is toggleable, otherwise false
   */
  private isToggleableButton(button: ControlButton): button is ToggleableControlButton {
    return "isToggleable" in button;
  }

  /**
   * Retrieves the current state (tooltip and icon) of a toggleable button
   *
   * @param {ToggleableControlButton} button - The toggleable button
   * @returns {{ tooltip: string; icon: string }} - The current state
   */
  private getToggleableButtonState(button: ToggleableControlButton): { tooltip: string; icon: string } {
    return button.toggled
      ? { tooltip: button.onTooltip, icon: button.onIcon }
      : { tooltip: button.offTooltip, icon: button.offIcon };
  }

  /**
   * Retrieves and updates the attributes (icon and tooltip) for a given ControlButton.
   *
   * @param {ControlButton} button - The button whose attributes need to be updated.
   * @returns {Partial<ControlButton>} - Partial object containing updated icon and tooltip.
   */
  private getUpdatedButtonAttributes(button: ControlButton): Partial<ControlButton> {
    if (this.isToggleableButton(button)) {
      const state = this.getToggleableButtonState(button);
      return { icon: state.icon, tooltip: state.tooltip };
    }
    return { icon: button.icon, tooltip: button.tooltip };
  }

  /** Initializes control buttons and their state */
  private initControlButtons(): void {
    this.initializeButtonState();
    this.updateControlButtons();
    this.updateSpeedDialItems();
  }

  /** Initializes the default state of buttons */

  private initializeButtonState(): void {
    this.controlButtons = [
      {
        key: "stopLayout",
        actionFn: (button: ControlButton, graphManager: GraphManagerService): void => graphManager.stopLayouting(),
        tooltip: "Stop Layouting",
        icon: "pi pi-stop-circle",
      },
      {
        key: "continueLayout",
        actionFn: (button: ControlButton, graphManager: GraphManagerService): void => graphManager.continueLayouting(),
        tooltip: "Continue Layouting",
        icon: "pi pi-play",
      },
      {
        key: "restartLayout",
        actionFn: (button: ControlButton, graphManager: GraphManagerService): void => graphManager.restartLayout(),
        tooltip: "Restart Layouting",
        icon: "pi pi-sync",
      },
      {
        key: "zoomIn",
        actionFn: (button: ControlButton, graphManager: GraphManagerService): void => graphManager.zoomIn(),
        tooltip: "Zoom In",
        icon: "pi pi-search-plus",
      },
      {
        key: "zoomOut",
        actionFn: (button: ControlButton, graphManager: GraphManagerService): void => graphManager.zoomOut(),
        tooltip: "Zoom Out",
        icon: "pi pi-search-minus",
      },
      {
        key: "resetZoom",
        actionFn: (button: ControlButton, graphManager: GraphManagerService): void => graphManager.resetZoom(),
        tooltip: "Reset Zoom",
        icon: "pi pi-refresh",
      },
      {
        key: "toggleFullscreen",
        actionFn: (button: ControlButton, graphManager: GraphManagerService): void => {
          if ((button as ToggleableControlButton).toggled) graphManager.requestFullscreen();
          else graphManager.exitFullscreen();
        },
        isToggleable: true,
        toggled: false,
        onTooltip: "Escape Fullscreen",
        offTooltip: "View Fullscreen",
        onIcon: "pi pi-window-minimize",
        offIcon: "pi pi-window-maximize",
      },
    ];
  }
}
