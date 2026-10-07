/* (c) https://github.com/MontiCore/monticore */
package manual;

import domain.Person;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartDataBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntry;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class GemPieChartTest extends GemPieChartTestTOP {

  @Override
  public GemPieChartData getData() {
    List<GemPieChartEntry> dataEntries = new ArrayList<>();
    dataEntries.add(new GemPieChartEntry(0, "People born before 1993", Optional.empty()));
    dataEntries.add(new GemPieChartEntry(0, "People born in and after 1993", Optional.of("#000000")));

    int group1 = 0;
    int group2 = 0;

    if (this.getPersons() == null || this.getPersons().isEmpty()) {
      group1 = 45;
      group2 = 55;
    } else {
      for (Person person : this.getPersons()) {
        int personBirthday = person.getBirthday().getYear();
        if (personBirthday < 1993) {
          group1++;
        }
        else {
          group2++;
        }
      }
    }
    dataEntries.get(0).setValue(group1);
    dataEntries.get(1).setValue(group2);
    return new GemPieChartDataBuilder().entries(dataEntries).build().get();
  }
}
