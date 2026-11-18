package home;

import jsweet.lang.Replace;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component(tsImports = {"PendingCopy, @angular/cdk/clipboard"})
public class CopyLinkButton extends CopyLinkButtonTOP {

  @Override
  @Replace("""
      return async () => {
        if (this.isCopying()) {
          return;
        }

        this.setCopying(true);
        const text = this.getText();
        let copied = false;
        try {
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
        } catch {
          copied = false;
        } finally {
          this.setCopying(false);
          this.copyMessageChange.emit(copied ? 'Link copied!' : 'Select and copy the link above.');
        }
      };
      """)
  public Consumer<Void> copyLink() {
    return null;
  }
}
