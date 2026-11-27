package home;

import jsweet.lang.Async;
import jsweet.lang.Replace;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component(tsImports = {"PendingCopy, @angular/cdk/clipboard"})
public class CopyLinkButton extends CopyLinkButtonTOP {

  @Override
  public Consumer<Void> copyLink() {
    return VOID -> {
      if (this.isCopying()) {
        return;
      }
      this.setCopying(true);
      String text = this.getText();
      boolean copied = false;
      try {
        copied = doCopy(text);
      } finally {
        this.setCopying(false);
        this.setCopyMessage(copied ? "Link copied!" : "Select and copy the link above.");
      }
    };
  }

  @Async
  @Replace("""
          let copied = false;
          if (navigator.clipboard?.writeText) {
            try {
              await navigator.clipboard.writeText(text);
              copied = true;
            } catch {
              // Fall back to selection-based copying when clipboard access is denied.
            }
          }

          if (!copied) {
            const pendingCopy = new PendingCopy(text, document);
            try {
              copied = pendingCopy.copy();
            } finally {
              pendingCopy.destroy();
            }
          }
          return copied;
      """)
  boolean doCopy(String text) {
    return false;
  }
}
