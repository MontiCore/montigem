package kitchencompanion;

import kitchencompanion.commands.User_get_filtered;
import kitchencompanion.gemlists.UserGemList;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.ListResult;
import umlp.backendrte.common.GemList;
import umlp.backendrte.common.GemListBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KitchenCompanionManagerClientImpl extends KitchenCompanionManagerClientImplTOP {
  //private GemList<User> users = new UserGemList();
  @umlp.jsweet.extension.annotation.Synchronized
  @Override
  protected GemList<User> _doGetUserList(){
    if (this.loadedUserList.isEmpty()) {
      ListResult<Long, List<Long>> res = CommandManager.executeCommand(new User_get_filtered()).asListResult();
      GemListBuilder<User> list = new UserGemList(res.get());
      this.loadedUserList = Optional.of(sortUsersList(list.build().get()));
    }
    return this.loadedUserList.get();
  }

  private static GemList<User> sortUsersList(GemList<User> users) {
    User.sortForDisplay(users);
    return users;
  }

}
