/* (c) https://github.com/MontiCore/monticore */
package loadingtests.commands;

import loadingtests.MyEnum;
import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.Objects;
import java.util.Optional;

public class TestCommand_DomainEnumOpt extends TestCommand_DomainEnumOptTOP {

  public TestCommand_DomainEnumOpt(Optional<MyEnum> e) {
    super(e);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(e));
  }
}
