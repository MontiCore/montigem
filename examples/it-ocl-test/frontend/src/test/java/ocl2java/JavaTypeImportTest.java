package ocl2java;

import mydomain.MydomainManager;
import mydomain.Person;
import mydomain.javaTypeImports.JavaTypeImport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import util.AbstractOCL2JavaGenTest;
import util.IOCL2XTest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public interface JavaTypeImportTest extends IOCL2XTest<Person> {
  @Tag("JavaGen")
  class JavaTypeImportJavaGenTest extends AbstractOCL2JavaGenTest<Person>
      implements JavaTypeImportTest {
    @Override
    protected List<Person> getContextObjects() {
      return MydomainManager.getPersonList();
    }
  }

  @Override
  default Map<String, Predicate<Person>> getConstraintToChecker() {
    return Map.of(
        "JavaTypeImport", JavaTypeImport::checkInv0
    );
  }

  @Override
  default String getResourceFolder() {
    return "javaTypeImports";
  }

  @Test
  default void testImportedJavaType() throws IOException, SQLException {
    MydomainManager.personBuilder().name("Alice").age(21).build().get();

    List<Long> violations = translateAndExecQueryFromPath("JavaTypeImport");

    assertTrue(violations.isEmpty());
  }
}
