package chatcomponents;

import chatmodule.ChatModuleManager;
import chatmodule.Message;
import chatmodule.MessageHistory;
import chatmodule.commands.SendMessage;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.CommandResult;
import umlp.jsweet.extension.annotation.Component;
import java.util.function.Consumer;

@Component
public class Conversation extends ConversationTOP {
  @Override
  public Consumer<Void> sendMessage() {
    return $ -> send();
  }

  private void send() {
    if (isWaitingForResponse() || getMsgHistory() == null
        || !Message.hasLetterOrDigit(getContent())) {
      return;
    }
    setWaitingForResponse(true);
    try {
      Message message = ChatModuleManager.messageBuilder()
          .content(getContent()).timestamp(Message.getCurrentTime()).user(true).build().get();
      getMsgHistory().addMessages(message);
      setContent("");
      CommandResult<Void> result = CommandManager.executeCommand(
          new SendMessage(message.getGemId(), getMsgHistory().getGemId(), getContextGemId()));
      if (!result.isSuccessful()) {
        setContent(message.getContent());
      }
    } finally {
      setWaitingForResponse(false);
    }
  }

  @Override
  public void notifyAddMessages(MessageHistory history, long messageGemId, int indexInList) {
    super.notifyAddMessages(history, messageGemId, indexInList);
    Message.setArrivalTimeIfMissing(ChatModuleManager.getMessageSure(messageGemId));
  }
}
