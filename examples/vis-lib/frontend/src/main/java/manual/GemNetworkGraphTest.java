/* (c) https://github.com/MontiCore/monticore */
package manual;

import domain.Person;
import mc.fenix.charts.gemnetworkgraphtypes.GemNetworkData;
import mc.fenix.charts.gemnetworkgraphtypes.GemNetworkEdge;
import mc.fenix.charts.gemnetworkgraphtypes.GemNetworkEdgeBuilder;
import mc.fenix.charts.gemnetworkgraphtypes.GemNetworkNode;
import mc.fenix.charts.gemnetworkgraphtypes.GemNetworkNodeBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GemNetworkGraphTest extends GemNetworkGraphTestTOP {

  @Override
  public GemNetworkData getNetworkgraphData() {
    if (this.getPersons() == null) {
      return null;
    }
    List<GemNetworkNode> nodes = new ArrayList<>();
    List<GemNetworkEdge> edges = new ArrayList<>();

    Integer nodeId = 1;
    Map<Person, Integer> mapOfPersons = new HashMap<>();
    Map<Integer, List<Person>> mapOfriends = new HashMap<>();

    for (Person person : this.getPersons()) {
      Optional<GemNetworkNode> node = new GemNetworkNodeBuilder()
          .id(nodeId.toString())
          .value(nodeId)
          .label(person.getName())
          .mass(person.getFriendsList().size())
          .color("Red").build();
      if (node.isPresent()) {
        mapOfriends.put(nodeId, person.getFriendsList());
        mapOfPersons.put(person, nodeId);
        nodeId++;
        nodes.add(node.get());
      }
    }

    final Integer nodeIdVlue = nodeId;
    mapOfriends.entrySet().forEach(e -> {
      for (Person friend : e.getValue()) {
        Optional<GemNetworkEdge> edge = new GemNetworkEdgeBuilder()
            .from(e.getKey().toString())
            .to(mapOfPersons.get(friend).toString())
            .length(200).build();
        edge.ifPresent(obj -> edges.add(obj));
      }
    });

    return new GemNetworkData(nodes, edges);
  }

}
