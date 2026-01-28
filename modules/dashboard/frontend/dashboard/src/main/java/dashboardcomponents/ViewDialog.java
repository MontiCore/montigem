package dashboardcomponents;

import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.Preset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ViewDialog extends ViewDialogTOP{

  public List<Preset> presets;

  @Override
  public List<Preset> getPresetList () {
    if(presets == null) {
      presets = new ArrayList<>();
      presets.add(DashboardModuleManager.presetBuilder().index(2).source("assets/images/preset1.png").build().get());
      presets.add(DashboardModuleManager.presetBuilder().index(1).source("assets/images/empty-preset.png").build().get());
    }
    return this.presets;
  }

  public Boolean checkCurrentPreset(Preset preset) {
    return this.getSelectedPreset() == preset.getIndex();
  }
  public Consumer<Void> selectPreset(Preset preset) {
    return $ -> {
      this.setSelectedPreset(preset.getIndex());
    };
  }

  public String getPresetSource(Preset preset) {
    return preset.getSource();
  }

  @Override
  public Consumer<Void> closeDialog() {
    return $ -> {
      this.getClose().accept($);
    };
  }

  @Override
  public Consumer<Void> createNewView() {
    return $ -> {
      if (getSelectedPreset() != 0) {
        this.getCreateView().accept(this.getSelectedPreset());
        //tutorial
        if(this.getTutorialStep() != 0) {
          this.getChangeTutorialStep().accept(1);
        }
        this.getClose().accept($);
      }
    };
  }

  @Override
  public Boolean checkIfTutorialStep29() {
    return this.getTutorialStep() == 29;
  }

  @Override
  public Boolean alwaysDisabledByTutorial() {
    return this.getTutorialStep() != 0;
  }

}
