/* (c) https://github.com/MontiCore/monticore */
package loadingtests.commands;

import loadingtests.MyEnum;
import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.List;
import java.util.Objects;

public class TestCommand_DomainEnumListCT extends TestCommand_DomainEnumListCTTOP {
  public TestCommand_DomainEnumListCT(List<MyEnum> e) {
    super(e);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(getEList()));
  }
}
