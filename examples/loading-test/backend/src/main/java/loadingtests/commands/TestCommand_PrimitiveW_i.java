package loadingtests.commands;

import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.command.StatusResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.Objects;

public class TestCommand_PrimitiveW_i  extends TestCommand_PrimitiveW_iTOP {

  public TestCommand_PrimitiveW_i(Integer i) {
    super(i);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(getI()));
  }
}
