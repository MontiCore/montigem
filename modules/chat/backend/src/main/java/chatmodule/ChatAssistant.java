package chatmodule;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import jsweet.lang.Erased;

/** LangChain4j implements this interface and discovers the annotated tools. */
@Erased
public interface ChatAssistant {
  Result<String> chat(@UserMessage String message);
}
