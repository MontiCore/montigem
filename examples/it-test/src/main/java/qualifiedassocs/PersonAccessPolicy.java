/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import qualifiedassocs.commands.Club_putMemberByCode;
import qualifiedassocs.commands.Club_removeMemberByCode;
import umlp.backendrte.command.Command;

/**
 * The commands of a bidirectional qualified association also need the permission of the value type (see
 * OtherSideAccessPolicyDbTest): persons refuse to be put under the key "forbidden" and the removal of a person with
 * the login "locked".
 */
public class PersonAccessPolicy extends PersonAccessPolicyTOP {

  @Override
  public boolean checkCreatePermission(Command<?> cmd) {
    if (cmd instanceof Club_putMemberByCode && "forbidden".equals(((Club_putMemberByCode) cmd).getKey())) {
      return false;
    }
    return super.checkCreatePermission(cmd);
  }

  @Override
  public boolean checkDeletePermission(Command<?> cmd) {
    if (cmd instanceof Club_removeMemberByCode) {
      long personId = ((Club_removeMemberByCode) cmd).getMemberByCodeGemId();
      if (QualifiedAssocsManager.personOpt(personId).map(p -> "locked".equals(p.getLogin())).orElse(false)) {
        return false;
      }
    }
    return super.checkDeletePermission(cmd);
  }
}
