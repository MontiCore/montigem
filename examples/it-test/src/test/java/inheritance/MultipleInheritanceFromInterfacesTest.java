package inheritance;

import inheritance.test_infrastructure.InheritanceDBTestSetup;
import inheritance.test_infrastructure.InheritanceMockTestSetup;
import inheritance.test_infrastructure.InheritanceSQLDBTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.TestSetup;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface MultipleInheritanceFromInterfacesTest extends TestSetup<InheritanceTestContext> {

  class MultipleInheritanceFromInterfacesDBTest extends InheritanceSQLDBTestSetup implements MultipleInheritanceFromInterfacesTest {
    public MultipleInheritanceFromInterfacesDBTest() {
      super(0);
    }
  }

  class MultipleInheritanceFromInterfacesMockTest extends InheritanceMockTestSetup  implements MultipleInheritanceFromInterfacesTest {
    public MultipleInheritanceFromInterfacesMockTest() {
      super(0);
    }
  }

  @Test
  default void testFields(){
    AssocHelper ah1 = InheritanceManager.assocHelperBuilder().build().get();
    AssocHelper ah2 = InheritanceManager.assocHelperBuilder().build().get();
    AssocHelper ah3 = InheritanceManager.assocHelperBuilder().build().get();
    AssocHelper ah4 = InheritanceManager.assocHelperBuilder().build().get();
    AssocHelper ah5 = InheritanceManager.assocHelperBuilder().build().get();
    AssocHelper ah6 = InheritanceManager.assocHelperBuilder().build().get();

    MultipleInhClass inst = InheritanceManager.multipleInhClassBuilder()
        .simpleAttr1(1)
        .simpleAttr2(2)
        .simpleAttr3(3)

        .listAttr1(List.of(4))
        .listAttr2(List.of(5))
        .listAttr3(List.of(6))

        .gemAssoc1(ah1)
        .gemAssoc2(ah2)
        .gemAssoc3(ah3)

        .listAssoc1GemListIds(List.of(ah4.gemId))
        .listAssoc2GemListIds(List.of(ah5.gemId))
        .listAssoc3GemListIds(List.of(ah6.gemId))

        .build().get();

    assertNotNull(inst);

    if(hasDatabase()){
      MultipleInhClass dbInst = InheritanceDAOFactory.getMultipleInhClassDAO().selectMultipleInhClass(inst.gemId).get();
      assertNotSame(inst, dbInst);
      assertTrue(inst.TESTING_deepEquals(dbInst));
    }
  }

  /** a map attribute declared in an interface is stored in the table of the interface, like a list attribute */
  @Test
  default void testMapAttributeOfInterface() {
    MultipleInhClass inst = InheritanceManager.multipleInhClassBuilder()
        .simpleAttr1(1).simpleAttr2(2).simpleAttr3(3)
        .mapAttr1(Map.of("a", 4)).mapAttr3(Map.of("b", 5))
        .build().get();
    inst.putMapAttr1("c", 6);
    if (hasDatabase()) {
      MultipleInhClass dbInst = InheritanceDAOFactory.getMultipleInhClassDAO().selectMultipleInhClass(inst.gemId).get();
      assertEquals(Map.of("a", 4, "c", 6), dbInst.getMapAttr1Map());
      assertEquals(Map.of("b", 5), dbInst.getMapAttr3Map());
    }
    InheritanceManager.deleteMultipleInhClass(inst.gemId);
    if (hasDatabase()) {
      assertFalse(InheritanceDAOFactory.getMultipleInhClassDAO().selectMultipleInhClass(inst.gemId).isPresent());
    }
  }
}
