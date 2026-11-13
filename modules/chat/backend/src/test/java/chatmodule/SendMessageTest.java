/* (c) https://github.com/MontiCore/monticore */
package chatmodule;

import chatmodule.commands.SendMessage;
import org.junit.jupiter.api.Test;
import umlp.backendrte.command.Command;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.CommandObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class SendMessageTest {
  @Test
  void dispatchesSerializedCommandAndAppendsReplyWithoutDashboardDomain() {
    ChatModuleMock.init();
    MessageHistory history = ChatModuleManager.messageHistoryBuilder().name("Chat").build().get();
    Message message = ChatModuleManager.messageBuilder()
        .content("Hello").timestamp("12:00:00").user(true).build().get();
    history.addMessages(message);
    long contextGemId = 12345L;
    MessageSender.setHandler((sentMessage, conversation, context) -> {
      assertEquals(message.getGemId(), sentMessage.getGemId());
      assertEquals(history.getGemId(), conversation.getGemId());
      assertEquals(contextGemId, context);
      conversation.addMessages(ChatModuleManager.messageBuilder()
          .content("Reply to " + sentMessage.getContent()).timestamp("").user(false).build().get());
    });

    String json = CommandObjectMapper.serializeCommand(
        new SendMessage(message.getGemId(), history.getGemId(), contextGemId));
    Command<?> command = CommandObjectMapper.deserializeCommand(json).orElseThrow();
    assertInstanceOf(SendMessage.class, command);
    assertTrue(CommandManager.executeCommand(command).isSuccessful());

    assertEquals(2, history.getMessagesList().size());
    assertEquals("Reply to Hello", history.getMessages(1).getContent());
    assertFalse(history.getMessages(1).isUser());
  }
}
