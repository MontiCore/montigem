/* (c) https://github.com/MontiCore/monticore */
package loadingtests.commands;

import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.util.List;
import java.util.Objects;

public class TestCommand_PrimitiveWList_iCT /*extends TestCommand_PrimitiveWList_iCTTOP*/ {

  // TODO: Only String is supported as a primitive type! (https://git.rwth-aachen.de/monticore/monticore/-/work_items/5256)
  // See LoadingTests.umlp!

  //  public TestCommand_PrimitiveWList_iCT(List<Integer> i) {
//    super(i);
//  }
//
//  @BackendOnly(BackendOnlyModes.THROW_ERROR)
//  @Override
//  public SimpleResult<String> doAction() {
//    return replySuccess(Objects.toString(getIList()));
//  }
}
