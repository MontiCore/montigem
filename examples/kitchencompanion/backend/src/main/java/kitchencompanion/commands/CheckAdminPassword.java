package kitchencompanion.commands;

import kitchencompanion.AdminPasswordService;
import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class CheckAdminPassword extends CheckAdminPasswordTOP {

  public CheckAdminPassword(String password) {
    super(password);
  }
  
  @Override
  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  public SimpleResult<Boolean> doAction() {
    return replySuccess(AdminPasswordService.matches(getPassword()));
  }
  
}
