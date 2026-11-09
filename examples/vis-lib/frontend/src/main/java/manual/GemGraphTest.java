/* (c) https://github.com/MontiCore/monticore */
package manual;

import domain.Person;
import mc.fenix.networkgraph.networkgraphtypes.Graph;
import mc.fenix.networkgraph.networkgraphtypes.GraphBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdge;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdgeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphNode;
import mc.fenix.networkgraph.networkgraphtypes.GraphNodeBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GemGraphTest extends GemGraphTestTOP {

  @Override
  public Graph getNetworkgraphGraph() {
    if (this.getPersons() == null) {
      return null;
    }
    List<GraphNode> nodes = new ArrayList<>();
    List<GraphEdge> edges = new ArrayList<>();

    Integer nodeId = 1;
    Map<Person, Integer> mapOfPersons = new HashMap<>();
    Map<Integer, List<Person>> mapOfriends = new HashMap<>();

    for (Person person : this.getPersons()) {
      Optional<GraphNode> node = new GraphNodeBuilder()
          .id(nodeId.toString())
          .value(Optional.of(nodeId))
          .label(person.getName())
          .color(Optional.of("#ff0000")).build();
      if (node.isPresent()) {
        mapOfriends.put(nodeId, person.getFriendsList());
        mapOfPersons.put(person, nodeId);
        nodeId++;
        nodes.add(node.get());
      }
    }

    Integer edgeId = 1;
    for (Map.Entry<Integer, List<Person>> e : mapOfriends.entrySet()) {
      for (Person friend : e.getValue()) {
        Optional<GraphEdge> edge = new GraphEdgeBuilder()
            .id(edgeId.toString())
            .source(e.getKey().toString())
            .target(mapOfPersons.get(friend).toString())
            .build();
        if (edge.isPresent()) {
          edgeId++;
          edges.add(edge.get());
        }
      }
    }

    return new GraphBuilder()
        .nodes(nodes)
        .edges(edges)
        .build().get();
  }

}
