package chatmodule.commands;

import chatmodule.MessageSender;
import chatmodule.ChatModuleManager;
import umlp.backendrte.command.StatusResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class SendMessage extends SendMessageTOP {
  public SendMessage(long messageGemId, long chatGemId, long contextGemId) {
    super(messageGemId, chatGemId, contextGemId);
  }

  @Override
  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  public StatusResult doAction() {
    MessageSender.send(ChatModuleManager.getMessageSure(getMessageGemId()),
        ChatModuleManager.getMessageHistorySure(getChatGemId()), getContextGemId());
    return StatusResult.ok(getId());
  }
}
