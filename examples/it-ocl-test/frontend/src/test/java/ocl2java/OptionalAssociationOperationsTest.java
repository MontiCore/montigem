package ocl2java;

import com.microsoft.playwright.junit.UsePlaywright;
import mydomain.Classroom;
import mydomain.Course;
import mydomain.MydomainManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import util.AbstractOCL2CypherTest;
import util.AbstractOCL2JavaE2ETest;
import util.AbstractOCL2JavaGenTest;
import util.IOCL2XTest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UsePlaywright
public interface OptionalAssociationOperationsTest extends IOCL2XTest<Course> {
  @Disabled("Not yet implemented")
  @Tag("Cypher")
  class OptionalAssociationOperationsCypherTest extends AbstractOCL2CypherTest<Course> implements OptionalAssociationOperationsTest {
  }

  @Tag("JavaGen")
  class OptionalAssociationOperationsJavaGenTest extends AbstractOCL2JavaGenTest<Course> implements OptionalAssociationOperationsTest {
    @Override
    protected List<Course> getContextObjects() {
      return MydomainManager.getCourseList();
    }
  }

  @UsePlaywright
  @Tag("Playwright")
  class OptionalAssociationOperationsPlaywrightTest extends AbstractOCL2JavaE2ETest<Course> implements OptionalAssociationOperationsTest {
    @Override
    protected void _initConstraints() {
      registerGeneratedOclPages(getConstraintToChecker().keySet());
    }
  }

  @Override
  default Map<String, Predicate<Course>> getConstraintToChecker() {
    Map<String, Predicate<Course>> res = new HashMap<>();
    res.put("ElvisEquals1Association", mydomain.optionalOperations.ElvisEquals1Association::checkInv0);
    res.put("ElvisEquals2Association", mydomain.optionalOperations.ElvisEquals2Association::checkInv0);
    res.put("ElvisNotEquals1Association", mydomain.optionalOperations.ElvisNotEquals1Association::checkInv0);
    res.put("ElvisNotEquals2Association", mydomain.optionalOperations.ElvisNotEquals2Association::checkInv0);
    res.put("ElvisOptionalExpressionPrefixAssociation", mydomain.optionalOperations.ElvisOptionalExpressionPrefixAssociation::checkInv0);
    res.put("ElvisOptionalExpressionPrefixAssociation_Nested_NoBrackets", mydomain.optionalOperations.ElvisOptionalExpressionPrefixAssociation_Nested_NoBrackets::checkInv0);
    return res;
  }

  @Override
  default String getResourceFolder() {
    return "optionalOperations";
  }

  @Test
  default void testElvisEquals1Association() throws IOException, SQLException {
    String constraintName = "ElvisEquals1Association";

    // Setup data
    Classroom berlin = MydomainManager.classroomBuilder().name("Berlin").build().get();
    Classroom london = MydomainManager.classroomBuilder().name("London").build().get();
    Course sameRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(berlin).build().get();
    Course differentRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(london).build().get();
    Course noRoom = MydomainManager.courseBuilder().defaultRoom(berlin).build().get();

    // Exec query
    List<Long> violatingObjectGemIds = translateAndExecQueryFromPath(constraintName);

    // check results
    assertEquals(2, violatingObjectGemIds.size());
    assertFalse(violatingObjectGemIds.contains(sameRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(differentRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(noRoom.getGemId()));
  }

  @Test
  default void testElvisEquals2Association() throws IOException, SQLException {
    String constraintName = "ElvisEquals2Association";

    // Setup data
    Classroom berlin = MydomainManager.classroomBuilder().name("Berlin").build().get();
    Classroom london = MydomainManager.classroomBuilder().name("London").build().get();
    Course sameRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(berlin).build().get();
    Course differentRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(london).build().get();
    Course noRoom = MydomainManager.courseBuilder().defaultRoom(berlin).build().get();

    // Exec query
    List<Long> violatingObjectGemIds = translateAndExecQueryFromPath(constraintName);

    // check results
    assertEquals(2, violatingObjectGemIds.size());
    assertFalse(violatingObjectGemIds.contains(sameRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(differentRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(noRoom.getGemId()));
  }

  @Test
  default void testElvisNotEquals1Association() throws IOException, SQLException {
    String constraintName = "ElvisNotEquals1Association";

    // Setup data
    Classroom berlin = MydomainManager.classroomBuilder().name("Berlin").build().get();
    Classroom london = MydomainManager.classroomBuilder().name("London").build().get();
    Course sameRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(berlin).build().get();
    Course differentRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(london).build().get();
    Course noRoom = MydomainManager.courseBuilder().defaultRoom(berlin).build().get();

    // Exec query
    List<Long> violatingObjectGemIds = translateAndExecQueryFromPath(constraintName);

    // check results
    assertEquals(2, violatingObjectGemIds.size());
    assertTrue(violatingObjectGemIds.contains(sameRoom.getGemId()));
    assertFalse(violatingObjectGemIds.contains(differentRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(noRoom.getGemId()));
  }

  @Test
  default void testElvisNotEquals2Association() throws IOException, SQLException {
    String constraintName = "ElvisNotEquals2Association";

    // Setup data
    Classroom berlin = MydomainManager.classroomBuilder().name("Berlin").build().get();
    Classroom london = MydomainManager.classroomBuilder().name("London").build().get();
    Course sameRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(berlin).build().get();
    Course differentRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(london).build().get();
    Course noRoom = MydomainManager.courseBuilder().defaultRoom(berlin).build().get();

    // Exec query
    List<Long> violatingObjectGemIds = translateAndExecQueryFromPath(constraintName);

    // check results
    assertEquals(2, violatingObjectGemIds.size());
    assertTrue(violatingObjectGemIds.contains(sameRoom.getGemId()));
    assertFalse(violatingObjectGemIds.contains(differentRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(noRoom.getGemId()));
  }

  @Test
  default void testElvisOptionalExpressionPrefixAssociation() throws IOException, SQLException {
    String constraintName = "ElvisOptionalExpressionPrefixAssociation";

    // Setup data
    Classroom berlin = MydomainManager.classroomBuilder().name("Berlin").build().get();
    Classroom london = MydomainManager.classroomBuilder().name("London").build().get();
    Course assignedRoom = MydomainManager.courseBuilder().defaultRoom(london).room(berlin).build().get();
    Course wrongAssignedRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(london).build().get();
    Course fallbackRoom = MydomainManager.courseBuilder().defaultRoom(berlin).build().get();
    Course wrongFallbackRoom = MydomainManager.courseBuilder().defaultRoom(london).build().get();

    // Exec query
    List<Long> violatingObjectGemIds = translateAndExecQueryFromPath(constraintName);

    // check results
    assertEquals(2, violatingObjectGemIds.size());
    assertFalse(violatingObjectGemIds.contains(assignedRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(wrongAssignedRoom.getGemId()));
    assertFalse(violatingObjectGemIds.contains(fallbackRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(wrongFallbackRoom.getGemId()));
  }

  @Test
  default void testElvisOptionalExpressionPrefixAssociation_Nested_NoBrackets() throws IOException, SQLException {
    String constraintName = "ElvisOptionalExpressionPrefixAssociation_Nested_NoBrackets";

    // Setup data
    Classroom berlin = MydomainManager.classroomBuilder().name("Berlin").build().get();
    Classroom london = MydomainManager.classroomBuilder().name("London").build().get();
    Course assignedRoom = MydomainManager.courseBuilder().defaultRoom(london).room(berlin).build().get();
    Course wrongAssignedRoom = MydomainManager.courseBuilder().defaultRoom(berlin).room(london).build().get();
    Course fallbackRoom = MydomainManager.courseBuilder().defaultRoom(berlin).build().get();
    Course wrongFallbackRoom = MydomainManager.courseBuilder().defaultRoom(london).build().get();
    Course alternativeRoom = MydomainManager.courseBuilder().defaultRoom(london).alternativeRoom(berlin).build().get();
    Course wrongAlternativeRoom = MydomainManager.courseBuilder().defaultRoom(berlin).alternativeRoom(london).build().get();
    Course preferredRoom = MydomainManager.courseBuilder().defaultRoom(london).alternativeRoom(london).room(berlin).build().get();
    Course wrongPreferredRoom = MydomainManager.courseBuilder().defaultRoom(berlin).alternativeRoom(berlin).room(london).build().get();

    // Exec query
    List<Long> violatingObjectGemIds = translateAndExecQueryFromPath(constraintName);

    // check results
    assertEquals(4, violatingObjectGemIds.size());
    assertFalse(violatingObjectGemIds.contains(assignedRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(wrongAssignedRoom.getGemId()));
    assertFalse(violatingObjectGemIds.contains(fallbackRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(wrongFallbackRoom.getGemId()));
    assertFalse(violatingObjectGemIds.contains(alternativeRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(wrongAlternativeRoom.getGemId()));
    assertFalse(violatingObjectGemIds.contains(preferredRoom.getGemId()));
    assertTrue(violatingObjectGemIds.contains(wrongPreferredRoom.getGemId()));
  }
}
