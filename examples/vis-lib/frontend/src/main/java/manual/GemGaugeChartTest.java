/* (c) https://github.com/MontiCore/monticore */
package manual;

import domain.Person;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartDataBuilder;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartEntry;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartEntryBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class GemGaugeChartTest extends GemGaugeChartTestTOP {

  @Override
  public GemGaugeChartData getData() {
    List<GemGaugeChartEntry> entries = Arrays.asList(
        new GemGaugeChartEntryBuilder()
            .name("Average friends across all age groups")
            .value(0)
            .type("#friends")
            .build().get()
    );

    int totalFriends = 0;
    int totalPeople = 0;

    for (Person person : this.getPersons()) {
      totalFriends += person.getFriendsList().size();
      totalPeople++;
    }

    int avg = totalPeople == 0 ? 0 : (int) Math.round((double) totalFriends / totalPeople) * 10;
    entries.get(0).setValue(avg);

    GemGaugeChartData data = new GemGaugeChartDataBuilder().entries(entries).build().get();
    return data;
  }
}
