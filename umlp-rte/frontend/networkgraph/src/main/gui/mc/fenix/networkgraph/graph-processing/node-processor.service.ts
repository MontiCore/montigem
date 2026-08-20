import { Injectable } from "@angular/core";
import { EdgeAttributes, Internal_GraphNode, NodeAttributes, NodePrograms } from "../types/graph.types";
import { Attributes, SerializedNode } from "graphology-types";
import { ColorUtil, HEX } from "../shared/utils/color.util";
import { GroupInformation } from "../types/filter-group.type";
import { PathUtil } from "../shared/utils/path.util";
import { ProcessorServiceTemplate } from "./processor-service-template.service";
import { GroupMappingService } from "./group-mapping.service";

/**
 * The `NodeProcessorService` extends `ProcessorServiceTemplate` and is designed to convert raw node data into a format
 * understandable by `SigmaJS`. It inherits core functionalities from the template while introducing mechanisms to
 * handle node-specific operations. By preprocessing the data, we allow for more flexibility with respect to naming and
 * structure of the input data. For example, the group of a node can be specified by "group", "category", "type", and
 * "tag". The service also handles the generation of unique identifiers for nodes (i.e., if no key has been specified).
 *
 * @template N - Type of the Node Attributes.
 * @template E - Type of the Edge Attributes.
 * @template G - Type of the Graph Attributes.
 */
@Injectable()
export class NodeProcessorService<
  N extends NodeAttributes,
  E extends EdgeAttributes,
  G extends Attributes,
> extends ProcessorServiceTemplate<N, E, G> {
  /**
   * Holds the raw edge data received as input. The data format is adapted to allow for flexibility in the naming
   * conventions and structure of graphs.
   */
  public rawNodeData: Internal_GraphNode[] | null = null;

  /** Counter utilized for generating unique identifiers for edges. */
  private nodeIdCounter: number = 0;

  constructor(private groupMapping: GroupMappingService<N, E, G>) {
    super();
  }

  /** Processes raw node data to fit the graph model. */
  public processNodeData(): void {
    this.reset();
    let largestNodeSize: number = this.graphSettings.getSetting("defaultNodeSize");
    const duplicatedNodeKeys: string[] = [];

    // Initialize the processed nodes array
    this.data.processedNodes = [];

    for (const node of this.rawNodeData ?? []) {
      const key: string = this.generateNodeIdentifier(node);

      // Check for duplicate node keys
      if (this.data.uniqueNodeKeySet.has(key)) {
        duplicatedNodeKeys.push(key);
        continue;
      }

      // Keep track of the unique node keys
      this.data.uniqueNodeKeySet.add(key);

      // Create the node
      const serializedNode: SerializedNode<N> = this.createNode(key, node);

      // Extract UI Information from the serialized node (for other UI components
      this.extractUIInformation(serializedNode);

      // Keep track of the largest node (for the stage padding)
      largestNodeSize = Math.max(largestNodeSize, serializedNode.attributes?.size || 0);

      // Push the processed node
      this.data.processedNodes.push(serializedNode);
    }

    // Update the largest node size
    this.graphSettings.setLargestNodeSize(largestNodeSize);

    this.logNodeWarnings(duplicatedNodeKeys);
  }

  public reset(): void {
    this.data.resetNodeData();
    this.nodeIdCounter = 0;
  }

  /**
   * Generates a unique identifier for a given node.
   *
   * @param {Internal_GraphNode} node - The node for which a unique identifier is to be generated.
   * @returns {string} - A unique identifier for the given node.
   */
  private generateNodeIdentifier(node: Internal_GraphNode): string {
    const [identifier, newCounter] = ProcessorServiceTemplate.generateIdentifier(
      this.nodeIdCounter,
      this.data.uniqueNodeKeySet,
      node,
      "node"
    );
    this.nodeIdCounter = newCounter;
    return identifier;
  }

  /**
   * Converts a node of type {@link Internal_GraphNode} into a {@link SerializedNode} that is suitable for `SigmaJS`.
   *
   * @param {string} key - The key of the node
   * @param {Internal_GraphNode} node - The node to construct a graph node from.
   * @returns {SerializedNode} - The constructed node.
   */
  private createNode(key: string, node: Internal_GraphNode): SerializedNode<N> {
    const imageLink: string | undefined = ProcessorServiceTemplate.getAttribute(node, "image");
    const nodeProgram: NodePrograms | undefined =
      ProcessorServiceTemplate.getAttribute(node, "nodeProgram") ??
      (imageLink ? "image" : this.graphSettings.getSetting("defaultNodeProgram"));
    const group: string = this.groupMapping.getNodeGroup(node);
    this.groupMapping.addNodeGroup(key, group);

    const x: number = ProcessorServiceTemplate.getAttribute(node, "x") ?? 0;
    const y: number = ProcessorServiceTemplate.getAttribute(node, "y") ?? 0;
    return {
      key,
      attributes: {
        ...this.getCommonProperties(node, key),
        size: ProcessorServiceTemplate.getAttribute(node, "size") ?? this.graphSettings.getSetting("defaultNodeSize"),
        type: nodeProgram,
        group: group,
        image: imageLink ? imageLink : undefined,
        color: node.color? node.color : this.groupMapping.getColorForGroup(group, this.graphSettings.getSetting("defaultNodeColor")),
        fixed: ProcessorServiceTemplate.getAttribute(node, "fixed"),
        x: x,
        y: y,
      } as N,
    };
  }

  /**
   * Function used to extract information for other UI-components such as the Filter-Node Dropdown
   * {@link SearchNodeComponent} and Filter-Group Dropdown {@link FilterGroupComponent}.
   *
   * @param {SerializedNode<N>} node - The node to extract the information from.
   */
  private extractUIInformation(node: SerializedNode<N>): void {
    const groupColor: HEX = ColorUtil.parseIntoHEX(
      node.attributes?.color ?? this.graphSettings.getSetting("defaultNodeColor")
    );
    const key: string = node.key;
    const group: string = node.attributes?.group ?? this.unknownGroupName;
    const label: string | undefined = node.attributes?.label ?? undefined;

    const existingEntry: GroupInformation = this.data.groupMemberCountMap.get(group) ?? {
      count: 0,
      groupColor: groupColor,
    };
    existingEntry.count += 1;
    this.data.groupMemberCountMap.set(group, existingEntry);
    this.data.nodeSearchInformation.push({ searchLabel: label, group: group, groupColor: groupColor, nodeKey: key });
  }

  /**
   * Logs any warnings related to nodes.
   *
   * @param {string[]} duplicatedNodeKeys - An array of duplicated node keys.
   */
  private logNodeWarnings(duplicatedNodeKeys: string[]): void {
    const warnings: string[] = [];
    if (duplicatedNodeKeys.length > 0) {
      warnings.push(`Duplicated node key(s) detected. Skipping node(s) with keys: ${duplicatedNodeKeys.join(", ")}`);
    }

    if (warnings.length > 0) console.warn(warnings.join(" "));
  }
}
