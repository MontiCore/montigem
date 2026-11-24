/* (c) https://github.com/MontiCore/monticore */
package gui.test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** text inputs of the types date, time and datetime-local bound to java.time values, see TimeInputsE2ETest */
public class TestTimeInputs extends TestTimeInputsTOP {

  @Override
  public String describe(LocalDate date, LocalTime time, LocalDateTime dateTime) {
    return "date=" + date + " time=" + time + " dateTime=" + dateTime;
  }
}
