package dashboardcomponents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.ViewConfig;
import dashboardmodule.TutorialStep;
import de.se_rwth.commons.logging.Log;

import static dashboardmodule.VizType.BAR;
import static dashboardmodule.VizType.LINE;
import static dashboardmodule.VizType.NETWORK;
import static dashboardmodule.VizType.PIE;
import static dashboardmodule.VizType.SCATTER;
import static def.dom.Globals.document;

public class Dashboard extends DashboardTOP {

  @Override
  public Consumer<Void> toggleColors() {
    return $ -> {
      this.setNight(!this.isNight());
      document.body.classList.toggle("dark-theme");
    };
  }

  //------------------------------------
  //left side view tabs related
  //-------------------------------------

  @Override
  public Boolean checkCurrentView(ViewConfig candidate) {
    return (this.getCurrentView() == candidate.getVid());
  }

  public String getCurrentViewWidth(int current, ViewConfig candidate) {
    int baseW = candidate.getName().length() * 8 + 80;
    if (current == candidate.getVid()) {
      int minW = Math.max(baseW, 70);
      int gatedW = Math.min(minW, 180);
      return gatedW + "px";
    }
    return "120px";
  }

  @Override
  public Consumer<Void> changeCurrentView(ViewConfig view) {
    return $ -> {
      this.setCurrentView(view.getVid());
    };
  }

  public Boolean checkIfSingleView() { return this.getConfig().getViewsList().size() > 1; }

  @Override
  public Consumer<Void> openViewDialog() {
    return $ -> {
      this.setCreateView(true);
      //tutorial
      if(this.getTutorialStep() == 6) {
        this.changeTutorialStep().accept(1);
      }
    };
  }

  @Override
  public Consumer<Void> closeViewDialog() {
    return $ -> {
      this.setCreateView(false);
    };
  }

  @Override
  public Consumer<Void> deleteView(ViewConfig view) {
    return $ -> {
      List<ViewConfig> list = DashboardModuleManager.getDashboardModuleConfigSure(this.getConfig().getGemId()).getViewsList();
      //int getVid
      int newIndex = list.get(list.indexOf(view)-1).getVid();
      setCurrentView(newIndex);
      this.getConfig().removeViews(view);
      DashboardModuleManager.deleteViewConfig(view.getGemId());
    };
  }

  @Override
  public Consumer<Integer> createNewView() {
    return preset -> {
      if (this.getConfig().getViewsList().size() >= 6) {
        this.setInfo(true);
        return;
      }
      Set<CellConfig> emptyCells = new HashSet<>();
      int nvid = (int)DashboardModuleManager.getFreeGemId();
      ViewConfig nview = DashboardModuleManager.viewConfigBuilder()
          .name("New View")
          .vid(nvid)
          .cells(preset == 2 ? createPreset1() : emptyCells)
          .build()
          .get();
      this.getConfig().addViews(nview);
      this.setCurrentView(nvid);
      //tutorial-only relevant step
      if (this.getTutorialStep() == 6) {
        this.setTutorialStep(7);
      }
    };
  }

  //view create functions of presets
  public Set<CellConfig> createPreset1() {
    Set<CellConfig> cells = new HashSet<CellConfig>();
    CellConfig cell0 = DashboardModuleManager.cellConfigBuilder()
        .data(defaultNetworkData)
        .name("Group Structure")
        .viz(NETWORK)
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .height(8.0)
            .width(6.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(0.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();
    CellConfig cell1 = DashboardModuleManager.cellConfigBuilder()
        .data(defaultBarData)
        .name("Lines of Code")
        .viz(BAR)
        .extra(DashboardModuleManager.extraDataBuilder()
            .barextra(DashboardModuleManager.barExtraDataBuilder()
                .maxValue(Optional.empty())
                .minValue(0)
                .overlapping(false)
                .stacked(false)
                .build()
                .get()
            )
            .build()
            .get()
        )
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .height(4.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(6.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();
    CellConfig cell2 = DashboardModuleManager.cellConfigBuilder()
        .data(defaultLineData)
        .name("No. of Issues Closed")
        .viz(LINE)
        .extra(DashboardModuleManager.extraDataBuilder()
            .linextra(DashboardModuleManager.lineExtraDataBuilder()
                .minValue(0)
                .maxValue(Optional.empty())
                .build()
                .get()
            )
            .build()
            .get()
        )
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .height(4.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(6.0)
            .y(4.0)
            .build()
            .get())
        .build()
        .get();
    CellConfig cell3 = DashboardModuleManager.cellConfigBuilder()
        .data(defaultPieData)
        .name("No. of Commits")
        .viz(PIE)
        .extra(DashboardModuleManager.extraDataBuilder()
            .piextra(DashboardModuleManager.pieExtraDataBuilder()
                .innerRadius(0)
                .build()
                .get())
            .build()
            .get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .height(4.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(9.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();
    CellConfig cell4 = DashboardModuleManager.cellConfigBuilder()
        .data(defaultScatterData)
        .name("No. of issues closed")
        .viz(SCATTER)
        .extra(DashboardModuleManager.extraDataBuilder()
            .scatterextra(DashboardModuleManager.scatterExtraDataBuilder()
                .xAxis(DashboardModuleManager.axisBuilder()
                    .label("X-Axis Label")
                    .stepWidth(999d)
                    .maxValue(Optional.empty())
                    .minValue(0d)
                    .build()
                    .get()
                )
                .yAxis(DashboardModuleManager.axisBuilder()
                    .label("Y-Axis Label")
                    .stepWidth(999d)
                    .maxValue(Optional.empty())
                    .minValue(0d)
                    .build()
                    .get()
                )
                .build()
                .get()
            )
        )
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .height(4.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(9.0)
            .y(4.0)
            .build()
            .get())
        .build()
        .get();
    cells.add(cell0);
    cells.add(cell1);
    cells.add(cell2);
    cells.add(cell3);
    cells.add(cell4);
    return cells;
  }

  //-------------------------------------
  //right side buttons related
  //--------------------------------------

  @Override
  public Consumer<Void> changeEdit() {
    return $ -> {
      boolean current = this.isEdit();
      this.setEdit(!current);
      //tutorial-only related
      if (this.getTutorialStep() == 18) {
        this.setTutorialStep(19);
      }
    };
  }

  @Override
  public Consumer<Void> changeCreate() {
    return $ -> {
      boolean current = this.isCreateCell();
      this.setCreateCell(!current);
      //tutorial-only relevant step
      if(this.getTutorialStep() == 8) {
        this.setTutorialStep(9);
      }
    };
  }

  @Override
  public Consumer<Void> closeInfo() {
    return $ -> {
      this.setInfo(false);
    };
  }

  //------------------------------------------------------------
  //set functions for parameters to be passed down to components
  //------------------------------------------------------------

  @Override
  public Consumer<Boolean> changeDelete() {
    return value -> {
      this.setDelete(value);
    };
  }

  @Override
  public Consumer<Boolean> changeRunning() {
   return value -> {
     this.setRunning(value);
   };
  }

  @Override
  public Consumer<Boolean> changeDownload() {
    return value -> {
      this.setDownload(value);
    };
  }

  @Override
  public Consumer<Void> closeEdit() {
    return $ -> {
      this.setEditCellFlag(false);
    };
  }

  @Override
  public Consumer<Void> setCreateFalse() {
    return $ -> {
      this.setCreateCell(false);
    };
  }

  //
  //button disabling related
  //
  @Override
  public Boolean checkIfDialogOpen() { return (this.isCreateCell() || this.isEditCellFlag() || this.getTutorialStep() != 0 || this.isRunning() || this.isDelete() || this.isDownload()); }


  //---------------------------------
  //tutorial related section
  //----------------------------------
  @Override
  public Consumer<Void> switchTutorial() {
    return $ -> {
      int current = this.getTutorialStep();
      if(current == 0) {
        this.setTutorialStep(1);
        this.setEdit(false);
      } else {
        this.setTutorialStep(0);
      }
    };
  }

  @Override
  public Consumer<Integer> changeTutorialStep() {
    return numberOfSteps -> {
      this.setTutorialStep(getTutorialSteps().get(searchTutorialStep(this.getTutorialStep()) + numberOfSteps).getStepId());
    };
  }

  public int searchTutorialStep(int id) {
    List<TutorialStep> steps = getTutorialSteps();

    for (int i = 0; i < steps.size(); i++) {
      if (steps.get(i).getStepId() == id) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public Boolean checkIfDialogOpenCurrentViewTutorialStep7() {
    return checkIfDialogOpen() && this.getTutorialStep() != 7;
  }

  @Override
  public Boolean checkTutorial() { return this.getTutorialStep() > 0; }

  @Override
  public Boolean checkIfTutorialStep45() { return (this.getTutorialStep() == 4 || this.getTutorialStep() == 5); }

  @Override
  public Boolean checkIfTutorialStep6() { return this.getTutorialStep() == 6; }

  @Override
  public Boolean checkIfTutorialStep8() { return this.getTutorialStep() == 8; }

  @Override
  public Boolean checkIfTutorialStep18() { return this.getTutorialStep() == 18; }

  @Override
  public Boolean checkIfTutorialHasBackdrop() {
    int stepId = this.getTutorialStep();

    for (TutorialStep step : this.getTutorialSteps()) {
      if (step.getStepId() == stepId) {
        return step.isHasBackdrop();
      }
    }
    return false;
  }

  @Override
  public Boolean checkIfTutorialDialogIsTop() {
    int stepId = this.getTutorialStep();

    for (TutorialStep step : this.getTutorialSteps()) {
      if (step.getStepId() == stepId) {
        return !step.isBottom();
      }
    }
    return true;
  }

  public List<TutorialStep> tutorialSteps;

  @Override
  public List<TutorialStep> getTutorialSteps() {
    if(tutorialSteps == null) {
        tutorialSteps = new ArrayList<>();
        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(1)
                .instruction("Welcome to the tutorial of the dashboard component.")
                .section("1. Introduction")
                .hasButton(true)
                .hasBackdrop(true)
                .bottom(false)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(2)
                .instruction(
                    "In this tutorial I will show you the main functionalities and parts of\n" +
                        "this component and familiarize you with navigating through its layout.\n" +
                        "You can cancel the tutorial anytime by clicking the now red tutorial\n" +
                        "button again."
                )
                .section("1. Introduction")
                .hasButton(true)
                .hasBackdrop(true)
                .bottom(false)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(3)
                .instruction(
                    "First main part of the dashboard is the cell.\n" +
                        "Any visualization you are seeing on screen now from graphs to charts is\n" +
                        "a cell. So basically cells are just containers for any visualization of\n" +
                        "the metrics you want."
                )
                .section("1. Introduction")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(4)
                .instruction("Next part of the dashboard are the views.")
                .section("2. Views")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(5)
                .instruction(
                    "Each of them represents a container of cells, where you can group them\n" +
                        "up together, according to whatever criteria you want. Maybe they are\n" +
                        "related to each-other, maybe you just want to divide a chaotic sum of\n" +
                        "cells into smaller more digestible parts."
                )
                .section("2. Views")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(6)
                .instruction(
                    "For the moment you can ignore the default set of views that are already\n" +
                        "there and you can try creating a fresh new one yourself. To do that click\n" +
                        "on the now highlighted button."
                )
                .section("2. Views")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(28)
                .instruction(
                    "In this dialog you can choose from one of the presets to get started\n" +
                        "already with some configuration of visualizations. Otherwise you can start\n" +
                        "with a clean slate by choosing the empty preset. For this tutorial please" +
                        "select the empty preset."
                )
                .section("2. Views")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(29)
                .instruction(
                    "To make the tutorial easier for you select the empty preset and press the now \n" +
                        "highlighted \"Create\" button to continue\n"
                )
                .section("2. Views")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(false)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(7)
                .instruction(
                    "You can change the name of your new view anytime by just clicking over\n" +
                        "its tab and modifying the current name. If you do not want to, you can\n" +
                        "just directly click next."
                )
                .section("2. Views")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(8)
                .instruction(
                    "Now we can try to fill our new view with some cells. To start creating a\n" +
                        "new cell click the highlighted plus button on the top right."
                )
                .section("3. Creating Cells")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(9)
                .instruction(
                    "In this dialog we can start configuring details about our new cell. These\n" +
                        "details are divided into 2 parts: the main options and the extra options."
                )
                .section("3. Creating Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(10)
                .instruction(
                    "What you are looking at now are the main options. For all visualizations\n" +
                        "this step will look the same. You have fields where you can set the name,\n" +
                        "type of visualization you want the cell to display, and the data field."
                )
                .section("3. Creating Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(11)
                .instruction(
                    "This last data field is the most important field, but also the most\n" +
                        "difficult to familiarize yourself with. In short here you write code\n" +
                        "in Java to create new or get existing data you want to visualize and\n" +
                        "structure it in an appropriate way according to the visualization type."
                )
                .section("3. Creating Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(12)
                .instruction(
                    "Do not worry on figuring everything out from the start yourself as\n" +
                        "whenever you will try to create a cell, these fields will be filled out\n" +
                        "with an example, which you can use as a reference for your own use-\n" +
                        "case. Also if you make any mistake at any field an error message will\n" +
                        "appear at the top to notify you what is wrong with it."
                )
                .section("3. Creating Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );


        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(13)
                .instruction(
                    "Last thing, you can terminate the creation anytime using the abort button\n" +
                        "below. Now if everything is clear so far, you can click the other button\n" +
                        "next to it to move on to the extra options."
                )
                .section("3. Creating Cells")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(14)
                .instruction(
                    "What you are viewing now are the extra options of the cell. On the left\n" +
                        "you have some new fields that depend on the type of visualization you\n" +
                        "choose. Here you can set and change some less important details about\n" +
                        "your visualization"
                )
                .section("3. Creating Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(15)
                .instruction(
                    "Meanwhile on the right you can see a nice preview of the visualization\n" +
                        "you are creating. Every change you make to the fields will be also\n" +
                        "displayed here, so you can see what your changes actually look like."
                )
                .section("3. Creating Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(16)
                .instruction(
                    "Now we have gone through the whole creation process of a cell. You can\n" +
                        "click on create to see the new cell appear on your view."
                )
                .section("3. Creating Cells")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(17)
                .instruction(
                    "The last step of this tutorial will be regarding editing cells. We can\n" +
                        "try out on our newly created cell."
                )
                .section("4. Editing Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(false)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(18)
                .instruction(
                    "First, click on the now highlighted edit button."
                )
                .section("4. Editing Cells")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(false)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(19)
                .instruction(
                    "Great, now we have new functionalities available to us."
                )
                .section("4. Editing Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(20)
                .instruction(
                    "Repositioning: You can click and hold on a cell and then move it to a\n" +
                        "new position of your choosing."
                )
                .section("4. Editing Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(21)
                .instruction(
                    "Resizing: You can click and hold at the bottom right of a cell and by\n" +
                        "moving you are able to resize the cell how you want."
                )
                .section("4. Editing Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(22)
                .instruction(
                    "Downloading: You can download the data of the visualization in a json\n" +
                        "format or the visualization itself as a png or svg"
                )
                .section("4. Editing Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(23)
                .instruction(
                    "Reconfiguring: You can change all options of a cell set in creation or\n" +
                        "in a previous reconfiguration. To look at that now click on the\n" +
                        "highlighted buttton on top of the cell."
                )
                .section("4. Editing Cells")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(24)
                .instruction(
                    "You can see the popped up window is really similar to previous one we\n" +
                        "saw during creation. The only difference is that here you can switch\n" +
                        "between the options more easily."
                )
                .section("4. Editing Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(25)
                .instruction(
                    "Error messages will still pop up if you make mistakes when chaning\n" +
                        "fields. Also the abort button continues to be present if you want to\n" +
                        "discard all changes and close the window."
                )
                .section("4. Editing Cells")
                .hasButton(true)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(26)
                .instruction(
                    "When you are finished with changes, you can click on the save button to\n" +
                        "save those changes and continue."
                )
                .section("4. Editing Cells")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(true)
                .build().get()
        );

        tutorialSteps.add(
            DashboardModuleManager.tutorialStepBuilder()
                .stepId(27)
                .instruction(
                    "Congratulations! You have completed the tutorial.\n" +
                        "Click on the tutorial button to continue to explore and experiment with\n" +
                        "the component yourself :)."
                )
                .section("5. Outro")
                .hasButton(false)
                .hasBackdrop(false)
                .bottom(false)
                .build().get()
        );
    }
    return tutorialSteps;
  }

  //----------------------------------------
  //default data for visualizations (not dependent on data model)
  //----------------------------------------
  String defaultNetworkData = """
      GraphNode node1 = GraphNodeBuilder().id("1").label("Frontend").fixed(Optional.of(true)).x(Optional.of(2.5)).y(Optional.of(2.5)).size(Optional.of(40)).build().get();
      GraphNode node2 = GraphNodeBuilder().id("2").label("Team 1").fixed(Optional.of(true)).x(Optional.of(1.0)).y(Optional.of(1.0)).size(Optional.of(40)).build().get();
      GraphNode node3 = GraphNodeBuilder().id("3").label("Team 2").fixed(Optional.of(true)).x(Optional.of(4.0)).y(Optional.of(1.0)).size(Optional.of(40)).build().get();
      GraphNode node4 = GraphNodeBuilder().id("4").label("Team 3").fixed(Optional.of(true)).x(Optional.of(2.5)).y(Optional.of(4.0)).size(Optional.of(40)).build().get();
      GraphNode node5 = GraphNodeBuilder().id("5").label("Marc").fixed(Optional.of(true)).x(Optional.of(1.0)).y(Optional.of(4.0)).size(Optional.of(40)).build().get();
      GraphNode node6 = GraphNodeBuilder().id("6").label("Constantin").fixed(Optional.of(true)).x(Optional.of(4.0)).y(Optional.of(4.0)).size(Optional.of(40)).build().get();
      GraphNode node7 = GraphNodeBuilder().id("7").label("Kelsi").fixed(Optional.of(true)).x(Optional.of(2.5)).y(Optional.of(5.5)).size(Optional.of(40)).build().get();
      List<GraphNode> nodes = [node1, node2, node3, node4, node5, node6, node7];
      GraphEdge edge1 = GraphEdgeBuilder().id("1").source("1").target("2").size(Optional.of(5)).build().get();
      GraphEdge edge2 = GraphEdgeBuilder().id("2").source("1").target("3").size(Optional.of(5)).build().get();
      GraphEdge edge3 = GraphEdgeBuilder().id("3").source("1").target("4").size(Optional.of(5)).build().get();
      GraphEdge edge4 = GraphEdgeBuilder().id("4").source("2").target("5").size(Optional.of(5)).build().get();
      GraphEdge edge5 = GraphEdgeBuilder().id("5").source("2").target("6").size(Optional.of(5)).build().get();
      GraphEdge edge6 = GraphEdgeBuilder().id("6").source("2").target("7").size(Optional.of(5)).build().get();
      List<GraphEdge> edges = [edge1, edge2, edge3, edge4, edge5, edge6];
      return GraphBuilder().nodes(nodes).edges(edges).build().get();
      """;

  String defaultBoxData = """
      List<Double> points1 = [4.0, 7.0, 8.0, 3.0, 1.0, 5.0, 4.0, 5.0];
      GemBoxPlotBox box1 = GemBoxPlotBoxBuilder().median(4.5).lowerQuantile(3.0).upperQuantile(7.0).lowerExtreme(1.0).upperExtreme(8.0).build().get();
      List<Double> points2 = [2.0, 5.0, 3.0, 3.0, 3.0, 8.0, 9.0, 4.0];
      GemBoxPlotBox box2 = GemBoxPlotBoxBuilder().median(3.5).lowerQuantile(3.0).upperQuantile(8.0).lowerExtreme(2.0).upperExtreme(9.0).build().get();
      List<Double> points3 = [4.0, 4.0, 4.0, 5.0, 3.0, 4.0, 5.0, 2.0];
      GemBoxPlotBox box3 = GemBoxPlotBoxBuilder().median(4.0).lowerQuantile(3.0).upperQuantile(5.0).lowerExtreme(2.0).upperExtreme(5.0).build().get();
      GemBoxPlotEntry entry1 = GemBoxPlotEntryBuilder().group("Team 1").color(Optional.of("red")).points(points1).box(box1).build().get();
      GemBoxPlotEntry entry2 = GemBoxPlotEntryBuilder().group("Team 2").color(Optional.of("green")).points(points2).box(box2).build().get();
      GemBoxPlotEntry entry3 = GemBoxPlotEntryBuilder().group("Team 3").color(Optional.of("blue")).points(points3).box(box3).build().get();
      List<GemBoxPlotEntry> entries = [entry1, entry2, entry3];
      return GemBoxPlotDataBuilder().entries(entries).build().get();
      """;

  String defaultPieData = """
     GemPieChartEntry entry1 = GemPieChartEntryBuilder().value(30).label("Team 1").build().get();
     GemPieChartEntry entry2 = GemPieChartEntryBuilder().value(60).label("Team 2").build().get();
     GemPieChartEntry entry3 = GemPieChartEntryBuilder().value(10).label("Team 3").build().get();
     List<GemPieChartEntry> entries = [entry1, entry2, entry3];
     return GemPieChartDataBuilder().entries(entries).build().get();
     """;

  String defaultSpeedData = """
      GemGaugeChartEntry entry1 = GemGaugeChartEntryBuilder().type("#friends").name("Avg Performance Score").value(8).build().get();
      List<GemGaugeChartEntry> entries = [entry1];
      return GemGaugeChartDataBuilder().entries(entries).build().get();
      """;

  String defaultLineData = """
      List<String> labels = ["Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Week 7", "Week 8"];
      List<Double> entrydata1 = [4.0, 7.0, 8.0, 3.0, 1.0, 5.0, 4.0, 5.0];
      List<Double> entrydata2 = [2.0, 5.0, 3.0, 3.0, 3.0, 8.0, 9.0, 4.0];
      List<Double> entrydata3 = [4.0, 4.0, 4.0, 5.0, 3.0, 4.0, 5.0, 2.0];
      GemLineChartEntry entry1 = GemLineChartEntryBuilder().label("Team 1").data(entrydata1).build().get();
      GemLineChartEntry entry2 = GemLineChartEntryBuilder().label("Team 2").data(entrydata2).build().get();
      GemLineChartEntry entry3 = GemLineChartEntryBuilder().label("Team 3").data(entrydata3).build().get();
      List<GemLineChartEntry> entries = [entry1, entry2, entry3];
      return GemLineChartDataBuilder().entries(entries).labels(labels).build().get();
      """;

  String defaultBarData = """
      List<String> labels = [""];
      List<Integer> numbers1 = [576];
      List<Integer> numbers2 = [455];
      List<Integer> numbers3 = [505];
      GemBarChartEntry entry1 = GemBarChartEntryBuilder().data(numbers1).label(Optional.of("Team 1")).build().get();
      GemBarChartEntry entry2 = GemBarChartEntryBuilder().data(numbers2).label(Optional.of("Team 2")).build().get();
      GemBarChartEntry entry3 = GemBarChartEntryBuilder().data(numbers3).label(Optional.of("Team 3")).build().get();
      List<GemBarChartEntry> entries = [entry1, entry2, entry3];
      return GemBarChartDataBuilder().entries(entries).labels(labels).build().get();
      """;

  String defaultScatterData = """
     List<GemScatterPlotPoint> points1 = [GemScatterPlotPoint(1.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(2.0, 7.0, "", Optional.of(8)), GemScatterPlotPoint(3.0, 8.0, "", Optional.of(8)), GemScatterPlotPoint(4.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(5.0, 1.0, "", Optional.of(8)), GemScatterPlotPoint(6.0, 5.0, "", Optional.of(8)), GemScatterPlotPoint(7.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(8.0, 5.0, "", Optional.of(8))];
     List<GemScatterPlotPoint> points2 = [GemScatterPlotPoint(1.0, 2.0, "", Optional.of(8)), GemScatterPlotPoint(2.0, 5.0, "", Optional.of(8)), GemScatterPlotPoint(3.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(4.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(5.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(6.0, 8.0, "", Optional.of(8)), GemScatterPlotPoint(7.0, 9.0, "", Optional.of(8)), GemScatterPlotPoint(8.0, 4.0, "", Optional.of(8))];
     List<GemScatterPlotPoint> points3 = [GemScatterPlotPoint(1.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(2.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(3.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(4.0, 5.0, "", Optional.of(8)), GemScatterPlotPoint(5.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(6.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(7.0, 5.0, "", Optional.of(8)), GemScatterPlotPoint(8.0, 2.0, "", Optional.of(8))];
     GemScatterPlotSet set1 = GemScatterPlotSetBuilder().label("Team 1").points(points1).build().get();
     GemScatterPlotSet set2 = GemScatterPlotSetBuilder().label("Team 2").points(points2).build().get();
     GemScatterPlotSet set3 = GemScatterPlotSetBuilder().label("Team 3").points(points3).build().get();
     List<GemScatterPlotSet> sets = [set1, set2, set3];
     return GemScatterPlotDataBuilder().sets(sets).build().get();
     """;

}
