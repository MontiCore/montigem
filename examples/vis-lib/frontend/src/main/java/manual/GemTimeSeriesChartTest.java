package manual;

import domain.Person;
import jsweet.lang.Replace;
import mc.fenix.charts.gemtimeseriescharttypes.TimeSeries;
import mc.fenix.charts.gemtimeseriescharttypes.TimeSeriesData;
import mc.fenix.charts.gemtimeseriescharttypes.TimeSeriesDataPoint;
import mc.fenix.charts.gemtimeseriescharttypes.TimeSeriesEvent;
import umlp.guiutil.GUIUtils;
import umlp.jsweet.extension.annotation.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class GemTimeSeriesChartTest extends GemTimeSeriesChartTestTOP {

  private List<TimeSeriesDataPoint> points = new ArrayList<>();
  private List<TimeSeriesEvent> events = new ArrayList<>();
  private int updateCounter = 0;
  private Object intervalId = null;

  @Override
  public void init() {
    super.init();

    // Initialisiere Daten exakt jetzt, um Lücken zum Live-Stream zu vermeiden
    points.clear();
    events.clear();
    LocalDateTime start = LocalDateTime.now().minusSeconds(100);
    for (int i = 0; i < 100; i++) {
      TimeSeriesDataPoint p = new TimeSeriesDataPoint();
      p.setTimestamp(start.plusSeconds(i));
      p.setValue(20.0 + Math.random() * 50.0);
      points.add(p);
    }

    TimeSeriesEvent e = new TimeSeriesEvent();
    e.setTimestamp(start.plusSeconds(50));
    e.setName("Initial Event");
    e.setShow(true);
    e.setBackgroundColor(Optional.of("#00E396"));
    e.setTextColor(Optional.of("#FFFFFF"));
    events.add(e);

    this.initUpdates();
  }

  @Replace("if(this.intervalId) clearInterval(this.intervalId); this.intervalId = setInterval(() => this.updateData2(), 1000)")
  private void initUpdates() {
  }

  public void updateData2() {
    updateCounter++;

    TimeSeriesDataPoint p = new TimeSeriesDataPoint();
    p.setTimestamp(LocalDateTime.now());
    p.setValue(20.0 + Math.random() * 50.0);
    points.add(p);

    // Manual removal removed here - the component will handle the view window

    if (updateCounter % 10 == 0) {
      TimeSeriesEvent e = new TimeSeriesEvent();
      e.setTimestamp(LocalDateTime.now());
      e.setName("Event " + updateCounter);
      e.setShow(true);
      e.setBackgroundColor(Optional.of(updateCounter % 20 == 0 ? "#FF4560" : "#00E396"));
      e.setTextColor(Optional.of("#FFFFFF"));
      events.add(e);

      if (events.size() > 5) {
        events.remove(0);
      }
    }

    this.triggerUpdates();
  }

  private void triggerUpdates() {
    GUIUtils.update(() -> this.getChart2Data());
  }

  @Override
  public TimeSeriesData computeData1(List<Person> val_$_$0) {
    TimeSeriesData data = new TimeSeriesData();

    TimeSeries s1 = new TimeSeries();
    s1.setName("Sine Wave");
    s1.setIdentifier(1L);
    s1.setColor(Optional.of("#008FFB"));

    TimeSeries s2 = new TimeSeries();
    s2.setName("Cosine Wave");
    s2.setIdentifier(2L);
    s2.setColor(Optional.of("#00E396"));

    LocalDateTime start = LocalDateTime.now().minusHours(1);
    for (int i = 0; i < 60; i++) {
      LocalDateTime t = start.plusMinutes(i);

      TimeSeriesDataPoint p1 = new TimeSeriesDataPoint();
      p1.setTimestamp(t);
      p1.setValue(50.0 + 30.0 * Math.sin(i * 0.2));
      s1.addDatapoints(p1);

      TimeSeriesDataPoint p2 = new TimeSeriesDataPoint();
      p2.setTimestamp(t);
      p2.setValue(50.0 + 30.0 * Math.cos(i * 0.2));
      s2.addDatapoints(p2);
    }

    data.addSeries(s1);
    data.addSeries(s2);

    TimeSeriesEvent e1 = new TimeSeriesEvent();
    e1.setTimestamp(start.plusMinutes(15));
    e1.setName("Peak Start");
    e1.setShow(true);
    e1.setBackgroundColor(Optional.of("#FEB019"));
    data.addEvents(e1);

    return data;
  }

  @Override
  public TimeSeriesData computeData2(List<Person> val_$_$0) {
    TimeSeriesData data = new TimeSeriesData();
    TimeSeries s = new TimeSeries();
    s.setName("Live Data");
    s.setIdentifier(3L);
    s.setColor(Optional.of("#FEB019"));

    for (int i = 0; i < points.size(); i++) {
      s.addDatapoints(points.get(i));
    }

    data.addSeries(s);

    for (int i = 0; i < events.size(); i++) {
      data.addEvents(events.get(i));
    }

    return data;
  }

  public Consumer<TimeSeriesEvent> onEventClick() {
    return event -> {
      setLastClickedEvent(event.getName() + " (at " + event.getTimestamp().toString() + ")");
    };
  }
}
