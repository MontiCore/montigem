import { ChangeDetectionStrategy, ChangeDetectorRef, Component, effect } from "@angular/core";
import { CommonModule } from "@angular/common";
import { GraphSelectComponent } from "../shared/ui/graph-select/graph-select.component";
import { GraphSelectChangeEvent } from "../shared/ui/graph-select/graph-select.types";
import { GraphTemplateDirective } from "../shared/directives/graph-template.directive";
import { NodeKeyFormatterPipe } from "./node-key-formatter.pipe";
import { SearchInformation, SearchInformationArray } from "../types/filter-group.type";
import { GraphManagerService } from "../data/graph-manager.service";
import { AutoUnsubscribe } from "../shared/decorators/auto-unsubscribe.decorator";

/** SearchNodeComponent is a component for performing searches on graph nodes via a searchable dropdown. */
@Component({
  selector: "graph-search-node",
  standalone: true,
  imports: [CommonModule, GraphSelectComponent, GraphTemplateDirective, NodeKeyFormatterPipe],
  templateUrl: "./search-node.component.html",
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
@AutoUnsubscribe()
export class SearchNodeComponent {
  /** The array of nodes that will be displayed in the dropdown. */
  protected searchNodes: SearchInformationArray = [];

  /** Visible Nodes */
  protected visibleSearchNodes: SearchInformationArray = [];

  constructor(
    protected graphManager: GraphManagerService,
    protected cdReference: ChangeDetectorRef
  ) {
    effect((): void => {
      // Access the signal so that we can detect changes properly and update the search information.
      this.graphManager.graphData.selectedNode;
      this.updateSearchNodes();
    });
    // Update the search information when the graph is initialized.
    this.graphManager.onSigmaInit$.subscribe(() => {
      this.updateSearchNodes();
      this.cdReference.detectChanges();
    });

    // Update the search information initially.
    this.updateSearchNodes();
  }

  /**
   * Handles the event when a node is selected from the dropdown. Emits the selected node's key through the
   * `nodeSelectionChange` event emitter.
   *
   * @param {GraphSelectChangeEvent} event - The select change event that triggered the selection.
   */
  public nodeSelected(event: GraphSelectChangeEvent): void {
    this.graphManager.zoomToNode(event.value);
    this.graphManager.setSelectedNode(event.value);
  }

  /**
   * Angular trackBy function to optimize ngFor performance by using unique identifiers for elements.
   *
   * @param {number} index - The index of the element in the array.
   * @param {SearchInformation} item - The actual node item.
   * @returns {string} - A unique identifier for each node.
   */
  protected trackByFn(index: number, item: SearchInformation): string {
    return item.nodeKey;
  }

  /**
   * Updates the node search information based on the data and filter for visible nodes based on the hidden attribute of
   * the nodes.
   */
  protected updateSearchNodes(): void {
    this.searchNodes = this.graphManager.graphData.getNodeSearchInformation() ?? [];
    this.visibleSearchNodes = this.searchNodes.filter(
      (node: SearchInformation) => !this.graphManager.graph?.getNodeAttribute(node.nodeKey, "hidden")
    );
  }
}
