/* (c) https://github.com/MontiCore/monticore */
package loadingtests.commands;

import loadingtests.MyEnum;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.command.StatusResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.Objects;

public class TestCommand_DomainEnum extends TestCommand_DomainEnumTOP{

  public TestCommand_DomainEnum(MyEnum e) {
    super(e);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(getE()));
  }
}
