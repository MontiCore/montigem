package home;

import java.util.List;
import java.util.function.Consumer;

import chatmodule.ChatModuleManager;
import dashboardmodule.DashboardModuleConfig;
import dashboardmodule.ViewConfig;
import umlp.jsweet.extension.annotation.Component;
import dashboardmodule.DashboardModuleManager;

@Component
public class TestDashboard extends TestDashboardTOP {

  @Override
  public Consumer<Integer> changeCurrentView() {
    return value -> setCurrentView(value);
  }

  @Override
  protected void initChatHistory() {
    super.initChatHistory();
    if (!this._chatHistorySetExternally) {
      this.setChatHistoryInternal(ChatModuleManager.messageHistoryBuilder()
          .name("Dashboard chat").build().get());
      this.notifyChatHistoryObservers();
    }
  }

  /**
   * Loads the configs from the dashboard module.
   * Page arguments are only loaded automatically for classes of the domain of the application (Dashboard),
   * not for classes of extended domains, such as the DashboardModule
   */
  @Override
  protected void initConfigs() {
    super.initConfigs();
    if (!this._configsSetExternally) {
      this.setConfigsInternal(DashboardModuleManager.getDashboardModuleConfigList());
      // recompute values depending on the configs (e.g., the config of the dashboard)
      this.notifyConfigsObservers();
    }
  }

  @Override
  public DashboardModuleConfig getBackendConfig(List<DashboardModuleConfig> configs) {
    if (configs == null || configs.isEmpty()) {
      return null;
    }
    return configs.get(0);
  }

  @Override
  public Long getChatContextId(List<DashboardModuleConfig> configs, Integer currentView) {
    ViewConfig view = getCurrentDashboardView(configs, currentView);
    return view == null ? 0L : view.getGemId();
  }

  public ViewConfig getCurrentDashboardView(List<DashboardModuleConfig> configs, Integer currentView) {
    DashboardModuleConfig config = getBackendConfig(configs);
    if (config != null) {
      for (ViewConfig view : config.getViewsList()) {
        if (view.getVid() == currentView) {
          return view;
        }
      }
    }
    return null;
  }
}
