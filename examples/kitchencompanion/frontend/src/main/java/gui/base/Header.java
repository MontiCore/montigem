package gui.base;

import app.AppRouter;
import de.se_rwth.commons.logging.Log;
import def.dom.Globals;

import java.util.function.Consumer;

import static def.dom.Globals.clearInterval;
import static def.dom.Globals.setInterval;

public class Header extends HeaderTOP {
  private double timerInterval;
  private double passedSeconds;

  @Override
  public void init() {
    super.init();
    restartTimer();
  }
  
  @Override
  public void setTimerNeeded(Boolean timerNeeded) {
    super.setTimerNeeded(timerNeeded);
    restartTimer();
  }

  private void restartTimer() {
    clearInterval(this.timerInterval);
    if (isTimerNeeded()) {
      this.setProgress(1f);
      this.passedSeconds = 0;
      Consumer<Void> lambda = $ -> {
        this.passedSeconds = this.passedSeconds + 0.1;
        this.setProgress((float) (1 - (this.passedSeconds / 60)));
        if (passedSeconds > 60) {
          clearInterval(this.timerInterval);
          Log.warn("User timed out go back to main page");
          AppRouter.navigateToGuiKitchenKitchenPage();
        }
      };
      this.timerInterval = setInterval(lambda, 100);
    } else {
      this.setProgress(0f);
    }
  }

  @Override
  public Consumer<Void> navigateBack() {
    return $ -> {
      if (this.getHeadline().equals("Admin Login")) {
        AppRouter.navigateToGuiKitchenKitchenPage();
      } else {
        Globals.history.back();
      }
    };
  }

  @Override
  public Float getTimerProgress() {
    return getProgress();
  }

  @Override
  public void destroy() {
    super.destroy();
    clearInterval(this.timerInterval);
  }

}
