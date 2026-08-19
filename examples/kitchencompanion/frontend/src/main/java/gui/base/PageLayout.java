package gui.base;

import def.dom.Globals;

public class PageLayout extends PageLayoutTOP {
  @Override
  public void init() {
    super.init();
    setDocumentOverflowY("hidden");
  }

  @Override
  public void destroy() {
    setDocumentOverflowY("");
    super.destroy();
  }

  private void setDocumentOverflowY(String value) {
    Globals.document.documentElement.style.overflowY = value;
  }
}
