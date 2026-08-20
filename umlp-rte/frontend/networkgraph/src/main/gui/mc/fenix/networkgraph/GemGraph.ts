/* (c) https://github.com/MontiCore/monticore */

import { CommonModule } from "@angular/common";
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from "@angular/core";
import Graph from "graphology";
import { SerializedGraph } from "graphology-types";
import { Graph as GUIGraph } from "../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/Graph";
import { GraphNode } from "../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/GraphNode";
import { GraphNodeBuilder } from "../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/GraphNodeBuilder";
import { NodePos } from "../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/NodePos";
import { NodePosBuilder } from "../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/NodePosBuilder";
import { GraphTooltipOptions } from "./shared/types/tooltip.types";
import { Sigma } from "sigma";
import { CameraState } from "sigma/types";
import { ControlButtonsComponent } from "./control-buttons/control-buttons.component";
import { GraphDataService } from "./data/graph-data.service";
import { GraphManagerService } from "./data/graph-manager.service";
import { SigmaGraphService } from "./data/sigma-graph.service";
import { FilterGroupComponent } from "./filter-group/filter-group.component";
import { GemGraphBasis } from "./GemGraphBasis";
import { EdgeProcessorService } from "./graph-processing/edge-processor.service";
import { GraphProcessorService } from "./graph-processing/graph-processor.service";
import { GroupMappingService } from "./graph-processing/group-mapping.service";
import { NodeProcessorService } from "./graph-processing/node-processor.service";
import { MetricInformationComponent } from "./metric-information/metric-information.component";
import { SearchNodeComponent } from "./search-node/search-node.component";
import { GraphLayoutService } from "./services/graph-layout.service";
import { GraphMetricsService } from "./services/graph-metrics.service";
import { GraphSettingsService } from "./services/graph-settings.service";
import { AutoUnsubscribe } from "./shared/decorators/auto-unsubscribe.decorator";
import { ControlButton, ControlButtonsLayout, SpeedDialConfig } from "./types/control-buttons.types";
import { GraphEvent, MouseEvent, TouchEvent } from "./types/graph-events.types";
import {
  ComplexGraphLayoutConfiguration,
  ComplexGraphLayouts,
  InitialGraphLayoutConfiguration, InitialGraphLayouts
} from "./types/graph-layout.types";
import { GraphSettings } from "./types/graph-settings.types";
import {
  EdgeAttributes,
  getNodeProgramOfString,
  Internal_Graph,
  NodeAttributes
} from "./types/graph.types";

/**
 * \NetworkGraphComponent\ is the core component that serves as the container for the entire graph rendering, user
 * interaction, and controls for modifying the graph view. This component is responsible for initializing and managing
 * the graph data, handling user interactions, and delegating rendering tasks to lower-level components and services.
 *
 * This component extends the GraphBasisComponent which encapsulates common graph-related functionality.
 *
 * @remarks
 *   - The graph can be configured using Input properties, it emits events for user interactions, and the actual graph
 *       representation itself can also be heavily configured using the settings object (see {@link GraphSettings}).
 *   - Other than that, this component is responsible for initializing and managing the graph data, handling user
 *       interactions, and delegating rendering tasks to lower-level components and services.
 *   - The graph manager service is provided publicly and can also be used to modify / manipulate the graph, call actions,
 *       access the actual graph instance, etc.
 *
 * @example
 *   <network-graph
 *   [serializedGraph]="serializedGraph"
 *   [cameraState]="cameraState"
 *   (nodeSelectionChange)="handleNodeSelectionChange(\)"
 *   ></network-graph>
 *
 * @see {@link GraphBasisComponent} for common graph-related functionalities.
 */
@Component({
  standalone: true,
  selector: "gem-graph",
  imports: [
    CommonModule,
    SearchNodeComponent,
    FilterGroupComponent,
    ControlButtonsComponent,
    MetricInformationComponent,
  ],
  providers: [
    GraphDataService,
    GraphManagerService,
    GraphProcessorService,
    GroupMappingService,
    EdgeProcessorService,
    NodeProcessorService,
    SigmaGraphService,
    GraphLayoutService,
    GraphMetricsService,
    GraphSettingsService,
  ],
  templateUrl: "./GemGraph.html",
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
@AutoUnsubscribe()
export class GemGraph extends GemGraphBasis implements OnChanges {

  switchExtendedButton: boolean = true;

  exampleComplexLayout = {
    mode: ComplexGraphLayouts.FORCEATLAS2,
    maxIterations: 100,
    parameters: {
      settings: {
        gravity: 1,
        slowDown: 1.2
      }
    }
  };

  // ========================================================================================
  // Input Properties
  // ========================================================================================

  /** MinHeight of Component, so that it is always rendered. */
  @Input() minHeight: string = "50vh";

  /** Flag to toggle the visibility of the SearchNodeComponent. */
  @Input() showSearchNode: boolean = true;

  /** Flag to toggle the visibility of the FilterGroupComponent. */
  @Input() showFilterGroup: boolean = true;

  /** Flag to toggle the visibility of the ControlButtonsComponent. */
  @Input() showControlButtons: boolean = true;

  /** Flag to toggle the visibility of the MetricInformationComponent. */
  @Input() showInformation: boolean = true;

  /** Array of keys representing metrics to be displayed. */
  @Input() metricKeys: string[] = this.graphManager.graphMetrics.getAllInformationMetricKeys();

  /** Custom function for generating metrics, if provided. */
  /*@Input() customMetricsGenerator?: CustomMetricsGenerator;*/

  /** Serialized representation of the graph to be displayed. */
  @Input() serializedGraph?: SerializedGraph<NodeAttributes, EdgeAttributes>;

  /** The initial camera state for displaying the graph. */
  @Input() cameraState?: CameraState;

  /** Partial configuration settings for the graph. */
  @Input() settings?: Partial<GraphSettings>;


  @Input({
    transform: (graph: GUIGraph) => {
      if(!graph) {
        return {
          nodes:[],
          edges:[]
        }
      }
      return {
        nodes: graph._nodes.__internalArray().map(n => {
          return {
            id: n.getId(),
            value: n.getValue(),
            label: n.getLabel(),
            size: n.getSize(),
            color: n.getColor(),
            group: n.getGroup(),
            fixed: n.getFixed(),
            x: n.getX(),
            y: n.getY(),
            nodeProgram: getNodeProgramOfString(n.getNodeProgram()),
            image: n.getImageUrl(),
            hoverLabel: n.getHoverLabel(),
            hover: n.getHover(),
            tooltip: n.getTooltip()
          }
        }),

        edges: graph._edges.__internalArray().map(e => {
          return {
            id: e.getId(),
            source: e.getSource(),
            target: e.getTarget(),
            name: e.getName(),
            size: e.getSize(),
            directed: e.getDirected(),
            color: e.getColor()
          }
        })
      };
    }
  })
  graph?: Internal_Graph;

  // TODO make Layouts configurable in GUIDSL Model/ in GUI component itself
  /** Initial layout configuration for displaying the graph. */
  @Input() initialLayout?: InitialGraphLayoutConfiguration | null = { mode: InitialGraphLayouts.RANDOM };

  /** Complex layout configuration for displaying the graph. */
  @Input() complexLayout?: ComplexGraphLayoutConfiguration | null = this.exampleComplexLayout;

  /** Array of additional control buttons to be displayed. */
  @Input() cbExtendButtons?: ControlButton[] = [
    {
      key: "continueLayout",
      actionFn: (button: ControlButton, graphManager: GraphManagerService): void => {
        if (this.switchExtendedButton) {
          this.switchExtendedButton = !this.switchExtendedButton
          if (this.complexLayout) {
            graphManager.updateLayoutConfiguration(this.complexLayout, true)
          }
        } else {
          this.switchExtendedButton = !this.switchExtendedButton
          graphManager.updateLayoutConfiguration(null, true)
        }
      },
      tooltip: "Switch Layout",
      icon: "pi pi-arrow-right-arrow-left",
    }, {
      key: "downloadGraph",
      actionFn: (button: ControlButton, graphManager: GraphManagerService): void => {
        this.downloadAsPNG(new Date().toISOString() + "Graph.png")
      },
      tooltip: "Download Graph",
      icon: "pi pi-file-export",
    }
  ];

  /** Array of identifiers for control buttons to be removed. */
  @Input() cbRemoveButtons?: string[];

  /** Layout configuration for control buttons. */
  @Input() cbLayout?: ControlButtonsLayout;

  /** Configuration for speed dial functionality of control buttons. */
  @Input() cbSpeedDialConfig?: SpeedDialConfig;

  /** Configuration options for tooltips. */
  @Input() tooltipOptions?: GraphTooltipOptions = { showDelay: 100 };

  /** If this is not set, the bounding box will be calculated based on the maxima and minima for x and y coordinates
   *  in the graph. Use it to ensure that different graphs have the same zoom.
   *  The customBoundinBox should not be calculated based on the graph nodes position. This default behavior is handled
   *  by Sigma and will break layout algorithms if used in our graph component.
   * */
  @Input() customBoundingBox?: { x: [number, number], y: [number, number] };

  // ========================================================================================
  // Output Events - Graph Interaction Events
  // ========================================================================================
  /** Event emitted when the chart has been initialized. */
  @Output() chartInit: EventEmitter<Sigma> = new EventEmitter();

  /** Event emitted when a control button is clicked. */
  @Output() controlButtonClicked: EventEmitter<ControlButton> = new EventEmitter();

  /** Event emitted when the selection of groups changes. */
  @Output() groupSelectionChange: EventEmitter<Set<string>> = new EventEmitter();

  /** Event emitted during the iteration of graph layout. */
  @Output() layoutIteration: EventEmitter<number> = new EventEmitter();

  /** Event emitted when the stage area of the graph is clicked. */
  @Output() graphClickStage: EventEmitter<GraphEvent<"clickStage">> = this.createLazyGraphEvent("clickStage");

  /** Event emitted when the stage area of the graph is right-clicked. */
  @Output() graphRightClickStage: EventEmitter<GraphEvent<"rightClickStage">> =
    this.createLazyGraphEvent("rightClickStage");

  /** Event emitted when a double click occurs on the stage area of the graph. */
  @Output() graphDoubleClickStage: EventEmitter<GraphEvent<"doubleClickStage">> =
    this.createLazyGraphEvent("doubleClickStage");

  /** Event emitted when the mouse wheel is used on the stage area of the graph. */
  @Output() graphWheelStage: EventEmitter<GraphEvent<"wheelStage">> = this.createLazyGraphEvent("wheelStage");

  /** Event emitted when a mousedown event occurs on the stage area of the graph. */
  @Output() graphDownStage: EventEmitter<GraphEvent<"downStage">> = this.createLazyGraphEvent("downStage");

  // ========================================================================================
  // Output Events - Node and Edge Interaction Events
  // ========================================================================================
  /** Event emitted when a node on the graph is clicked. */
  @Output() graphClickNode: EventEmitter<GraphNode> = new EventEmitter<GraphNode>();

  /** Event emitted when a node on the graph is right-clicked. */
  @Output() graphRightClickNode: EventEmitter<GraphEvent<"rightClickNode">> =
    this.createLazyGraphEvent("rightClickNode");

  /** Event emitted when a double click occurs on a node on the graph. */
  @Output() graphDoubleClickNode: EventEmitter<GraphEvent<"doubleClickNode">> =
    this.createLazyGraphEvent("doubleClickNode");

  /** Event emitted when the mouse wheel is used on a node on the graph. */
  @Output() graphWheelNode: EventEmitter<GraphEvent<"wheelNode">> = this.createLazyGraphEvent("wheelNode");

  /** Event emitted when a mousedown event occurs on a node on the graph. */
  @Output() graphDownNode: EventEmitter<GraphEvent<"downNode">> = this.createLazyGraphEvent("downNode");

  /**
   * Event emitted when an edge on the graph is clicked.
   *
   * @remarks
   *   - The {@link Settings.enableEdgeClickEvents} needs to be activated in order to work.
   */
  @Output() graphClickEdge: EventEmitter<GraphEvent<"clickEdge">> = this.createLazyGraphEvent("clickEdge");

  /**
   * Event emitted when an edge on the graph is right-clicked.
   *
   * @remarks
   *   - The {@link Settings.enableEdgeClickEvents} needs to be activated in order to work.
   */
  @Output() graphRightClickEdge: EventEmitter<GraphEvent<"rightClickEdge">> =
    this.createLazyGraphEvent("rightClickEdge");

  /**
   * Event emitted when a double click occurs on an edge on the graph.
   *
   * @remarks
   *   - The {@link Settings.enableEdgeClickEvents} needs to be activated in order to work.
   */
  @Output() graphDoubleClickEdge: EventEmitter<GraphEvent<"doubleClickEdge">> =
    this.createLazyGraphEvent("doubleClickEdge");

  /**
   * Event emitted when the mouse wheel is used on an edge on the graph.
   *
   * @remarks
   *   - The {@link Settings.enableEdgeWheelEvents} needs to be activated in order to work.
   */
  @Output() graphWheelEdge: EventEmitter<GraphEvent<"wheelEdge">> = this.createLazyGraphEvent("wheelEdge");

  /**
   * Event emitted when a mousedown event occurs on an edge on the graph.
   *
   * @remarks
   *   - The {@link Settings.enableEdgeClickEvents} needs to be activated in order to work.
   */
  @Output() graphDownEdge: EventEmitter<GraphEvent<"downEdge">> = this.createLazyGraphEvent("downEdge");

  /** Event emitted before the rendering process of the graph. */
  @Output() graphBeforeRender: EventEmitter<GraphEvent<"beforeRender">> = this.createLazyGraphEvent("beforeRender");

  /** Event emitted after the rendering process of the graph. */
  @Output() graphAfterRender: EventEmitter<GraphEvent<"afterRender">> = this.createLazyGraphEvent("afterRender");

  /** Event emitted when the graph is resized. */
  @Output() graphResize: EventEmitter<GraphEvent<"resize">> = this.createLazyGraphEvent("resize");

  /** Event emitted when the graph is killed or destroyed. */
  @Output() graphKill: EventEmitter<GraphEvent<"kill">> = this.createLazyGraphEvent("kill");

  /** Event emitted when the mouse enters a node on the graph. */
  @Output() graphEnterNode: EventEmitter<GraphEvent<"enterNode">> = this.createLazyGraphEvent("enterNode");

  /** Event emitted when the mouse leaves a node on the graph. */
  @Output() graphLeaveNode: EventEmitter<GraphEvent<"leaveNode">> = this.createLazyGraphEvent("leaveNode");

  /** Event emitted when the mouse enters an edge on the graph. */
  @Output() graphEnterEdge: EventEmitter<GraphEvent<"enterEdge">> = this.createLazyGraphEvent("enterEdge");

  /** Event emitted when the mouse leaves an edge on the graph. */
  @Output() graphLeaveEdge: EventEmitter<GraphEvent<"leaveEdge">> = this.createLazyGraphEvent("leaveEdge");

  // ========================================================================================
  // Output Events - Mouse and Touch Interaction Events
  // ========================================================================================
  /** Event emitted when the mouse captor is clicked. */
  @Output() mouseCaptorClick: EventEmitter<MouseEvent<"click">> = this.createLazyMouseEvent("click");

  /** Event emitted when the mouse captor is right-clicked. */
  @Output() mouseCaptorRightClick: EventEmitter<MouseEvent<"rightClick">> = this.createLazyMouseEvent("rightClick");

  /** Event emitted when a double click occurs on the mouse captor. */
  @Output() mouseCaptorDoubleClick: EventEmitter<MouseEvent<"doubleClick">> = this.createLazyMouseEvent("doubleClick");

  /** Event emitted when a mouseup event occurs on the mouse captor. */
  @Output() mouseCaptorUp: EventEmitter<MouseEvent<"mouseup">> = this.createLazyMouseEvent("mouseup");

  /** Event emitted when a mousedown event occurs on the mouse captor. */
  @Output() mouseCaptorDown: EventEmitter<MouseEvent<"mousedown">> = this.createLazyMouseEvent("mousedown");

  /** Event emitted when the mouse captor is moved. */
  @Output() mouseCaptorMove: EventEmitter<MouseEvent<"mousemove">> = this.createLazyMouseEvent("mousemove");

  /** Event emitted when the mouse captor is moved while over the body of the graph. */
  @Output() mouseCaptorMoveBody: EventEmitter<MouseEvent<"mousemovebody">> = this.createLazyMouseEvent("mousemovebody");

  /** Event emitted when the mouse wheel is used on the mouse captor. */
  @Output() mouseCaptorWheel: EventEmitter<MouseEvent<"wheel">> = this.createLazyMouseEvent("wheel");

  /** Event emitted when a touchup event occurs. */
  @Output() touchUp: EventEmitter<TouchEvent<"touchup">> = this.createLazyTouchEvent("touchup");

  /** Event emitted when a touchmove event occurs. */
  @Output() touchMove: EventEmitter<TouchEvent<"touchmove">> = this.createLazyTouchEvent("touchmove");

  /** Event emitted when a touchdown event occurs. */
  @Output() touchDown: EventEmitter<TouchEvent<"touchdown">> = this.createLazyTouchEvent("touchdown");

  // ========================================================================================
  // Output Events - Node and Edge Highlight and Selection Events
  // ========================================================================================
  /** Event emitted when the highlighted node changes. */
  @Output() highlightedNodeChange: EventEmitter<string | null> = new EventEmitter<string | null>();

  /** Event emitted when the selected node changes. */
  @Output() selectedNodeChange: EventEmitter<string | null> = new EventEmitter<string | null>();

  /** Event emitted when the selected edge changes. */
  @Output() selectedEdgeChange: EventEmitter<string | null> = new EventEmitter<string | null>();

  /** Event emitted when the dragged node changes. */
  @Output() draggedNodeChange: EventEmitter<string | null> = new EventEmitter<string | null>();

  /** Event emitted when the dragging state changes. */
  @Output() isDraggingChange: EventEmitter<boolean> = new EventEmitter<boolean>();

  /**
   * Event emitted when the hovered node changes.
   *
   * @remarks
   *   - The {@link Settings.enableEdgeHoverEvents} needs to be activated in order to work.
   */
  @Output() hoveredNodeChange: EventEmitter<string | null> = new EventEmitter<string | null>();

  /**
   * Event emitted when the hovered edge changes.
   *
   * @remarks
   *   - The {@link Settings.enableEdgeHoverEvents} needs to be activated in order to work.
   */
  @Output() hoveredEdgeChange: EventEmitter<string | null> = new EventEmitter<string | null>();

  // ========================================================================================
  // Layout Event
  // ========================================================================================
  /** Event emitted when layouting has finished */
  @Output() nodePositionsAfterLayout: EventEmitter<NodePos[]> = new EventEmitter<NodePos[]>()

  // ========================================================================================
  // View Child References
  // ========================================================================================
  /** Reference to the HTML container element for the Sigma.js graph. */
  @ViewChild("sigmaContainer") protected container: ElementRef<HTMLDivElement> | undefined;

  constructor() {
    super();
    // Subscribe to graph data changes to properly emit events
    effect(() => this.highlightedNodeChange.emit(this.graphManager.graphData.highlightNode));
    effect(() => this.selectedNodeChange.emit(this.graphManager.graphData.selectedNode));
    effect(() => this.selectedEdgeChange.emit(this.graphManager.graphData.selectedEdge));
    effect(() => this.draggedNodeChange.emit(this.graphManager.graphData.draggedNode));
    effect(() => this.isDraggingChange.emit(this.graphManager.graphData.isDragging));
    effect(() => this.hoveredNodeChange.emit(this.graphManager.graphData.hoveredNode));
    effect(() => this.hoveredEdgeChange.emit(this.graphManager.graphData.hoveredEdge));
    this.graphManager.layoutService.finishedLayouting.subscribe(x => {
      this.nodePositionsAfterLayout.emit(this.graphManager.graphData.getGraphData().nodes?.map(n => {
        const nodePosBuilder = new NodePosBuilder();
        nodePosBuilder.realBuilder=nodePosBuilder;
        nodePosBuilder.id(n.key);
        if (n.attributes?.x) {
          nodePosBuilder.x(n.attributes?.x)
        }
        if (n.attributes?.y) {
          nodePosBuilder.y(n.attributes?.y)
        }
        return nodePosBuilder.build()
      }))
    })

    // Subscribe to chart init event
    this.graphManager.graphInitialized$.subscribe((graph: Sigma) => {
      this.chartInit.next(graph);
    });

    this.createLazyGraphEvent("clickNode").subscribe((event: any) => {
      const nodeId = event.node;
      const attributes = this.graphManager.graph?.getNodeAttributes(nodeId);
      if (attributes) {
        const builder = new GraphNodeBuilder();
        builder.realBuilder=builder;
        builder.id(nodeId);
        if (attributes.label) builder.label(attributes.label);
        if (attributes.size) builder.size(attributes.size);
        if (attributes.color) builder.color(attributes.color);
        if (attributes.group) builder.group(attributes.group);
        if (attributes["fixed"]) builder.fixed(attributes["fixed"]);
        if (attributes.x !== undefined) builder.x(attributes.x);
        if (attributes.y !== undefined) builder.y(attributes.y);
        if (attributes.type) builder.nodeProgram(attributes.type);
        if (attributes.image) builder.imageUrl(attributes.image);
        if (attributes.tooltip) builder.tooltip(attributes.tooltip);
        if (attributes["hover"]) builder.hover(attributes["hover"]);
        if (attributes["hoverLabel"]) builder.hoverLabel(attributes["hoverLabel"]);

        this.graphClickNode.emit(builder.build());
      }
    });

    // After the initial rendering has completed, do the subscriptions
    afterNextRender((): void => {
      // Subscribe to iteration changes and emit an event on each iteration
      this.graphManager.layoutService.iterationCount$.subscribe((iterationCount) => {
        this.layoutIteration.emit(iterationCount);
      });

      // Trigger change detection whenever something in the data changes
      this.graphManager.graphData.onDataChanged$.subscribe(() => this.cdReference.detectChanges());
    });
  }

  /**
   * Check for changes in inputs related to the graph data, layout, and rendering settings, and refresh the graph
   * rendering if necessary.
   *
   * @param {SimpleChanges} changes - Object of current and previous property values.
   */
  public ngOnChanges(changes: SimpleChanges): void {
    let shouldRefresh: boolean = false;

    if (this.hasChanged(changes, "serializedGraph", false)) {
      this.graphManager.updateSerializedGraph(this.serializedGraph ?? null);
      shouldRefresh = true;
    }
    if (this.hasChanged(changes, "cameraState", false)) {
      this.graphManager.updateCameraState(this.cameraState ?? null);
      shouldRefresh = true;
    }
    if (this.hasChanged(changes, "settings") && this.settings) {
      // TODO: Somehow check if only the settings have changed and if so, then try to redraw only...?
      this.graphManager.updateSettings(this.settings);
      shouldRefresh = true;
    }
    if (this.hasChanged(changes, "graph") && this.graph) {
      this.graphManager.updateGraph(this.graph);
      shouldRefresh = true;
    }
    if (this.hasChanged(changes, "initialLayout")) {
      this.graphManager.updateInitialLayout(this.initialLayout || null);
      shouldRefresh = true;
    }
    if (this.hasChanged(changes, "complexLayout")) {
      this.graphManager.updateLayoutConfiguration(this.complexLayout || null);
      shouldRefresh = true;
    }
    if (this.hasChanged(changes, "customBoundingBox")) {
      this.graphManager.updateCustomBoundingBox(this.customBoundingBox || null);
      shouldRefresh = true;
    }
    /*if (this.hasChanged(changes, "customMetricsGenerator") && this.customMetricsGenerator) {
        // No need to refresh here
        this.graphManager.setGenerateMetrics(() => this.customMetricsGenerator!(this));
    }*/

    if (shouldRefresh) this.graphManager.reload();
  }
}
