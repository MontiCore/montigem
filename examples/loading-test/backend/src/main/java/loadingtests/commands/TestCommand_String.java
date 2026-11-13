/* (c) https://github.com/MontiCore/monticore */
package loadingtests.commands;

import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.command.StatusResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.Objects;

public class TestCommand_String extends TestCommand_StringTOP {

  public TestCommand_String(String s) {
    super(s);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(getS()));
  }
}
