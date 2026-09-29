package umlpwithgui._lsp.plugin;

import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;

public class UMLPWithGUILanguageServerFactory extends UMLPWithGUILanguageServerFactoryTOP {
  public static final String LANGUAGE_SERVER_INIT_WORK_DONE_TOKEN = "language-server-init-work-done-token";

  @Override
  public @NotNull LSPClientFeatures createClientFeatures() {
    return new LSPClientFeatures(){
      @Override
      public void initializeParams(@NotNull InitializeParams initializeParams) {
        initializeParams.setWorkDoneToken(LANGUAGE_SERVER_INIT_WORK_DONE_TOKEN);
      }
    };
  }

  @Override
  public @NotNull LanguageClientImpl createLanguageClient(@NotNull Project project) {
    LanguageClientImpl res = new LanguageClientImpl(project);
    res.createProgress(new WorkDoneProgressCreateParams(Either.forLeft(LANGUAGE_SERVER_INIT_WORK_DONE_TOKEN)));
    return res;
  }
}
