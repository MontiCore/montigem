package kitchencompanion.commands;

import kitchencompanion.DeploymentService;
import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class IsDevEnvironment extends IsDevEnvironmentTOP {

  @Override
  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  public SimpleResult<Boolean> doAction() {
    return replySuccess(DeploymentService.isDevEnvironment());
  }

}