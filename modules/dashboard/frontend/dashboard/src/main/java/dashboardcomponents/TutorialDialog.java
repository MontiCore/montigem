package dashboardcomponents;

import dashboardmodule.TutorialStep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TutorialDialog extends TutorialDialogTOP {

  //divide instruction string into smaller strings so they fit the gui loop and print nicely in front
  @Override
  public List<String> getTutorialInstruction() {
    int stepId = this.getTutorialStep();

    for (TutorialStep step : this.getTutorialSteps()) {
      if (step.getStepId() == stepId) {
        List<String> lines = new ArrayList<>();

        for (String line : step.getInstruction().split("\n")) {
          lines.add(line);
        }

        return lines;
      }
    }

    List<String> empty = new ArrayList<>();
    empty.add("");
    return empty;
  }

  @Override
  public String getSectionTitle() {
    int stepId = this.getTutorialStep();

    for (TutorialStep step : this.getTutorialSteps()) {
      if (step.getStepId() == stepId) {
        return step.getSection();
      }
    }

    return "";
  }

  @Override
  public Boolean hasStepButton() {
    int stepId = this.getTutorialStep();

    for (TutorialStep step : this.getTutorialSteps()) {
      if (step.getStepId() == stepId) {
        return step.isHasButton();
      }
    }

    return false;
  }

  @Override
  public Consumer<Void> nextStep(Consumer<Integer> changeTutorialStep) {
    return $ -> {
      changeTutorialStep.accept(1);
    };
  }
}
