package gui.test;

import jsweettest.JsweettestManager;
import umlp.backendrte.common.GeneralErrorCollector;
import umlp.backendrte.common.GeneralErrorObserver;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component
public class TestGeneralErrorCollector extends TestGeneralErrorCollectorTOP {

  @Override
  public void init() {
    super.init();

    GeneralErrorCollector.addErrorObserver(new GeneralErrorObserver() {
      @Override
      public void notifyConnectionError(String msg) {
        System.err.println("notifyConnectionError" + msg);
      }

      @Override
      public void notifyServerError(String msg) {
        System.err.println("notifyServerError" + msg);
      }

      @Override
      public void notifySyncError(String msg) {
        System.err.println("notifySyncError" + msg);
      }
    });
  }

  public Consumer<Void> raiseSyncError() {
    return unused -> {
      GeneralErrorCollector.reportSyncError();
    };
  }

  public Consumer<Void> addC2() {
    return unused -> {
      JsweettestManager.c2Builder()
          // strings
          .string("C2#fromTest2")
          // ints
          .int1(42)
          .int2(4242)
          .build().get();
    };
  }
}
