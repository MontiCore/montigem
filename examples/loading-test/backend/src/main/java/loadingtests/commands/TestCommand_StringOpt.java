/* (c) https://github.com/MontiCore/monticore */
package loadingtests.commands;

import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.Objects;
import java.util.Optional;

public class TestCommand_StringOpt extends TestCommand_StringOptTOP {

  public TestCommand_StringOpt(Optional<String> s) {
    super(s);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(s));
  }
}
