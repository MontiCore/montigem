/* (c) https://github.com/MontiCore/monticore */
package manual;

import com.google.common.collect.Lists;
import domain.Person;
import mc.fenix.charts.gemsunburstcharttypes.GemSunburstData;
import mc.fenix.charts.gemsunburstcharttypes.GemSunburstDataBuilder;
import mc.fenix.charts.gemsunburstcharttypes.GemSunburstEntry;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;

@Component
public class GemSunburstChartTest extends GemSunburstChartTestTOP {

  @Override
  public GemSunburstData getData() {
    GemSunburstData data = new GemSunburstDataBuilder()
        .entry(new GemSunburstEntry("People separated in age groups", this.getPersons() == null ? 0 : this.getPersons().size()))
        .subEntries(Lists.newArrayList(
            new GemSunburstDataBuilder().entry(new GemSunburstEntry("Number of people born before 1993", 0))
                .subEntries(Lists.newArrayList(
                    new GemSunburstDataBuilder().entry(new GemSunburstEntry("Number of people born before 1987", 0))
                        .subEntries(new ArrayList<>()).build().get(),
                    new GemSunburstDataBuilder()
                        .entry(new GemSunburstEntry("Number of people born between 1987 and 1991", 0))
                        .subEntries(new ArrayList<>()).build().get(),
                    new GemSunburstDataBuilder().entry(new GemSunburstEntry("Number of people after 1991", 0))
                        .subEntries(new ArrayList<>()).build().get()))
                .build().get(),
            new GemSunburstDataBuilder().entry(new GemSunburstEntry("People born between 1993 and 1998", 0))
                .subEntries(Lists.newArrayList(
                    new GemSunburstDataBuilder().entry(new GemSunburstEntry("Number of people born before 1995", 0))
                        .subEntries(new ArrayList<>()).build().get(),
                    new GemSunburstDataBuilder()
                        .entry(new GemSunburstEntry("Number of people born between 1995 and 1997", 0))
                        .subEntries(new ArrayList<>()).build().get(),
                    new GemSunburstDataBuilder().entry(new GemSunburstEntry("Number of people after 1997", 0))
                        .subEntries(new ArrayList<>()).build().get()))
                .build().get(),
            new GemSunburstDataBuilder().entry(new GemSunburstEntry("Number of people born after 1998", 0))
                .subEntries(Lists.newArrayList(
                    new GemSunburstDataBuilder().entry(new GemSunburstEntry("Number of people born before 2001", 0))
                        .subEntries(new ArrayList<>()).build().get(),
                    new GemSunburstDataBuilder()
                        .entry(new GemSunburstEntry("Number of people born between 2001 and 2002", 0))
                        .subEntries(new ArrayList<>()).build().get(),
                    new GemSunburstDataBuilder().entry(new GemSunburstEntry("Number of people after 2002\"", 0))
                        .subEntries(new ArrayList<>()).build().get()))
                .build().get()))
        .build().get();

    if (this.getPersons() == null || this.getPersons().isEmpty()) {
      data.getEntry().setValue(100);

      data.getSubEntries(0).getEntry().setValue(45);
      data.getSubEntries(0).getSubEntries(0).getEntry().setValue(15);
      data.getSubEntries(0).getSubEntries(1).getEntry().setValue(20);
      data.getSubEntries(0).getSubEntries(2).getEntry().setValue(10);

      data.getSubEntries(1).getEntry().setValue(30);
      data.getSubEntries(1).getSubEntries(0).getEntry().setValue(10);
      data.getSubEntries(1).getSubEntries(1).getEntry().setValue(10);
      data.getSubEntries(1).getSubEntries(2).getEntry().setValue(10);

      data.getSubEntries(2).getEntry().setValue(25);
      data.getSubEntries(2).getSubEntries(0).getEntry().setValue(5);
      data.getSubEntries(2).getSubEntries(1).getEntry().setValue(15);
      data.getSubEntries(2).getSubEntries(2).getEntry().setValue(5);
    } else {
      for (Person person : this.getPersons()) {
        int birthday = person.getBirthday().getYear();
        if (birthday < 1993) {
          data.getSubEntries(0).getEntry().setValue(data.getSubEntries(0).getEntry().getValue() + 1);
          if (birthday < 1987) {
            data.getSubEntries(0).getSubEntries(0).getEntry()
                .setValue(data.getSubEntries(0).getSubEntries(0).getEntry().getValue() + 1);
          } else if (birthday < 1992) {
            data.getSubEntries(0).getSubEntries(1).getEntry()
                .setValue(data.getSubEntries(0).getSubEntries(1).getEntry().getValue() + 1);
          } else {
            data.getSubEntries(0).getSubEntries(2).getEntry()
                .setValue(data.getSubEntries(0).getSubEntries(2).getEntry().getValue() + 1);
          }
        } else if (birthday <= 1998) {
          data.getSubEntries(1).getEntry().setValue(data.getSubEntries(1).getEntry().getValue() + 1);
          if (birthday < 1995) {
            data.getSubEntries(1).getSubEntries(0).getEntry()
                .setValue(data.getSubEntries(1).getSubEntries(0).getEntry().getValue() + 1);
          } else if (birthday < 1998) {
            data.getSubEntries(1).getSubEntries(1).getEntry()
                .setValue(data.getSubEntries(1).getSubEntries(1).getEntry().getValue() + 1);
          } else {
            data.getSubEntries(1).getSubEntries(2).getEntry()
                .setValue(data.getSubEntries(1).getSubEntries(2).getEntry().getValue() + 1);
          }
        } else {
          data.getSubEntries(2).getEntry().setValue(data.getSubEntries(2).getEntry().getValue() + 1);
          if (birthday < 2001) {
            data.getSubEntries(2).getSubEntries(0).getEntry()
                .setValue(data.getSubEntries(2).getSubEntries(0).getEntry().getValue() + 1);
          } else if (birthday < 2003) {
            data.getSubEntries(2).getSubEntries(1).getEntry()
                .setValue(data.getSubEntries(2).getSubEntries(1).getEntry().getValue() + 1);
          } else {
            data.getSubEntries(2).getSubEntries(2).getEntry()
                .setValue(data.getSubEntries(2).getSubEntries(2).getEntry().getValue() + 1);
          }
        }
      }
    }
    return data;
  }

}
