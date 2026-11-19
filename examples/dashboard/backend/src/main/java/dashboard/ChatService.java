package dashboard;

import chatmodule.AssistantInstructions;
import chatmodule.MessageHistory;
import dashboardmodule.CellTools;
import dashboardmodule.DomainModelTools;
import dashboardmodule.LayoutTools;
import dashboardmodule.ViewConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jsweet.lang.Erased;

import java.time.Duration;

/** Application-owned model configuration and dashboard assistant composition. */
@Erased
public class ChatService {
  private final chatmodule.ChatService service;

  public ChatService() {
    this(OpenAiChatModel.builder()
        .baseUrl(
            ensureEndsWith(environmentOrThrow("CHATBOT_DEMO_BASEURL"), "/openai/v1")
        )
        .apiKey(environmentOrThrow("CHATBOT_DEMO_KEY"))
        .modelName(environmentOrThrow("CHATBOT_DEMO_MODEL"))
        .timeout(Duration.ofSeconds(120))
        .maxRetries(0)
        .reasoningEffort("none")
        .build());
  }

  private static String ensureEndsWith(String base, String postfix) {
    if(base.endsWith(postfix)){
      return base;
    }
    return base + postfix;
  }

  ChatService(ChatModel model) {
    service = new chatmodule.ChatService(model,
        AssistantInstructions.load(ChatService.class.getClassLoader(), "chat/instructions.list"));
  }

  private static String environmentOrThrow(String name) {
    String value = System.getenv(name);
    if(value == null || value.isBlank()){
      throw new RuntimeException("Mandatory environment variable " + name + " has is not set!");
    }
    return  value;
  }

  public void send(MessageHistory chat) {
    send(chat, null);
  }

  public void send(MessageHistory chat, ViewConfig view) {
    service.send(chat, new LayoutTools(view), new CellTools(view),
        new DomainModelTools(ChatService.class.getClassLoader(), "models/Dashboard.umlp"));
  }
}
