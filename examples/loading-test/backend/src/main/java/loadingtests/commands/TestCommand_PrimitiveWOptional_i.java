package loadingtests.commands;

import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.Objects;
import java.util.Optional;

public class TestCommand_PrimitiveWOptional_i extends TestCommand_PrimitiveWOptional_iTOP {

  public TestCommand_PrimitiveWOptional_i(Optional<Integer> i) {
    super(i);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(this.i));
  }
}
