import { effect, Injectable, NgZone, OnDestroy, signal, untracked, WritableSignal } from "@angular/core";
import { SigmaGraphService } from "./sigma-graph.service";
import {
  AnimateZoomOptions,
  EdgeAttributes,
  GenerateMetricsFunction,
  Internal_Graph,
  Internal_GraphEdge,
  Internal_GraphNode,
  NodeAttributes,
} from "../types/graph.types";
import { Attributes, GraphType, SerializedGraph } from "graphology-types";
import { GraphDataService } from "./graph-data.service";
import { Observable, Subject } from "rxjs";
import { Sigma } from "sigma"
import { SigmaEdgeEventPayload, SigmaNodeEventPayload } from "sigma/types";
import Graph from "graphology";
import { CameraState, Coordinates, MouseCoords, NodeDisplayData } from "sigma/types";
import { ComplexGraphLayoutConfiguration, InitialGraphLayoutConfiguration, InitialGraphLayouts } from "../types/graph-layout.types";
import { GraphSettings, prettyNodeHoverConfiguration } from "../types/graph-settings.types";
import { GraphProcessorService } from "../graph-processing/graph-processor.service";
import { GraphLayoutService } from "../services/graph-layout.service";
import { GraphMetricsService } from "../services/graph-metrics.service";
import { GraphSettingsService } from "../services/graph-settings.service";
import { AnimateOptions } from "sigma/utils";
import { Settings } from "sigma/settings";

/**
 * <h1>GraphManagerService</h1>
 * <p>Serves as a hub for handling and managing all graph-related operations within the
 * application. Interfaces with Sigma.js for rendering graphs and Graphology for managing the graph data
 * structure.</p>
 * <h2>Key Responsibilities:</h2><ul>
 * <li><b>Graph Data Management:</b>
 *   <ul>
 *     <li>Manages graph data, including nodes and edges.</li>
 *     <li>Interacts with GraphDataService for data retrieval.</li>
 *     <li>Interacts with GraphProcessorService for data processing.</li>
 *     <li>Handles serialization and deserialization for state preservation and retrieval.</li>
 *   </ul>
 * </li>
 * <li><b>Graph Rendering:</b>
 *   <ul>
 *     <li>Manages the rendering process using Sigma.js.</li>
 *     <li>Handles the initialization and configuration of the Sigma renderer.</li>
 *     <li>Provides methods for zooming, panning, and other view manipulations.</li>
 *   </ul>
 * </li>
 * <li><b>Graph Interaction:</b>
 *   <ul>
 *     <li>Manages user interactions like node/edge selection, node dragging, and hover actions.</li>
 *     <li>Exposes methods and events for programmatic and UI interactions.</li>
 *   </ul>
 * </li>
 * <li><b>Graph Layout:</b>
 *   <ul>
 *     <li>Manages layout configurations.</li>
 *     <li>Provides methods to apply, update, and manage graph layouts.</li>
 *   </ul>
 * </li>
 * <li><b>Graph Metrics:</b>
 *   <ul>
 *     <li>Manages the computation and application of graph metrics.</li>
 *     <li>Interacts with GraphMetricsService for metrics computation.</li>
 *   </ul>
 * </li>
 * <li><b>Graph Settings:</b>
 *   <ul>
 *     <li>Manages global and local settings.</li>
 *     <li>Provides methods to update and retrieve settings.</li>
 *   </ul>
 * </li>
 * <li><b>Event Handling:</b>
 *   <ul>
 *     <li>Offers a robust event-handling system to capture and respond to various graph events.</li>
 *   </ul>
 * </li>
 * <li><b>Various Utilities:</b>
 *   <ul>
 *     <li>Includes utility methods for operations like fullscreen toggle, zoom reset, etc.</li>
 *   </ul>
 * </li>
 * </ul>
 *
 * @template N - Type of the Node Attributes
 * @template E - Type of the Edge Attributes
 * @template G - Type of the Graph Attributes
 */
@Injectable()
export class GraphManagerService<
  N extends NodeAttributes = NodeAttributes,
  E extends EdgeAttributes = EdgeAttributes,
  G extends Attributes = Attributes,
> implements OnDestroy
{
  /** Subject to emit the renderer after Sigma has been initialized. */
  private sigmaInitialized: Subject<Sigma<NodeAttributes, EdgeAttributes>> = new Subject();

  /** Observable that emits the renderer Sigma has been initialized. */
  public onSigmaInit$: Observable<Sigma<NodeAttributes, EdgeAttributes>> = this.sigmaInitialized.asObservable();

  /** Subject to emit the renderer after the graph has been initialized (see {@link initializeChart}). */
  private graphInitialized: Subject<Sigma<NodeAttributes, EdgeAttributes>> = new Subject();

  /** Observable that emits the the renderer after the graph has been initialized (see {@link initializeChart}). */
  public graphInitialized$: Observable<Sigma<NodeAttributes, EdgeAttributes>> =
    this.graphInitialized.asObservable();

  /** Signal to indicate whether the graph needs initialization. */
  private needsInitializationSignal: WritableSignal<boolean> = signal(true);

  /** Signal to indicate whether the container has been initialized properly. */
  private containerReadySignal: WritableSignal<boolean> = signal(false);

  /** Represents the container element for the graph. */
  private container: HTMLDivElement | null = null;

  /** Reprensents the custom bounding box */
  private customBoundingBox : {x:[number,number], y:[number,number] }| null = null;

  constructor(
    private ngZone: NgZone,
    public sigmaGraph: SigmaGraphService<N, E, G>,
    public graphData: GraphDataService<N, E, G>,
    public graphProcessor: GraphProcessorService<N, E, G>,
    public graphMetrics: GraphMetricsService<N, E, G>,
    public layoutService: GraphLayoutService<N, E, G>,
    public graphSettings: GraphSettingsService
  ) {
    // Create effects to properly react to signal changes
    effect(() => this.initializeChartIfNeeded(), { allowSignalWrites: true });
    effect(() => this.handleNodeHover(this.graphData.hoveredNode));
  }

  /**
   * Gets the graph instance.
   *
   * @returns {Graph<N, E, G> | null} - Graph instance
   */
  public get graph(): Graph<N, E, G> | null {
    return this.sigmaGraph.graph;
  }

  /**
   * Gets the renderer instance.
   *
   * @returns {Sigma<N, E, G> | null} - Renderer instance
   */
  public get renderer(): Sigma<N, E, G> | null {
    return this.sigmaGraph.renderer;
  }

  /**
   * Gets the state of whether the graph needs initialization.
   *
   * @returns {boolean} - The state of whether the graph needs initialization.
   */
  public get needsInitializationSignalTracked(): boolean {
    return this.needsInitializationSignal();
  }

  /**
   * Gets the state of whether the graph needs initialization.
   *
   * @returns {boolean} - The state of whether the graph needs initialization.
   */
  public get needsInitializationSignalUntracked(): boolean {
    return untracked(() => this.needsInitializationSignal());
  }

  /** Continue the layouting process */
  public continueLayouting(): void {
    if (!this.graph) return;
    this.releaseDraggingBBox();
    this.layoutService.continueLayout();
  }

  /** Stop the layouting process */
  public stopLayouting(): void {
    this.layoutService.stopLayouting();
  }

  /**
   * Zooms in on the graph.
   *
   * @param {AnimateZoomOptions} options - Options for the zoom animation.
   */
  public zoomIn(options?: AnimateZoomOptions): void {
    this.sigmaGraph.animatedZoom(options);
  }

  public restartLayout(): void {
    this.applyGraphLayout();
  }

  /**
   * Zooms out of the graph.
   *
   * @param {AnimateZoomOptions} options - Options for the zoom animation.
   */
  public zoomOut(options?: AnimateZoomOptions): void {
    this.sigmaGraph.animatedUnzoom(options);
  }

  /**
   * Resets the zoom back to default so that the entire graph is visible.
   *
   * @param {Partial<AnimateOptions>} options - Options for the reset animation.
   */
  public resetZoom(options?: Partial<AnimateOptions>): void {
    this.sigmaGraph.resetZoom(options);
    // Reset highlighted node
    this.highlightNode(null);
  }

  /**
   * Zooms the camera view to the specified node and highlights it.
   *
   * @param {string | null} nodeKey - The key identifying the node to zoom to. Pass null to reset zoom and highlight.
   * @param {Partial<AnimateOptions>} options - Options for the zoom animation.
   */
  public zoomToNode(nodeKey: string | null, options?: Partial<AnimateOptions>): void {
    this.highlightNode(nodeKey);

    // Don't zoom when nodeKey is null; only reset the highlighted node.
    if (!nodeKey) return;

    const nodeDisplayData: NodeDisplayData | undefined = this.renderer?.getNodeDisplayData(nodeKey);

    if (nodeDisplayData) this.sigmaGraph.animateCamera(nodeDisplayData, options);
  }

  /** Try to open the graph in fullscreen mode. */
  public requestFullscreen(): void {
    this.renderer?.getContainer().parentElement?.requestFullscreen();
  }

  /** Exit fullscreen mode */
  public exitFullscreen(): void {
    document.exitFullscreen();
  }

  /**
   * Highlights the specified node in the graph.
   *
   * @param {string | null} nodeKey - The key identifying the node to be highlighted. Pass null to remove the highlight.
   */
  public highlightNode(nodeKey: string | null): void {
    this.graphData.highlightNode = nodeKey;
  }

  /**
   * Sets the node as selected in the graph.
   *
   * @param {string | null} nodeKey - The key identifying the node to be selected. Pass null to deselect.
   */
  public setSelectedNode(nodeKey: string | null): void {
    this.graphData.selectedNode = nodeKey;
  }

  /**
   * Sets the edge as selected in the graph.
   *
   * @param {string | null} edgeKey - The key identifying the edge to be selected. Pass null to deselect.
   */
  public setSelectedEdge(edgeKey: string | null): void {
    this.graphData.selectedEdge = edgeKey;
  }

  /**
   * Sets the node as being dragged in the graph.
   *
   * @param {string | null} nodeKey - The key identifying the node to be dragged. Pass null to stop dragging.
   */
  public setDraggedNode(nodeKey: string | null): void {
    this.graphData.draggedNode = nodeKey;
  }

  /**
   * Sets the node as hovered in the graph.
   *
   * @param {string | null} key - The key identifying the node to be hovered. Pass null to remove hover state.
   */
  public setHoveredNode(key: string | null): void {
    this.graphData.hoveredNode = key;
  }

  /**
   * Sets the edge as hovered in the graph.
   *
   * @param {string | null} key - The key identifying the edge to be hovered. Pass null to remove hover state.
   */
  public setHoveredEdge(key: string | null): void {
    this.graphData.hoveredEdge = key;
  }

  /**
   * @param {Set<string> | string[] | null} groupKeys - A set or array of group keys to show. Updates each node's
   *   attributes to set the 'hidden' property based on whether the node's group is in the provided set or array of
   *   group keys.
   */
  public showGroupKeys(groupKeys: Set<string> | string[] | null): void {
    this.graphData.showGroupKeys(groupKeys);
  }

  /**
   * Allows updating the initial layout configuration.
   *
   * @param {InitialGraphLayoutConfiguration | null} layout - The initial layout configuration.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `false`
   */
  public updateInitialLayout(layout: InitialGraphLayoutConfiguration | null, reload: boolean = false): void {
    this.graphData.initialLayoutConfiguration = layout;
    if (reload) this.reload();
  }

  /**
   * Updates the layout with the given layout.
   *
   * @remarks
   *   For complex layouts, the graph needs to be initialized with proper x-y-coordinates in advance to properly work.
   *   Thus, it is recommended to use this function in combination with a suitable initial layout (see
   *   {@link updateInitialLayout}).
   * @param {ComplexGraphLayoutConfiguration | null} layout - The complex layout configuration.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `false`
   */
  public updateLayoutConfiguration(layout: ComplexGraphLayoutConfiguration | null, reload: boolean = false): void {
    this.graphData.complexLayoutConfiguration = layout;
    if (reload) this.reload();
  }

  /**
   * Update the Graph Type
   *
   * @param {GraphType} type - The graph type.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `false`
   */
  public updateGraphType(type: GraphType, reload: boolean = false): void {
    const wasUpdated: boolean = this.graphSettings.updateSetting("graphType", type);
    if (reload && wasUpdated) this.reload();
  }

  /**
   * Update whether duplicated edges should be ignored or not when processing the edges.
   *
   * @param {boolean} ignoreDuplicateEdges - Whether to ignore duplicated edges nor not.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `false`
   */
  public updateIgnoreDuplicatedEdges(ignoreDuplicateEdges: boolean, reload: boolean = false): void {
    const wasUpdated: boolean = this.graphSettings.updateSetting("ignoreDuplicatedEdges", ignoreDuplicateEdges);
    if (reload && wasUpdated) this.reload();
  }

  /**
   * Update whether the graph should allow for self loops or not.
   *
   * @param {boolean} allow - Whether to allow self loops in the graph.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `false`
   */
  public updateAllowSelfLoops(allow: boolean, reload: boolean = false): void {
    const wasUpdated: boolean = this.graphSettings.updateSetting("allowSelfLoops", allow);
    if (reload && wasUpdated) this.reload();
  }

  public updateGraph(graph: Internal_Graph, reload: boolean = false) : void {
    this.updateNodes(graph.nodes, false);
    this.updateEdges(graph.edges, reload)
  }

  /**
   * Updates the internal graph node data with the provided new nodes and optionally reloads the graph.
   *
   * @param {Internal_GraphNode[]} newNodes - The new nodes to be added.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `false`
   */
  private updateNodes(newNodes: Internal_GraphNode[], reload: boolean = false): void {
    this.graphProcessor.setNodeData(newNodes);
    if (reload) this.reload();
  }

  /**
   * Updates the custom bounding box.
   *
   * @param {{x:[number,number], y:[number,number] } | null} customBBox - The custom bounding box.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `true`
   */
  public updateCustomBoundingBox(customBBox: {x:[number,number], y:[number,number] } | null, reload: boolean = true): void {
    this.customBoundingBox = customBBox
    this.renderer?.setCustomBBox(customBBox);
    if (reload) this.reload();
  }

  /**
   * Sets the container in which the graph should be positioned.
   *
   * @param {HTMLDivElement} container - Container element for the graph.
   */
  public setContainer(container: HTMLDivElement): void {
    this.container = container;
    this.containerReadySignal.set(this.hasContainerDimensions());
  }

  /**
   * Sets the state of whether the graph needs initialization.
   *
   * @param {boolean} needsInitialization - Whether the graph needs initialization or not
   */
  public needsInitialization(needsInitialization: boolean): void {
    this.needsInitializationSignal.set(needsInitialization);
  }

  /**
   * Gets the state of whether the graph needs initialization.
   *
   * @returns {boolean} - The state of whether the graph needs initialization.
   */
  public containerReadySignalTracked(): boolean {
    return this.containerReadySignal();
  }

  public ngOnDestroy(): void {
    // Remove all listeners
    this.renderer?.removeAllListeners();
  }

  /** Resizes the graph rendering area. */
  public resize(): void {
    // When the graph is resized, we want to re-compute the area dependent scaling for proper sizing of
    // nodes and edges depending on the container's size
    this.graphSettings.computeAreaDependentScaling(this.container!);
    this.graphSettings.computeStagePadding();
    this.sigmaGraph.resize();
  }

  /**
   * Updates the internal representation of the serialized graph.
   *
   * This function sets the `serializedGraph` property of the `graphData` object to the given serialized graph. The next
   * time the graph gets reloaded, the serialized graph will be used instead of the current graph data.
   *
   * @param {SerializedGraph<N, E, G> | undefined} graph - The serialized graph representation to set, or `undefined` to
   *   clear the current serialized graph.
   * @param {boolean} [reload] - Whether to reload the graph. Default is `false`
   */
  public updateSerializedGraph(graph: SerializedGraph<N, E, G> | null, reload: boolean = false): void {
    this.graphData.serializedGraph = graph;
    if (reload) this.reload();
  }

  /**
   * Updates the internal camera state of the graph. The next time the graph gets reloaded, the serialized graph will be
   * used instead of the current graph data.
   *
   * @param {CameraState | undefined} state - The new camera state to set, or `undefined` to reset to the default camera
   * @param {boolean} [reload] - Whether to reload the graph. Default is `false` state.
   */
  public updateCameraState(state: CameraState | null, reload: boolean = false): void {
    this.graphData.cameraState = state;
    if (reload) this.reload();
  }

  /**
   * Updates the graph settings.
   *
   * @param {Partial<GraphSettings>} settings - New settings to apply.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `false`
   */
  public updateSettings(settings: Partial<GraphSettings>, reload: boolean = false): void {
    if (this.graphSettings.updateSettings(settings) && reload) this.reload();
  }

  /**
   * Updates the internal graph node data with the provided new edges and optionally reloads the graph.
   *
   * @param {Internal_GraphEdge[]} newEdges - The new edges to be added.
   * @param {boolean} [reload] - Whether to reload the graph after updating. Default is `false`
   */
  private updateEdges(newEdges: Internal_GraphEdge[], reload: boolean = false): void {
    this.graphProcessor.setEdgeData(newEdges);
    if (reload) this.reload();
  }

  /** Forces a simple redraw of the graph without re-initializing. Useful for minor UI updates. */
  public redraw(): void {
    this.sigmaGraph.refresh();
  }

  /**
   * Sets the function to generate metrics for the graph used inside the {@link MetricInformationComponent}.
   *
   * @param {GenerateMetricsFunction} generateMetricsFunction - Function that generates the desired metrics.
   */
  public setGenerateMetrics(generateMetricsFunction: GenerateMetricsFunction): void {
    this.graphMetrics.setGenerateMetrics(generateMetricsFunction);
  }

  /** Completely rebuilds the graph, useful when significant changes are made to the data or layout. */
  public reload(): void {
    // If our signal is already true, then call the init function directly - otherwise, set the signal
    if (this.needsInitializationSignalUntracked) this.initializeChartIfNeeded();
    else this.needsInitialization(true);
  }

  /** Reset all selections */
  public resetSelection(): void {
    this.setSelectedNode(null);
    this.setSelectedEdge(null);
    this.setHoveredEdge(null);
    this.setHoveredNode(null);
  }

  /**
   * Applies the configured graph layout settings. If the graph instance does not exist, the method will return without
   * any effect.
   */
  public applyGraphLayout(): void {
    if (!this.graph) return;
    // The layouts move the nodes into a different extent, which a bounding box frozen for dragging would cut off
    this.releaseDraggingBBox();
    this.ngZone.runOutsideAngular(() => {
      // Override the layout algorithm based on the hierarchicalLayoutMode setting when the
      // configured mode is one of the two hierarchical variants.
      const configured = this.graphData.initialLayoutConfiguration;
      const hierarchicalModes = [InitialGraphLayouts.HIERARCHICAL, InitialGraphLayouts.CHANNEL_HIERARCHICAL] as string[];
      const modeOverridden: InitialGraphLayoutConfiguration | null =
        configured && hierarchicalModes.includes(configured.mode)
          ? { ...configured, mode: this.graphSettings.getSetting("hierarchicalLayoutMode") as InitialGraphLayouts }
          : configured;

      // Inject useDummyNodes into the layout-specific parameters so both layout algorithms receive it.
      const useDummyNodes = this.graphSettings.getSetting("useDummyNodes");
      const initialLayout: InitialGraphLayoutConfiguration | null = modeOverridden
        ? { ...modeOverridden, parameters: { ...(modeOverridden.parameters as object ?? {}), useDummyNodes } as any }
        : null;
      if (initialLayout && this.graphSettings.getSetting("reverseEdgeDirection")) {
        // Build a copy of the graph with edges swapped back to their original direction so the
        // layout algorithm sees the original topology. Positions are then copied to the real graph.
        const layoutCopy = this.graph!.copy();
        layoutCopy.edges().forEach(key => {
          const src = layoutCopy.source(key);
          const tgt = layoutCopy.target(key);
          const attrs = { ...layoutCopy.getEdgeAttributes(key) };
          const undirected = layoutCopy.isUndirected(key);
          layoutCopy.dropEdge(key);
          undirected
            ? layoutCopy.addUndirectedEdgeWithKey(key, tgt, src, attrs)
            : layoutCopy.addDirectedEdgeWithKey(key, tgt, src, attrs);
        });
        this.layoutService.applyInitialLayout(initialLayout, layoutCopy);
      } else {
        this.layoutService.applyInitialLayout(initialLayout);
      }
      if (this.graphSettings.getSetting("hierarchicalDirection") === "horizontal") {
        this.rotateLayout90();
      }
      this.layoutService.startLayouting(this.graphData.complexLayoutConfiguration);
    });
  }

  /**
   * Rotates all node positions and edge waypoints by 90° counter-clockwise (swaps x/y so a
   * top-to-bottom vertical layout becomes a left-to-right horizontal layout).
   */
  private rotateLayout90(): void {
    if (!this.graph) return;
    this.graph.forEachNode((key, attrs) => {
      const x: number = attrs["x"] ?? 0;
      const y: number = attrs["y"] ?? 0;
      this.graph!.setNodeAttribute(key, "x", -y);
      this.graph!.setNodeAttribute(key, "y", x);
    });
    this.graph.forEachEdge((key, attrs) => {
      const waypoints: { x: number; y: number }[] | undefined = attrs["waypoints"];
      if (waypoints) {
        this.graph!.setEdgeAttribute(
          key,
          "waypoints",
          waypoints.map(wp => ({ x: -wp.y, y: wp.x })) as E["waypoints"]
        );
      }
    });
  }

  /**
   * Drags a node based on the mouse coordinates. The method will only proceed if draggable nodes are enabled, dragging
   * is currently occurring, and all necessary instances are available.
   *
   * @param {MouseCoords} event - The mouse coordinates event data.
   */
  private handleNodeDragging(event: MouseCoords): void {
    if (
      !this.graphSettings.getSetting("draggableNodes") ||
      !this.graphData.isDragging ||
      !this.graphData.draggedNode ||
      !this.renderer ||
      !this.graph
    )
      return;

    // Get new position of node
    const pos: Coordinates = this.renderer.viewportToGraph(event);

    this.graph.setNodeAttribute(this.graphData.draggedNode, "x", pos.x);
    this.graph.setNodeAttribute(this.graphData.draggedNode, "y", pos.y);

    // Prevent sigma to move camera:
    event.preventSigmaDefault();
    event.original.preventDefault();
    event.original.stopPropagation();
  }

  /**
   * Handles node hover behavior by updating renderer settings based on whether pretty hover effects should be used.
   * This function dynamically updates the node and edge styles in the graph when hovering, based on the settings.
   *
   * @param {string | null} hoveredNode - The key of the node that is currently hovered.
   */
  private handleNodeHover(hoveredNode: string | null): void {
    if (!this.graphSettings.getSetting("enableNodeHoverEvents")) return;

    const shouldUsePrettyNodeHover: boolean = this.graphSettings.getSetting("highlightNeighboursOnNodeHover");
    const nodeHoverProperties: prettyNodeHoverConfiguration =
      this.graphSettings.getSetting("prettyNodeHoverConfiguration");

    // Update the nodeReducer setting to change node styles dynamically based on the hovered node.
    // This will make the hovered node and its adjacent nodes/edges appear in a different style.
    this.renderer?.setSetting(
      "nodeReducer",
      hoveredNode && shouldUsePrettyNodeHover
        ? (node: string, data: Attributes): Attributes =>
            node === hoveredNode || this.graph?.hasEdge(node, hoveredNode) || this.graph?.hasEdge(hoveredNode, node)
              ? { ...data, ...nodeHoverProperties.activeNodeStyle }
              : {
                  ...data,
                  ...nodeHoverProperties.inactiveNodeStyle,
                }
        : null
    );

    // Update the edgeReducer setting to change edge styles dynamically based on the hovered node.
    // This will make edges connected to the hovered node appear in a different style.
    this.renderer?.setSetting(
      "edgeReducer",
      hoveredNode && shouldUsePrettyNodeHover
        ? (edge: string, data: Attributes): Attributes =>
            this.graph?.hasExtremity(edge, hoveredNode)
              ? { ...data, ...nodeHoverProperties.activeEdgeStyle }
              : { ...data, ...nodeHoverProperties.inactiveEdgeStyle }
        : null
    );
  }

  /**
   * Conditionally initializes the chart based on signals, running outside Angular's zone for performance.
   *
   * @remarks
   *   To increase the performance while maintaining the reactivity of the service, we use Signals to automatically
   *   determine whether a new initialization is required and call the {@link initializeChart} accordingly.
   */
  private initializeChartIfNeeded(): void {
    if (!this.needsInitializationSignalTracked || !this.containerReadySignalTracked()) return;
    this.ngZone.runOutsideAngular(() => this.initializeChart());
  }

  /**
   * Subscribes to various graph interaction events for managing node and edge states. This utility function sets up all
   * the necessary event listeners related to the graph renderer.
   */
  private subscribeToGraphEvents(): void {
    const isNodeHoverEnabled: boolean = this.graphSettings.getSetting("enableNodeHoverEvents");
    const isNodeClickEnabled: boolean = this.graphSettings.getSetting("enableNodeClickEvents");
    const isEdgeEnabled: boolean = this.graphSettings.getSetting("enableEdgeEvents");
    // Triggered when the mouse enters a node. Used to set the hovered node.
    this.renderer?.on("enterNode", (event: SigmaNodeEventPayload) => {
      if (isNodeHoverEnabled) this.setHoveredNode(event.node);
    });

    // Triggered when the mouse leaves a node. Used to clear the hovered node.
    this.renderer?.on("leaveNode", () => {
      if (isNodeHoverEnabled) this.setHoveredNode(null);
    });

    // Triggered when the mouse enters an edge. Used to set the hovered edge.
    this.renderer?.on("enterEdge", (event: SigmaEdgeEventPayload): void => {
      if (isEdgeEnabled) this.setHoveredEdge(event.edge);
    });

    // Triggered when the mouse leaves an edge. Used to clear the hovered edge.
    this.renderer?.on("leaveEdge", (): void => {
      if (isEdgeEnabled) this.setHoveredEdge(null);
    });

    // Triggered when a node is clicked. Used to set the selected node.
    this.renderer?.on("clickNode", (event: SigmaNodeEventPayload): void => {
      if (isNodeClickEnabled) this.setSelectedNode(event.node);
    });

    // Triggered when an edge is clicked. Used to set the selected edge.
    this.renderer?.on("clickEdge", (event: SigmaEdgeEventPayload): void => {
      if (isEdgeEnabled) this.setSelectedEdge(event.edge);
    });

    // Triggered when clicking on an empty area in the graph. Used to reset selection.
    this.renderer?.on("clickStage", (): void => {
      this.resetSelection();
    });

    // Triggered when a node is pressed down. Used to initiate node dragging.
    this.renderer?.on("downNode", (event: SigmaNodeEventPayload): void => {
      if (isNodeClickEnabled) this.setDraggedNode(event.node);
    });

    // Triggered when the mouse is moved. Used for dragging the node.
    this.renderer?.getMouseCaptor().on("mousemovebody", (event: MouseCoords): void => {
      this.handleNodeDragging(event);
    });

    // Triggered when the mouse is released. Used to stop node dragging.
    this.renderer?.getMouseCaptor().on("mouseup", (): void => {
      this.setDraggedNode(null);
    });

    // Triggered when the mouse is pressed down. Used to disable autoscale. Not while a layout runs, as the layout moves
    // the nodes out of the frozen bounding box.
    this.renderer?.getMouseCaptor().on("mousedown", (): void => {
      if (this.graphSettings.getSetting("draggableNodes") && !this.renderer?.getCustomBBox()
        && !this.layoutService.isRunning)
        this.renderer?.setCustomBBox(this.renderer.getBBox());
    });
  }

  /** Releases the bounding box frozen for dragging nodes, the custom bounding box (if any) applies again. */
  private releaseDraggingBBox(): void {
    this.renderer?.setCustomBBox(this.customBoundingBox);
  }

  /**
   * Checks if container dimensions are set.
   *
   * @returns {boolean} True if dimensions are set, otherwise false.
   */
  private hasContainerDimensions(): boolean {
    return !!this.container && this.container.offsetWidth > 0 && this.container.offsetHeight > 0;
  }

  /**
   * Initializes or updates Sigma renderer.
   *
   * @param {any} graph - The Sigma graph object.
   */
  private initializeOrUpdateRenderer(graph: any): void {
    if (!this.hasContainerDimensions()) return;
    this.layoutService.reset();
    if (this.renderer) {
      this.sigmaGraph.setGraph(graph);
      if(this.customBoundingBox) {
        this.renderer.setCustomBBox(this.customBoundingBox)
      }
    } else {
      this.sigmaGraph.createRenderer(graph, this.container!, this.graphSettings.getSettings() as Settings);
      if (this.renderer) {
        this.sigmaInitialized.next(this.renderer);
      }
      if(this.sigmaGraph.renderer) {
        this.sigmaGraph.renderer.setCustomBBox(this.customBoundingBox)
      }
      this.subscribeToGraphEvents();
    }
  }

  /**
   * Initializes the chart with optimized steps. Checks preconditions, processes data, creates the graph, and applies
   * initial layout. Also configures rendering settings and starts layouting.
   */
  private initializeChart(): void {
    // Return if the container dimensions are not set.
    if (!this.hasContainerDimensions()) return;

    // Resets all selections
    this.resetSelection();

    // Processes the Graph Data
    this.graphProcessor.processGraphData();

    // If we have no nodes => we don't need to continue
    if (!this.graphData.hasNodes) {
      this.needsInitialization(false);
      return;
    }
    const shouldUseSerializedGraph: boolean = !!this.graphData.serializedGraph;

    // Compute stage padding and initialize or update renderer.
    this.graphSettings.computeAreaDependentScaling(this.container!);
    this.graphSettings.computeStagePadding();

    // Initialize (or update) the Sigma Renderer
    this.initializeOrUpdateRenderer(
      this.createGraph().import(
        shouldUseSerializedGraph && this.graphData.serializedGraph
          ? this.graphData.serializedGraph
          : this.graphData.getGraphData()
      )
    );

    // Update the camera state to use the stored one (if available)
    if (this.graphData.cameraState) this.sigmaGraph.updateCameraState(this.graphData.cameraState);

    // Apply graph layout if we haven't used a serialized graph
    if (!shouldUseSerializedGraph) this.applyGraphLayout();

    // Update the labels to properly force all labels to be visible / hidden depending on the setting
    this.graphData.updateForceLabels();

    // Reset "stored settings"
    this.graphData.cameraState = null;
    this.graphData.serializedGraph = null;

    // Initialization done
    this.needsInitialization(false);

    if (this.renderer) this.graphInitialized.next(this.renderer);
  }

  /**
   * Creates a new graph instance with the proper settings and returns it.
   *
   * @returns {Graph} - Creates a new Graph instance.F
   */
  private createGraph(): Graph<N, E, G> {
    return new Graph<N, E, G>({
      allowSelfLoops: this.graphSettings.getSetting("allowSelfLoops"),
      type: this.graphSettings.getSetting("graphType"),
      multi: this.graphData.isMultiGraph,
    });
  }
}
