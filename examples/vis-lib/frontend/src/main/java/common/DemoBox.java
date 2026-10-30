/* (c) https://github.com/MontiCore/monticore */
package common;

import jsweet.lang.Replace;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

/**
  Companion of the DemoBox: the collapse toggle plus its persistence.

  <p>The visitor's choice is stored in the browser instead of the domain model -
  it is a display preference of one visitor, not application data, and every
  example page embeds its own DemoBox instance. Without persistence the notes
  would come back on every navigation. Reading happens in {@code init()}, so a
  box that is opened after the notes were hidden elsewhere starts collapsed.

  <p>Both storage helpers are {@code @Replace}d raw JS (the sanctioned escape
  hatch for browser APIs without a JSweet counterpart, like the fetch in
  common.CodeViewer). Both swallow errors - private browsing modes throw on
  access, and a demo box that cannot remember its state is far better than a
  page that fails to render.
*/
@Component
public class DemoBox extends DemoBoxTOP {

  @Override
  public void init() {
    super.init();
    if (readHidden()) {
      setVisible(false);
    }
  }

  public Consumer<Void> toggleVisible() {
    return $ -> {
      boolean nowVisible = !isVisible();
      setVisible(nowVisible);
      writeHidden(!nowVisible);
    };
  }

  @Replace("try { return window.localStorage.getItem('visLibDemoNotesHidden') === '1'; } catch (e) { return false; }")
  private boolean readHidden() {
    return false;
  }

  @Replace("try { window.localStorage.setItem('visLibDemoNotesHidden', hidden ? '1' : '0'); } catch (e) { }")
  private void writeHidden(boolean hidden) {
  }
}
