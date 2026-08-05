package resources.exectests;

import util.JSweetTest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NestedFlatMapRuntimeCheck {

  @JSweetTest
  public static boolean checkNestedFlatMapThenMap() {
    TimeEntry entry = new TimeEntry(LocalDateTime.of(2024, 2, 29, 12, 34, 56));
    Day day = new Day(List.of(entry));
    User user = new User(List.of(new TimeSheet(List.of(day))));

    for (Day currentDay : user.getTimeSheets().stream()
        .flatMap(timeSheet -> timeSheet.getDays().stream()).toList()) {
      LocalDateTime end = currentDay.getEntries().stream()
          .map(timeEntry -> timeEntry.getEnd()).toList().get(0);
      return end.equals(entry.getEnd());
    }
    return false;
  }

  @JSweetTest
  public static boolean checkInferredLoopVariable() {
    TimeEntry entry = new TimeEntry(LocalDateTime.of(2024, 2, 29, 12, 34, 56));
    User user = new User(List.of(new TimeSheet(List.of(new Day(List.of(entry))))));

    for (var currentDay : user.getTimeSheets().stream()
        .flatMap(timeSheet -> timeSheet.getDays().stream()).toList()) {
      return currentDay.getEntries().get(0).getEnd().equals(entry.getEnd());
    }
    return false;
  }

  @JSweetTest
  public static boolean checkInferredExternalLoopElement() {
    for (var element : NestedFlatMapElementSource.elements()) {
      return element.getValue() == 42;
    }
    return false;
  }

  @JSweetTest
  public static boolean checkMapEntryLoop() {
    Map<Integer, Long> indexToId = new HashMap<>();
    indexToId.put(7, 42L);

    for (Map.Entry<Integer, Long> entry : indexToId.entrySet()) {
      return entry.getKey() == 7 && entry.getValue() == 42L;
    }
    return false;
  }

  @JSweetTest
  public static boolean checkMapEntryPrimitiveTypes() {
    Map<Integer, Long> indexToId = new HashMap<>();
    indexToId.put(7, 42L);

    for (Map.Entry<Integer, Long> entry : indexToId.entrySet()) {
      return hasExpectedPrimitiveValues(entry.getKey(), entry.getValue());
    }
    return false;
  }

  private static boolean hasExpectedPrimitiveValues(int key, long value) {
    return key == 7 && value == 42L;
  }

  private static class User {
    private final List<TimeSheet> timeSheets;

    private User(List<TimeSheet> timeSheets) {
      this.timeSheets = timeSheets;
    }

    private List<TimeSheet> getTimeSheets() {
      return timeSheets;
    }
  }

  private static class TimeSheet {
    private final List<Day> days;

    private TimeSheet(List<Day> days) {
      this.days = days;
    }

    private List<Day> getDays() {
      return days;
    }
  }

  private static class Day {
    private final List<TimeEntry> entries;

    private Day(List<TimeEntry> entries) {
      this.entries = entries;
    }

    private List<TimeEntry> getEntries() {
      return entries;
    }
  }

  private static class TimeEntry {
    private final LocalDateTime end;

    private TimeEntry(LocalDateTime end) {
      this.end = end;
    }

    private LocalDateTime getEnd() {
      return end;
    }
  }
}
