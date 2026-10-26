package loadingtests.commands;

import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.List;
import java.util.Objects;

public class TestCommand_PrimitiveWList_iJU /*extends TestCommand_PrimitiveWList_iJUTOP*/ {

 /* public TestCommand_PrimitiveWList_iJU(List<Integer> i) {
    super(i);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(getI())); // Nobody knows why the List is not required for java.util List :)
  }*/
}
