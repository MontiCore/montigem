/* (c) https://github.com/MontiCore/monticore */
package test_gemmap;

import loadingtests.Member;

import java.util.function.Consumer;

/**
 * Reads the club side of a member whose clubs are not loaded (by gemId, which loads nothing); see
 * loadingtests.e2e.TestBidirectGemMap
 */
public class TestBidirectionalGemMapMember extends TestBidirectionalGemMapMemberTOP {

  @Override
  public Consumer<Void> readClub(Member m) {
    return $ -> setResult("club=" + m.getClubGem().getGemId() + ";clubs=" + m.sizeClubss());
  }
}
