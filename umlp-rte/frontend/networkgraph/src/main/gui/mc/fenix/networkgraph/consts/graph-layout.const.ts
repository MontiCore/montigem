import { GraphLayoutOptions } from "../types/graph-layout.types";

/** Default options for the graph layout. */
export const DEFAULT_LAYOUT_OPTIONS: GraphLayoutOptions = {
  /** Flip visual arrow direction without changing graph data. */
  reverseEdgeDirection: true,

  /** Direction of the hierarchical layout: "vertical" = top-to-bottom, "horizontal" = left-to-right. */
  hierarchicalDirection: "vertical",

  /** Layout algorithm to use: "HIERARCHICAL" (Sugiyama) or "CHANNEL_HIERARCHICAL" (Ortali & Tollis). */
  hierarchicalLayoutMode: "HIERARCHICAL",

  /** If true, long-span edges bend through intermediate rank layers via dummy nodes/waypoints. */
  useDummyNodes: false,
};
