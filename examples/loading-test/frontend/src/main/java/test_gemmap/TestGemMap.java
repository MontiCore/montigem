/* (c) https://github.com/MontiCore/monticore */
package test_gemmap;

import loadingtests.Course;
import loadingtests.Enrollment;
import loadingtests.LoadingTestsManager;
import loadingtests.Student;
import loadingtests.Tutor;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Runs GemMap scenarios in the browser, i.e. against the TypeScript RTE GemMaps;
 * see loadingtests.e2e.TestGemMap
 */
public class TestGemMap extends TestGemMapTOP {

  @Override
  public Consumer<Void> putByOwner(Course c, Student s) {
    return $ -> c.putParticipantByMatNr(s);
  }

  @Override
  public Consumer<Void> putByMap(Course c, Student s) {
    return $ -> c.getParticipantByMatNrMap().put(s.getMatNr(), s);
  }

  @Override
  public Consumer<Void> removeByOwner(Course c, Student s) {
    return $ -> c.removeParticipantByMatNr(s.getMatNr());
  }

  @Override
  public Consumer<Void> removeByMap(Course c, Student s) {
    return $ -> c.getParticipantByMatNrMap().remove(s.getMatNr());
  }

  @Override
  public Consumer<Void> removeByKeySet(Course c, Student s) {
    return $ -> c.getParticipantByMatNrMap().keySet().remove(s.getMatNr());
  }

  /**
   * reads the map loaded from the server: keys are derived on first access, the entries are iterated in a view
   */
  @Override
  public Consumer<Void> read(Course c) {
    return $ -> {
      Map<Integer, Student> participants = c.getParticipantByMatNrMap();
      String text = "size=" + participants.size();
      for (Map.Entry<Integer, Student> entry : participants.entrySet()) {
        text += ";" + entry.getKey() + "=" + entry.getValue().getName();
      }
      text += ";contains1001=" + participants.containsKey(1001) + ";contains9999=" + participants.containsKey(9999);
      setResult(text);
    };
  }

  /**
   * changes the key attribute of a value that is keyed in the browser: the key observer rekeys it
   */
  @Override
  public Consumer<Void> rekey(Course c, Student s) {
    return $ -> {
      int oldMatNr = s.getMatNr();
      int newMatNr = oldMatNr + 1000;
      boolean before = c.containsKeyParticipantByMatNr(oldMatNr);
      s.setMatNr(newMatNr);
      setResult("before=" + before + ";old=" + c.containsKeyParticipantByMatNr(oldMatNr)
          + ";new=" + c.containsKeyParticipantByMatNr(newMatNr));
    };
  }

  /**
   * the key attribute staffId is declared in Staff, the superclass of the value Tutor
   */
  @Override
  public Consumer<Void> putTutor(Course c, Tutor t) {
    return $ -> {
      c.putTutorByStaffId(t);
      setResult("tutor=" + c.containsKeyTutorByStaffId(t.getStaffId()));
    };
  }

  @Override
  public Consumer<Void> rekeyTutor(Course c, Tutor t) {
    return $ -> {
      String oldStaffId = t.getStaffId();
      boolean before = c.containsKeyTutorByStaffId(oldStaffId);
      t.setStaffId(oldStaffId + "-new");
      setResult("before=" + before + ";old=" + c.containsKeyTutorByStaffId(oldStaffId)
          + ";new=" + c.containsKeyTutorByStaffId(oldStaffId + "-new"));
    };
  }

  /**
   * GemEntryMap: the key is the Student the Enrollment references
   */
  @Override
  public Consumer<Void> putEnrollment(Course c, Enrollment e, Student s) {
    return $ -> {
      c.putEnrollmentByStudent(e);
      setResult("enrollment=" + c.containsKeyEnrollmentByStudent(s)
          + ";grade=" + c.getEnrollmentByStudent(s).getGrade());
    };
  }

  /**
   * a builder keeps the values until it is built
   */
  @Override
  public Consumer<Void> buildCourse(Student s) {
    return $ -> {
      Course course = LoadingTestsManager.courseBuilder().title("Built In Browser").ects(3)
          .participantByMatNrPut(s).build().get();
      setResult("built=" + course.containsKeyParticipantByMatNr(s.getMatNr()));
    };
  }

  @Override
  public Consumer<Void> clear(Course c) {
    return $ -> c.getParticipantByMatNrMap().clear();
  }

  /**
   * type qualified association: the key is chosen on put
   */
  @Override
  public Consumer<Void> putSeat(Course c, Student s) {
    return $ -> {
      c.putStudentBySeat("A1", s);
      setResult("seat=" + c.containsKeyStudentBySeat("A1"));
    };
  }

  /**
   * a value is stored under one key only: put under another key, its entry moves
   */
  @Override
  public Consumer<Void> moveSeat(Course c, Student s) {
    return $ -> {
      c.putStudentBySeat("B2", s);
      setResult("A1=" + c.containsKeyStudentBySeat("A1") + ";B2=" + c.containsKeyStudentBySeat("B2")
          + ";size=" + c.sizeStudentBySeats());
    };
  }

  /**
   * reads the map loaded from the server: the keys travel with the value ids; "-" for a seat without a student
   */
  @Override
  public Consumer<Void> readSeats(Course c) {
    return $ -> {
      Map<String, Student> seats = c.getStudentBySeatMap();
      setResult("size=" + seats.size() + ";A1=" + (seats.containsKey("A1") ? seats.get("A1").getName() : "-")
          + ";B2=" + (seats.containsKey("B2") ? seats.get("B2").getName() : "-"));
    };
  }

  @Override
  public Consumer<Void> removeSeat(Course c) {
    return $ -> c.removeStudentBySeat("A1");
  }

  @Override
  public Consumer<Void> removeSeatByMap(Course c) {
    return $ -> c.getStudentBySeatMap().remove("A1");
  }

  /**
   * GemEntryMap: the number of entries, e.g. after a key object was deleted in the backend
   */
  @Override
  public Consumer<Void> readMentees(Course c) {
    return $ -> setResult("mentees=" + c.sizeMenteeByTutors());
  }

  /**
   * the largest long key the browser represents exactly (2^53-1)
   */
  @Override
  public Consumer<Void> putMaxPhone(Course c, Student s) {
    return $ -> {
      c.putStudentByPhone(9007199254740991L, s);
      setResult("max=" + c.containsKeyStudentByPhone(9007199254740991L));
    };
  }

  /**
   * a long key beyond 2^53-1: the browser cannot represent it exactly (2^53+1 becomes 2^53), the put is refused
   */
  @Override
  public Consumer<Void> putUnsafePhone(Course c, Student s) {
    return $ -> {
      c.putStudentByPhone(9007199254740993L, s);
      setResult("unsafe=" + c.containsKeyStudentByPhone(9007199254740993L) + ";size=" + c.sizeStudentByPhones());
    };
  }

  /**
   * type qualified GemEntryMap: the key is the Tutor chosen on put
   */
  @Override
  public Consumer<Void> putMentee(Course c, Tutor t, Student s) {
    return $ -> {
      c.putMenteeByTutor(t, s);
      setResult("mentee=" + c.getMenteeByTutor(t).getName());
    };
  }
}
