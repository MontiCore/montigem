// metric-information.component.ts
import { ChangeDetectionStrategy, Component, HostListener, Input, ViewChild } from "@angular/core";
import { MetricInformation } from "../types/graph.types";
import { GraphTooltipOptions } from "../shared/types/tooltip.types";
import { GraphPopoverComponent } from "../shared/ui/graph-popover/graph-popover.component";
import { GraphTooltipDirective } from "../shared/directives/graph-tooltip.directive";
import { FormatValuePipe } from "../shared/pipes/value-formatter.pipe";
import { ObjectKeysPipePipe } from "../shared/pipes/object-keys.pipe";
import { CommonModule } from "@angular/common";
import { GraphManagerService } from "../data/graph-manager.service";

/** MetricInformationComponent displays metric information in an overlay panel. */
@Component({
  selector: "graph-metric-information",
  standalone: true,
  templateUrl: "./metric-information.component.html",
  styleUrls: [],
  imports: [CommonModule, GraphPopoverComponent, GraphTooltipDirective, FormatValuePipe, ObjectKeysPipePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MetricInformationComponent {
  /** Keys to get the metric information for (defaults to "all keys"). */
  @Input() metricKeys: string[] = [];

  /** Optional tooltip options for the information button. */
  @Input() public tooltipOptions?: GraphTooltipOptions;

  /** Metric information to display. */
  protected metricInformation: MetricInformation = {};

  /** Reference to the popover panel. */
  @ViewChild("overlayPanel") private overlayPanel: GraphPopoverComponent | null = null;

  constructor(private graphManager: GraphManagerService) {
    this.metricKeys = this.graphManager.graphMetrics.getAllInformationMetricKeys();
  }

  /**
   * Toggles the overlay panel.
   *
   * @param {GraphPopoverComponent} overlayPanel - The overlay panel to toggle
   * @param {MouseEvent} event - The mouse event
   */
  toggleOverlayPanel(overlayPanel: GraphPopoverComponent, event: MouseEvent): void {
    overlayPanel.toggle(event);
  }

  /**
   * `trackBy` function for *ngFor directive to optimize rendering.
   *
   * @param {number} index - Index of the metric key
   * @param {string} value - Value of the metric key
   * @returns {number} The same index
   */
  public trackByIndex(index: number, value: string): number {
    return index;
  }

  protected updateMetricInformation(): void {
    this.metricInformation = this.graphManager.graphMetrics.getMetricInformation(this.metricKeys);
  }

  /** Hides the overlay panel when the escape key is pressed. */
  @HostListener("document:keydown.escape")
  public handleKeyboardEvent(): void {
    this.overlayPanel?.hide();
  }
}
