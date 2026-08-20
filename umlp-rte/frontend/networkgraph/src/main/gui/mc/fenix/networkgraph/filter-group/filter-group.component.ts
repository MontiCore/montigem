import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { GraphMultiSelectComponent } from "../shared/ui/graph-multi-select/graph-multi-select.component";
import { GraphMultiSelectChangeEvent } from "../shared/ui/graph-multi-select/graph-multi-select.types";
import { GraphTemplateDirective } from "../shared/directives/graph-template.directive";
import { GroupInformation, GroupInformationMap, SortedFilterGroupInformation } from "../types/filter-group.type";
import { CapitalizeFirstLetterPipe } from "../shared/pipes/capitalize-first-letter.pipe";
import { GraphManagerService } from "../data/graph-manager.service";
import { AutoUnsubscribe } from "../shared/decorators/auto-unsubscribe.decorator";

/**
 * FilterGroupComponent is an Angular component designed to display and filter a graph for certai groups. It takes a map
 * of group information and sorts it by group size. The component also handles user interactions for selecting different
 * groups through a multi-select interface.
 */
@Component({
  selector: "graph-filter-group",
  standalone: true,
  imports: [CommonModule, GraphMultiSelectComponent, GraphTemplateDirective, CapitalizeFirstLetterPipe],
  templateUrl: "./filter-group.component.html",
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
@AutoUnsubscribe()
export class FilterGroupComponent {
  /** Currently selected groups. */
  protected selectedGroups: string[] | null = null;

  /** Processed and sorted group information. */
  protected processedGroups: SortedFilterGroupInformation[] | undefined = undefined;

  /** Keep track of the maximum number of items within a group among all groups. */
  private maxItemsWithinGroup: number = Number.NEGATIVE_INFINITY;

  constructor(
    protected cdReference: ChangeDetectorRef,
    private graphManager: GraphManagerService
  ) {
    // Update the groups when the graph is initialized.
    this.graphManager.onSigmaInit$.subscribe(() => {
      this.updateGroups();
      this.cdReference.detectChanges();
    });
    // Update the groups initially.
    this.updateGroups();
  }

  /**
   * Handles changes in multi-select options and emits the updated group selection.
   *
   * @param {GraphMultiSelectChangeEvent} event - MultiSelect change event.
   */
  protected selectGroup(event: GraphMultiSelectChangeEvent): void {
    this.graphManager.showGroupKeys(new Set(event.value));
  }

  /** Updates the groups based on the data */
  private updateGroups(): void {
    const groups: GroupInformationMap = this.graphManager.graphData.getNodeFilterGroups();
    this.processedGroups = groups.size > 0 ? this.sortAndPrepareGroups(groups) : undefined;
    this.selectedGroups = [...(groups?.keys() ?? [])];
  }

  /**
   * Sorts and prepares the group information based on group sizes and other attributes.
   *
   * @param {GroupInformationMap} groups - The groups to sort and prepare.
   * @returns {SortedFilterGroupInformation[]} - An array of sorted and prepared group information.
   */
  private sortAndPrepareGroups(groups: GroupInformationMap): SortedFilterGroupInformation[] {
    return this.sortGroups(groups).map(
      ([key, { count, groupColor }]): SortedFilterGroupInformation => ({
        groupKey: key,
        width: (count / this.maxItemsWithinGroup) * 100,
        count,
        groupColor,
      })
    );
  }

  /**
   * Sorts the groups based on the number of items in each group.
   *
   * @param {GroupInformationMap} groups - The groups to sort.
   * @returns {SortedFilterGroupInformation[]} - An array of sorted and prepared group information.
   */
  private sortGroups(groups: GroupInformationMap): [string, GroupInformation][] {
    return [...groups.entries()].sort((a: [string, GroupInformation], b: [string, GroupInformation]) => {
      this.maxItemsWithinGroup = Math.max(this.maxItemsWithinGroup, a[1].count, b[1].count);
      return b[1].count - a[1].count;
    });
  }
}
