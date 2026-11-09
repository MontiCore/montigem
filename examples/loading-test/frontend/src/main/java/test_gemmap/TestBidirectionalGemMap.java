/* (c) https://github.com/MontiCore/monticore */
package test_gemmap;

import loadingtests.Club;
import loadingtests.Member;

import java.util.function.Consumer;

/**
 * Changes bidirectional qualified associations in the browser from the clubs (owners) and from the member (value
 * side); see loadingtests.e2e.TestBidirectGemMap
 */
public class TestBidirectionalGemMap extends TestBidirectionalGemMapTOP {

  public static final String ROLE = "chair";

  @Override
  public Consumer<Void> putIntoClub(Club c, Member m) {
    return $ -> c.putMemberByCode(m);
  }

  @Override
  public Consumer<Void> removeFromClub(Club c, Member m) {
    return $ -> c.removeMemberByCode(m.getCode());
  }

  @Override
  public Consumer<Void> setClub(Member m, Club c) {
    return $ -> m.setClub(c);
  }

  @Override
  public Consumer<Void> clearClub(Member m) {
    return $ -> m.setClubAbsent();
  }

  /**
   * both sides of [[code]]: whether each club holds the member under its code, and the club of the member
   */
  @Override
  public Consumer<Void> read(Club c1, Club c2, Member m) {
    return $ -> {
      long clubId = m.getClubGem().getGemId();
      String club = clubId == c1.getGemId() ? c1.getLabel() : clubId == c2.getGemId() ? c2.getLabel() : "none";
      setResult("inClub1=" + c1.containsKeyMemberByCode(m.getCode()) + ";inClub2="
          + c2.containsKeyMemberByCode(m.getCode()) + ";club=" + club);
    };
  }

  @Override
  public Consumer<Void> putRole(Club c, Member m) {
    return $ -> c.putMemberByRole(ROLE, m);
  }

  @Override
  public Consumer<Void> removeRole(Club c) {
    return $ -> c.removeMemberByRole(ROLE);
  }

  /**
   * both sides of [String]: whether each club holds the member under the role, and the clubs of the member
   */
  @Override
  public Consumer<Void> readRoles(Club c1, Club c2, Member m) {
    return $ -> setResult("inClub1=" + c1.containsKeyMemberByRole(ROLE) + ";inClub2="
        + c2.containsKeyMemberByRole(ROLE) + ";clubs=" + m.sizeClubss() + ";hasClub1=" + m.containsClubs(c1)
        + ";hasClub2=" + m.containsClubs(c2));
  }
}
