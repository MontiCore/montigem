package example_charts;

import domain.Person;
import mc.fenix.charts.gemtimelinecharttypes.Timeline;
import mc.fenix.charts.gemtimelinecharttypes.TimelineBlock;
import mc.fenix.charts.gemtimelinecharttypes.TimelineBlockBuilder;
import mc.fenix.charts.gemtimelinecharttypes.TimelineBuilder;
import mc.fenix.charts.gemtimelinecharttypes.TimelineData;
import mc.fenix.charts.gemtimelinecharttypes.TimelineDataBuilder;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class GemTimelineChartExample extends GemTimelineChartExampleTOP {

  private String[] colors = {"red", "green", "blue", "yellow", "orange", "purple", "cyan"};

  @Override
  public TimelineData computeDate(List<Person> persons) {
    List<Timeline> personList = getPersonlist(persons);
    TimelineDataBuilder data = new TimelineDataBuilder();
    data.timeLine(personList);
    return data.build().get();
  }

  private List<Timeline>  getPersonlist(List<Person> persons) {
    List<Timeline> personList = new ArrayList<>();
    if (persons == null) return personList;
    for (int i = 0 ; i< persons.size() && i < colors.length; i++) {
      TimelineBuilder timelineBuilder = new TimelineBuilder();
      TimelineBlockBuilder preBDay = new TimelineBlockBuilder()
          .start(LocalDateTime.of(2025, 0, 1, 0, 00, 00))
          .end(persons.get(i).getBirthday().withYear(2025).withDayOfMonth(1).atStartOfDay())
          .lane("Not Birthday Month");
      TimelineBlockBuilder BDay = new TimelineBlockBuilder()
          .start(persons.get(i).getBirthday().withYear(2025).withDayOfMonth(1).atStartOfDay())
          .end(persons.get(i).getBirthday().withYear(2025).plusMonths(1).withDayOfMonth(1).atStartOfDay())
          .lane("Birthday Month");
      TimelineBlockBuilder postBDay = new TimelineBlockBuilder()
          .start(persons.get(i).getBirthday().withYear(2025).plusMonths(1).withDayOfMonth(1).atStartOfDay())
          .end(LocalDateTime.of(2026, 0,1,0,0,0))
          .lane("Not Birthday Month");
      List<TimelineBlock> blockList = new ArrayList<>();
      blockList.add(preBDay);
      blockList.add(BDay);
      blockList.add(postBDay);
      timelineBuilder.color(Optional.of(colors[i%colors.length]))
          .name(persons.get(i).getName())
          .identifier((long) i)
          .deactivated(false)
          .hidden(false)
          .block(blockList);
      personList.add(timelineBuilder.build().get());
    }
    return personList;
  }

  @Override
  public Consumer<TimelineBlock> getBlockClick() {
    return x -> {
      System.out.println(x.getStart());
    };
  }
}
