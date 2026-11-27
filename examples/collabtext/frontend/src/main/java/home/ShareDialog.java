package home;

import def.dom.Element;
import jsweet.lang.Replace;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

import static def.dom.Globals.document;
import static def.dom.Globals.window;

@Component(styleUrls = {"...config.styleUrls"}, tsImports = {"default as qrcode, qrcode-generator"})
public class ShareDialog extends ShareDialogTOP {

  @Override
  public Consumer<Void> openShare() {
    return event -> {
      String url = window.location.href;
      setShareUrl(url);
      setCopyMessage("");
      try {
        setQrCodeUrl(createQrCode(url));
        setQrAvailable(true);
      }
      catch (Exception e) {
        setQrAvailable(false);
      }
      setDialogOpen(true);
      window.requestAnimationFrame(time -> {
        Element status = document.querySelector(".collabSharePanel .copyLinkStatus");
        if (status != null) {
          status.setAttribute("role", "status");
        }
      });
    };
  }

  @Override
  public Consumer<Void> closeShare() {
    return event -> setDialogOpen(false);
  }

  @Replace("""
      const code = qrcode(0, 'M');
      code.addData(url);
      code.make();
      return code.createDataURL(6, 24);
      """)
  private String createQrCode(String url) {
    return null;
  }
}
