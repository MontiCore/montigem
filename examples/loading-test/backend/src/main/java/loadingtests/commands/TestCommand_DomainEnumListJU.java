package loadingtests.commands;

import loadingtests.MyEnum;
import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.List;
import java.util.Objects;

public class TestCommand_DomainEnumListJU extends TestCommand_DomainEnumListJUTOP {
  public TestCommand_DomainEnumListJU(List<MyEnum> e) {
    super(e);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(getEList()));
  }
}
