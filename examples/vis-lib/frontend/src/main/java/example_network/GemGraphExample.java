/* (c) https://github.com/MontiCore/monticore */
package example_network;

import mc.fenix.networkgraph.networkgraphtypes.Graph;
import mc.fenix.networkgraph.networkgraphtypes.GraphBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdge;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdgeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphNode;
import mc.fenix.networkgraph.networkgraphtypes.GraphNodeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.InitialGraphLayout;
import mc.fenix.networkgraph.networkgraphtypes.InitialGraphLayoutBuilder;
import mc.fenix.networkgraph.networkgraphtypes.InitialGraphLayoutMode;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import de.se_rwth.commons.logging.Log;

@Component(
    imports = {"...config.imports", "GemGraph"},
    tsImports = {"GemGraph, @umlp/networkgraph"}
)
public class GemGraphExample extends GemGraphExampleTOP {

  protected List<GraphNode> nodes = new ArrayList<>();
  protected List<GraphEdge> edges = new ArrayList<>();

  @Override
  public void init() {
    Optional<GraphNode> node = createGraphNode("1", "Node 1", "Cluster A", Optional.of(100), Optional.of("triangle"));
    node.ifPresent(n -> nodes.add(n));
    //node = createGraphNode("12", "Node 1", "Cluster A", Optional.of(100), Optional.of("square"));
    //node.ifPresent(n -> nodes.add(n));
    //node = createGraphNode("13", "Node 1", "Cluster A", Optional.of(100), Optional.of("circle"));
    //node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("2", "Node 2", "Cluster A", Optional.of(35), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("3", "Node 3", "Cluster A", Optional.of(44), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("4", "Node 4", "Cluster A", Optional.of(55), Optional.empty());
    if (node.isPresent()) {
      node.get().setHoverLabel(Optional.of("another hover label!"));
      node.get().setColor(Optional.of("#ffc0cb"));
      nodes.add(node.get());
    }
    node = createGraphNode("5", "Node 5", "Cluster A", Optional.of(54), Optional.empty()); //fixed:true, x:0, y:0, size:54, tooltip: "custom Tooltip!"
    if (node.isPresent()) {
      node.get().setTooltip(Optional.of("custom Tooltip!"));
      //node.get().setFixed(Optional.of(true));
      //node.get().setX(Optional.of(0.0));
      //node.get().setY(Optional.of(0.0));
      nodes.add(node.get());
    }
    node = createGraphNode("6", "Node 6", "Cluster B", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("7", "Node 7", "Cluster B", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("8", "Node 8", "Cluster B", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("9", "Node 9", "Cluster B", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("10", "Node 10", "Cluster B", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
   // node = createGraphNode("11", "Node 11", "Cluster B", Optional.empty(), Optional.empty()); // hover:"No Group Demo!"
    if (node.isPresent()) {
      node.get().setHoverLabel(Optional.of("No Group Demo!"));
      nodes.add(node.get());
    }


    Optional<GraphEdge> edge = createGraphEdge("1", "1", "2", "Edge 1-2", Optional.of("#fc4c4e"));
    if (edge.isPresent()) {
      edge.get().setSize(Optional.of(25));
      edge.get().setDirected(Optional.of(true));
      edges.add(edge.get());
    }
    edge = createGraphEdge("2", "2", "3", "Edge 2-3", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("3", "3", "3", "Edge 3-4", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("4", "4", "5", "Edge 4-5", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("5", "1", "5", "Edge 1-5", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("6", "2", "4", "Edge 2-4", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("7", "6", "7", "Edge 6-7", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("8", "7", "8", "Edge 7-8", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("9", "8", "9", "Edge 8-9", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("10", "9", "10", "Edge 9-10", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("11", "6", "10", "Edge 6-10", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("12", "7", "9", "Edge 7-9", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("13", "1", "6", "Edge 1-6", Optional.of("#fc4c4e"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("14", "5", "10", "Edge 5-10", Optional.empty());
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("15", "3", "8", "Edge 3-8", Optional.empty());
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("16", "1", "3", "Edge 1-3", Optional.empty());
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("17", "2", "5", "Edge 2-5", Optional.empty());
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("18", "4", "1", "Edge Edge 4-1", Optional.empty());
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("19", "6", "8", "Edge 6-8", Optional.empty());
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("20", "7", "10", "Edge 7-10", Optional.empty());
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("21", "9", "6", "Edge 9-6", Optional.empty());
    edge.ifPresent(e -> edges.add(e));

    // Second disconnected component
    node = createGraphNode("A", "Node A", "Cluster C", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("B", "Node B", "Cluster C", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("C", "Node C", "Cluster C", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));
    node = createGraphNode("D", "Node D", "Cluster C", Optional.empty(), Optional.empty());
    node.ifPresent(n -> nodes.add(n));

    edge = createGraphEdge("22", "A", "B", "Edge A-B", Optional.of("#4c9ffc"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("23", "A", "C", "Edge A-C", Optional.of("#4c9ffc"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("24", "B", "D", "Edge B-D", Optional.of("#4c9ffc"));
    edge.ifPresent(e -> edges.add(e));
    edge = createGraphEdge("25", "A", "D", "Edge A-D", Optional.of("#4c9ffc"));
    edge.ifPresent(e -> edges.add(e));

    super.init();
  }

  public List<GraphEdge> getPersonGraphEdges() {
    return this.edges;
  }

  public List<GraphNode> getPersonGraphNodes() {
    return this.nodes;
  }

  private static Optional<GraphNode> createGraphNode(String id, String label, String group, Optional<Integer> size,
      Optional<String> nodeProgram) {
    return new GraphNodeBuilder()
        .id(id)
        .label(label)
        .group(Optional.of(group))
        .size(size)
        .nodeProgram(nodeProgram)
        .build();
  }

  private static Optional<GraphEdge> createGraphEdge(String id, String from, String to, String name, Optional<String> color) {
    return new GraphEdgeBuilder()
        .id(id)
        .source(from)
        .target(to)
        .color(color)
        .name(Optional.of(name))
        .build();
  }

  public Consumer<GraphNode> updateClickedNode() {
    return (node) -> {
      this.setLastClickedNodeName(node.getLabel());
      Log.info("GemGraphExample", "Clicked on node: " + node.getId() + " (" + node.getLabel() + ")");
    };
  }

  @Override
  public Graph getPersonGraphGraph() {
    return new GraphBuilder()
        .edges(this.getPersonGraphEdges())
        .nodes(this.getPersonGraphNodes())
        .build().get();
  }

  @Override
  public InitialGraphLayout getPersonGraphInitialLayout() {
    return new InitialGraphLayoutBuilder()
        .mode(InitialGraphLayoutMode.HIERARCHICAL)
        .build().get();
  }

}
