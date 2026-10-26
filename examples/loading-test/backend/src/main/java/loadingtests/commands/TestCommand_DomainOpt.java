package loadingtests.commands;

import loadingtests.MyEnum;
import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.Objects;
import java.util.Optional;

public class TestCommand_DomainOpt extends TestCommand_DomainOptTOP {

  public TestCommand_DomainOpt(Optional<Long> aGemId) {
    super(aGemId);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(aGemId));
  }
}
