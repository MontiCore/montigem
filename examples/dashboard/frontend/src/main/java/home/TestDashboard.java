package home;

import java.util.List;

import dashboardmodule.DashboardModuleConfig;
import dashboardmodule.DashboardModuleManager;

public class TestDashboard extends TestDashboardTOP {

  /**
   * Loads the configs from the dashboard module.
   * Page arguments are only loaded automatically for classes of the domain of the application (Dashboard),
   * not for classes of extended domains, such as the DashboardModule
   */
  @Override
  protected void initConfigs() {
    if (!this._configsSetExternally) {
      this.setConfigsInternal(DashboardModuleManager.getDashboardModuleConfigList());
      // recompute values depending on the configs (e.g., the config of the dashboard)
      this.notifyConfigsObservers();
    }
  }

  public DashboardModuleConfig getBackendConfig(List<DashboardModuleConfig> configs) {
    if (configs == null || configs.isEmpty()) {
      return null;
    }
    return configs.get(0);
  }
}
