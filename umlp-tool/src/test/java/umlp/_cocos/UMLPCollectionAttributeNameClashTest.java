/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import org.junit.jupiter.api.Test;

public class UMLPCollectionAttributeNameClashTest extends AbstractCoCoTest<UMLPCollectionAttributeNameClash> {

  UMLPCollectionAttributeNameClashTest() {
    super(new UMLPCollectionAttributeNameClash(), UMLPCoCoChecker::addCoCo);
  }

  /** a List attribute x next to an attribute xList, a Map attribute x next to xMap: the getters would clash */
  @Test
  public void testNameClash() {
    parseInvalid("UMLPCollectionAttributeNameClash");

    assertFindingAtLine(0, 5, "scores", "scoresMap", "getScoresMap()");
    assertFindingAtLine(1, 7, "byNick", "byNickMap", "getByNickMap()");
    assertFindingAtLine(2, 9, "tags", "tagsList", "getTagsList()");
    assertFindingAtLine(3, 11, "friends", "friendsList", "getFriendsList()");
    assertFindingsCount(4);
  }
}
