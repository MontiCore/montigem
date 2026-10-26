package loadingtests.commands;

import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.command.StatusResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class TestCommand_Domain extends TestCommand_DomainTOP {
  public TestCommand_Domain(long aGemId) {
    super(aGemId);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Long.toString(getAGemId()));
  }
}
