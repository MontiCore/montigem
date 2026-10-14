/* (c) https://github.com/MontiCore/monticore */
package manual;

import domain.Person;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntry;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntryBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class GemBarChartTest extends GemBarChartTestTOP {
  @Override
  public GemBarChartData getBarchartData() {
    List<String> labels = Arrays.asList("People born before 1993", "People born between 1993 and 1998", "People born after 1998");

    List<GemBarChartEntry> dataEntries = new ArrayList<>();
    dataEntries.add(new GemBarChartEntryBuilder()
        .label(Optional.of("People with less than 2 friends"))
        .data(Arrays.asList(0, 0, 0))
        .build().get());
    dataEntries.add(new GemBarChartEntryBuilder()
        .label(Optional.of("People with more than 2 friends"))
        .data(Arrays.asList(0, 0, 0))
        .build().get());

    if (this.getPersons() == null || this.getPersons().isEmpty()) {
      dataEntries.get(0).getDataList().set(0, 10);
      dataEntries.get(0).getDataList().set(1, 15);
      dataEntries.get(0).getDataList().set(2, 20);

      dataEntries.get(1).getDataList().set(0, 5);
      dataEntries.get(1).getDataList().set(1, 8);
      dataEntries.get(1).getDataList().set(2, 12);
    } else {
      for (Person person : this.getPersons()) {
        int birthday = person.getBirthday().getYear();
        int numberFriends = person.getFriendsList().size();
        if (birthday < 1993) {
          if (numberFriends <= 2) {
            incrementFirstDataValue(dataEntries, 0, 0);
          } else {
            incrementFirstDataValue(dataEntries, 1, 0);
          }
        } else if (birthday >= 1993 && birthday <= 1998) {
          if (numberFriends <= 2) {
            incrementFirstDataValue(dataEntries, 0, 1);
          } else {
            incrementFirstDataValue(dataEntries, 1, 1);
          }
        } else {
          if (numberFriends <= 2) {
            incrementFirstDataValue(dataEntries, 0, 2);
          } else {
            incrementFirstDataValue(dataEntries, 1, 2);
          }
        }
      }
    }

    return new GemBarChartData(dataEntries, labels);
  }

  private static void incrementFirstDataValue(List<GemBarChartEntry> dataEntries, int index1, int index2) {

    dataEntries.get(index1).getDataList().set(index2, dataEntries.get(index1).getDataList().get(index2) + 1);
  }
}
