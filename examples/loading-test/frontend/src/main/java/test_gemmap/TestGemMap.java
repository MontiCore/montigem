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
 * see loadingtests.e2e.TestGemMapE2E
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
          + ";grade=" + c.getEnrollmentByStudent(s).get().getGrade());
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
}
