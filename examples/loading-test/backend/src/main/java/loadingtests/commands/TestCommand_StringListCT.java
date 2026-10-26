/* (c) https://github.com/MontiCore/monticore */
package loadingtests.commands;

import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.List;
import java.util.Objects;

// TODO: // TODO: Collection Types broken, as they construct different methods as java.util types (https://git.rwth-aachen.de/monticore/monticore/-/work_items/5257)
// See LoadingTests.umlp!

public class TestCommand_StringListCT /*extends TestCommand_StringListCTTOP*/ {
 /* public TestCommand_StringListCT(List<String> s) {
    super(s);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<String> doAction() {
    return replySuccess(Objects.toString(getSList()));
  }*/
}
