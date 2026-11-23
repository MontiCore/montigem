/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import org.junit.jupiter.api.Test;

public class UMLPMapAttributeNameClashTest extends AbstractCoCoTest<UMLPMapAttributeNameClash> {

  UMLPMapAttributeNameClashTest() {
    super(new UMLPMapAttributeNameClash(), UMLPCoCoChecker::addCoCo);
  }

  /** a Map attribute x next to an attribute xMap: both getters would be getXMap() */
  @Test
  public void testNameClash() {
    parseInvalid("UMLPMapAttributeNameClash");

    assertFindingAtLine(0, 5, "scores", "scoresMap", "getScoresMap()");
    assertFindingAtLine(1, 7, "byNick", "byNickMap", "getByNickMap()");
    assertFindingsCount(2);
  }
}
