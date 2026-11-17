/* (c) https://github.com/MontiCore/monticore */
package test_mapattrs;

import loadingtests.Gradebook;
import loadingtests.MyEnum;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Runs map attribute scenarios in the browser; see loadingtests.e2e.TestMapAttributes
 */
public class TestMapAttributes extends TestMapAttributesTOP {

  @Override
  public Consumer<Void> putGrade(Gradebook g) {
    return $ -> g.putGrades("art", 2);
  }

  @Override
  public Consumer<Void> replaceGrade(Gradebook g) {
    return $ -> g.putGrades("math", 3);
  }

  @Override
  public Consumer<Void> removeGrade(Gradebook g) {
    return $ -> g.removeGrades("math");
  }

  /**
   * the entries in their order, e.g. "math=1;art=2"
   */
  @Override
  public Consumer<Void> readGrades(Gradebook g) {
    return $ -> {
      String result = "";
      for (Map.Entry<String, Integer> e : g.getGradesMap().entrySet()) {
        result += (result.isEmpty() ? "" : ";") + e.getKey() + "=" + e.getValue();
      }
      setResult(result);
    };
  }

  /**
   * the largest long key a browser represents exactly
   */
  @Override
  public Consumer<Void> putState(Gradebook g) {
    return $ -> g.putStates(9007199254740991L, MyEnum.B);
  }

  @Override
  public Consumer<Void> readStates(Gradebook g) {
    return $ -> setResult("size=" + g.sizeStatess() + ";max=" + g.getStates(9007199254740991L).name());
  }
}
