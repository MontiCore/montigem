package home;

import collabtextmodule.CollabTextModuleManager;
import umlp.backendrte.command.CommandManager;
import umlp.jsweet.extension.annotation.Component;

@Component
public class CollabDemo extends CollabDemoTOP {

  /** Texts belong to the module domain and are therefore loaded explicitly. */
  @Override
  protected void initTexts() {
    super.initTexts();
    if (!this._textsSetExternally) {
      this.setTextsInternal(CollabTextModuleManager.getCollabTextList());
      this.notifyTextsObservers();
    }
  }

  // The example has no login, hence the users are told apart by their connection
  @Override
  protected void initUserName() {
    if (!this._userNameSetExternally) {
      this.setUserName("User " + CommandManager.getParticipantId());
    }
    super.initUserName();
  }
}
