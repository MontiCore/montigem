package dashboardcomponents;

import dashboardmodule.TutorialStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.Map;
import java.util.HashMap;

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

  //-------------------------------------
  //categorizations of the tutorial steps
  //-------------------------------------

  String[] sectionNames = {
      "1. Introduction",
      "2. Views",
      "3. Creating Cells",
      "4. Editing Cells",
      "5. Outro"
  };

  int[][] sectionSteps = {
      {1, 2, 3},
      {4, 5, 6, 7},
      {8, 9, 10, 11, 12, 13, 14, 15, 16},
      {17, 18, 19, 20, 21, 22, 23, 24, 25, 26},
      {27}
  };

  int[] stepsWithoutButtons = {
      6, 8, 13, 16, 18, 23, 26, 27
  };

  List<List<String>> getInstructionLines() {
    return Arrays.asList(
        //0
        Arrays.asList(
            ""),
        //1
        Arrays.asList(
            "Welcome to the tutorial of the dashboard component."
        ),
        //2
        Arrays.asList(
            "In this tutorial I will show you the main functionalities and parts of",
            "this component and familiarize you with navigating through its layout.",
            "You can cancel the tutorial anytime by clicking the now red tutorial",
            "button again."
        ),
        //3
        Arrays.asList(
            "First main part of the dashboard is the cell.",
            "Any visualization you are seeing on screen now from graphs to charts is",
            "a cell. So basically cells are just containers for any visualization of",
            "the metrics you want."
        ),
        //4
        Arrays.asList(
            "Next part of the dashboard are the views."
        ),
        //5
        Arrays.asList(
            "Each of them represents a container of cells, where you can group them",
            "up together, according to whatever criteria you want. Maybe they are",
            "related to each-other, maybe you just want to divide a chaotic sum of",
            "cells into smaller more digestible parts."
        ),
        //6
        Arrays.asList(
            "For the moment you can ignore the default set of views that are already",
            "there and you can try creating a fresh new one yourself. To do that click",
            "on the now highlighted button."
        ),
        //7
        Arrays.asList(
            "You can change the name of your new view anytime by just clicking over",
            "its tab and modifying the current name. If you do not want to, you can",
            "just directly click next."
        ),
        //8
        Arrays.asList(
            "Now we can try to fill our new view with some cells. To start creating a",
            "new cell click the highlighted plus button on the top right."
        ),
        //9
        Arrays.asList(
            "In this dialog we can start configuring details about our new cell. These",
            "details are divided into 2 parts: the main options and the extra options. "
        ),
        //10
        Arrays.asList(
            "What you are looking at now are the main options. For all visualizations",
            "this step will look the same. You have fields where you can set the name,",
            "type of visualization you want the cell to display, and the data field. "
        ),
        //11
        Arrays.asList(
            "This last data field is the most important field, but also the most",
            "difficult to familiarize yourself with. In short here you write code",
            "in Java to create new or get existing data you want to visualize and",
            "structure it in an appropriate way according to the visualization type."
        ),
        //12
        Arrays.asList(
            "Do not worry on figuring everything out from the start yourself as",
            "whenever you will try to create a cell, these fields will be filled out",
            "with an example, which you can use as a reference for your own use-case.",
            "Also if you make any mistake at any field an error message will appear at",
            "the top to notify you what is wrong with it."
        ),
        //13
        Arrays.asList(
            "Last thing, you can terminate the creation anytime using the abort button",
            "below. Now if everything is clear so far, you can click the other button",
            "next to it to move on to the extra options."
        ),
        //14
        Arrays.asList(
            "What you are viewing now are the extra options of the cell. On the left",
            "you have some new fields that depend on the type of visualization you",
            "choose. Here you can set and change some less important details about",
            "your visualization"
        ),
        //15
        Arrays.asList(
            "Meanwhile on the right you can see a nice preview of the visualization",
            "you are creating. Every change you make to the fields will be also",
            "displayed here, so you can see what your changes actually look like."
        ),
        //16
        Arrays.asList(
            "Now we have gone through the whole creation process of a cell. You can",
            "click on create to see the new cell appear on your view."
        ),
        //17
        Arrays.asList(
            "The last step of this tutorial will be regarding editing cells. We can",
            "try out on our newly created cell."
        ),
        //18
        Arrays.asList(
            "First, click on the now highlighted edit button."
        ),
        //19
        Arrays.asList(
            "Great, now we have new functionalities available to us."
        ),
        //20
        Arrays.asList(
            "Repositioning: You can click and hold on a cell and then move it to a",
            "new position of your choosing."
        ),
        //21
        Arrays.asList(
            "Resizing: You can click and hold at the bottom right of a cell and by",
            "moving you are able to resize the cell how you want."
        ),
        //22
        Arrays.asList(
            "Downloading: You can download the data of the visualization in a json",
            "format or the visualization itself as a png or svg"
        ),
        //23
        Arrays.asList(
            "Reconfiguring: You can change all options of a cell set in creation or",
            "in a previous reconfiguration. To look at that now click on the",
            "highlighted buttton on top of the cell."
        ),
        //24
        Arrays.asList(
            "You can see the popped up window is really similar to previous one we",
            "saw during creation. The only difference is that here you can switch",
            "between the options more easily."
        ),
        //25
        Arrays.asList(
            "Error messages will still pop up if you make mistakes when chaning",
            "fields. Also the abort button continues to be present if you want to",
            "discard all changes and close the window."
        ),
        //26
        Arrays.asList(
            "When you are finished with changes, you can click on the save button to",
            "save those changes and continue."
        ),
        //27
        Arrays.asList(
            "Congratulations! You have completed the tutorial.",
            "Click on the tutorial button to continue to explore and experiment with",
            "the component yourself :)."
        )
    );
  }


}


