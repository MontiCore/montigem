/* (c) https://github.com/MontiCore/monticore */
package manual;

import com.google.common.collect.Lists;
import domain.Person;
import mc.fenix.charts.gemvenndiagramtypes.GemVennDiagramData;
import mc.fenix.charts.gemvenndiagramtypes.GemVennDiagramDataBuilder;
import mc.fenix.charts.gemvenndiagramtypes.GemVennDiagramEntry;
import mc.fenix.charts.gemvenndiagramtypes.GemVennDiagramEntryBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GemVennDiagramTest extends GemVennDiagramTestTOP {
  public GemVennDiagramData getVenndiagrammData() {
    if (this.getPersons() == null) {
      return null;
    }
    List<GemVennDiagramEntry> entries = new ArrayList<>();
    Map<String, List<Person>> mapOfriends = new HashMap<>();

    for (Person person : this.getPersons()) {
      GemVennDiagramEntry node = new GemVennDiagramEntryBuilder()
          .sets(Lists.newArrayList(person.getName()))
          .label(person.getName())
          .size(person.getFriendsList().size()).build().get();
      mapOfriends.put(person.getName(), person.getFriendsList());
      entries.add(node);
    }

    mapOfriends.entrySet().forEach(e -> {
      for (Person friend : e.getValue()) {
        if (mapOfriends.containsKey(friend.getName())) {
          GemVennDiagramEntry node = new GemVennDiagramEntryBuilder()
              .sets(Lists.newArrayList(e.getKey(), friend.getName()))
              .label("")
              .size(1).build().get();
          entries.add(node);
        }
      }
    });

    return new GemVennDiagramDataBuilder().entries(entries).build().get();
  }
}

