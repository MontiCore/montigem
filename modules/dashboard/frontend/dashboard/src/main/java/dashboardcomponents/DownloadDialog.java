package dashboardcomponents;

import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleBaseClassObjectMapper;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.ViewConfig;
import dashboardmodule.VizType;
import dashboardmodule.commands.InterpretBarChart;
import dashboardmodule.commands.InterpretBoxPlot;
import dashboardmodule.commands.InterpretGaugeChart;
import dashboardmodule.commands.InterpretLineChart;
import dashboardmodule.commands.InterpretNetworkGraph;
import dashboardmodule.commands.InterpretPieChart;
import dashboardmodule.commands.InterpretScatterPlot;
import de.se_rwth.commons.logging.Log;
import def.dom.CanvasRenderingContext2D;
import def.dom.HTMLCanvasElement;
import def.dom.HTMLElement;
import def.dom.NodeList;
import mc.fenix.charts.gembarcharttypes.GemBarChartDataBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotBox;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotBoxBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotData;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotDataBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotEntry;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotEntryBuilder;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartDataBuilder;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartEntry;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartEntryBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.charts.gemlinecharttypes.GemLineChartDataBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntry;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntryBuilder;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntry;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntryBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartDataBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntry;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntryBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotData;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotDataBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotPoint;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSet;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSetBuilder;
import mc.fenix.networkgraph.networkgraphtypes.Graph;
import mc.fenix.networkgraph.networkgraphtypes.GraphBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdge;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdgeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphNode;
import mc.fenix.networkgraph.networkgraphtypes.GraphNodeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.NodePos;
import mc.fenix.networkgraph.networkgraphtypes.NodePosBuilder;
import static def.dom.Globals.document;
import static jsweet.util.Lang.union;

import def.dom.URL;
import def.dom.Blob;
import def.js.JSON;
import def.dom.HTMLAnchorElement;
import umlp.backendrte.command.CommandManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DownloadDialog extends DownloadDialogTOP{

  //----------------------------------------------------------------------------------------------------
  //manual json reformatting as the response from serializers is currently not in the usual nice format
  //not 100% future-proof for any visualization; so to check if new ones are added
  //----------------------------------------------------------------------------------------------------
  public Consumer<Void> downloadCellJson() {
    return $ -> {
      String json = getDataInterpretation(this.getCell());
      if(json == null || json.isEmpty()) {
        this.setErrorMessage("The chosen cell's visualization is not supported currently for Json downloading");
        this.setError(true);
        return;
      }
      def.js.Object parsed = (def.js.Object) JSON.parse(json);
      def.js.Array<Object> oldEntries = (def.js.Array<Object>) parsed.$get("entries");
      Object[] newEntries = new Object[oldEntries.length];

      for (int i = 0; i < oldEntries.length; i++) {
        String entryString = (String) oldEntries.$get(i);
        Object entryObject = JSON.parse((String) JSON.parse(entryString));
        newEntries[i] = entryObject;
      }

      parsed.$set("entries", newEntries);
      String fixedJson = JSON.stringify(parsed, (Object[]) null, 2);

      HTMLAnchorElement a = (HTMLAnchorElement) document.createElement("a");
      a.href = "data:application/json;charset=utf-8," + fixedJson;
      a.setAttribute("download", this.getCell().getName() + ".json");
      a.click();
    };
  }

  //helper functions/wrappers for json serializers for each visualization data type
  public String getDataInterpretation(CellConfig cell) {
    switch (cell.getViz()) {
      case LINE:
        return serializeLineChartData(CommandManager.executeCommand(new InterpretLineChart(this.getCell().getData())).asSimpleResult().get());
      case BAR:
        return serializeBarChartData(CommandManager.executeCommand(new InterpretBarChart(this.getCell().getData())).asSimpleResult().get());
      case SCATTER:
        return serializeScatterPlotData(CommandManager.executeCommand(new InterpretScatterPlot(this.getCell().getData())).asSimpleResult().get());
      case SPEED:
        return serializeGaugeChartData(CommandManager.executeCommand(new InterpretGaugeChart(this.getCell().getData())).asSimpleResult().get());
      case BOX:
        return serializeBoxPlotData(CommandManager.executeCommand(new InterpretBoxPlot(this.getCell().getData())).asSimpleResult().get());
      case PIE:
        return serializePieChartData(CommandManager.executeCommand(new InterpretPieChart(this.getCell().getData())).asSimpleResult().get());
      case NETWORK:
        return serializeNetworkGraph(CommandManager.executeCommand(new InterpretNetworkGraph(this.getCell().getData())).asSimpleResult().get());
    }
    return "Visualization type of the cell is not accounted for in the cases";
  }

  private String serializePieChartData(GemPieChartData data) {
    return DashboardModuleBaseClassObjectMapper.serialize(data, "mc.fenix.charts.gempiecharttypes.GemPieChartData").get();
  }

  private String serializeBarChartData(GemBarChartData data) {
    return DashboardModuleBaseClassObjectMapper.serialize(data, "mc.fenix.charts.gembarcharttypes.GemBarChartData").get();
  }

  private String serializeLineChartData(GemLineChartData data) {
    return DashboardModuleBaseClassObjectMapper.serialize(data, "mc.fenix.charts.gemlinecharttypes.GemLineChartData").get();
  }

  private String serializeBoxPlotData(GemBoxPlotData data) {
    return DashboardModuleBaseClassObjectMapper.serialize(data, "mc.fenix.charts.gemboxplottypes.GemBoxPlotData").get();
  }

  private String serializeGaugeChartData(GemGaugeChartData data) {
    return DashboardModuleBaseClassObjectMapper.serialize(data, "mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData").get();
  }

  private String serializeScatterPlotData(GemScatterPlotData data) {
    return DashboardModuleBaseClassObjectMapper.serialize(data, "mc.fenix.charts.gemscatterplottypes.GemScatterPlotData").get();
  }

  private String serializeNetworkGraph(Graph data) {
    return DashboardModuleBaseClassObjectMapper.serialize(data, "mc.fenix.networkgraph.networkgraphtypes.Graph").get();
  }

  //--------------------------------------------------------------------------------
  //legend positioning done to best fit current visualizations and their usual sizes
  //not 100% future-proof for any visualization; so to check if new ones are added
  //--------------------------------------------------------------------------------

  public Consumer<Void> downloadCellPng() {
    return $ -> {
      Log.warn("Png function called");

      String targetId = "cell-container-" + this.getCell().getGemId();
      HTMLElement element = (HTMLElement) document.getElementById(targetId);

      HTMLCanvasElement internalCanvas = (HTMLCanvasElement) element.querySelector("canvas");
      HTMLElement legend = (HTMLElement) element.querySelector(".gem-legend");

      if(internalCanvas == null) {
          this.setErrorMessage("The chosen cell's visualization is not supported currently for PNG downloading. Maybe try the download feature within the visualization.");
          this.setError(true);
          return;
      }

      HTMLCanvasElement canvas = (HTMLCanvasElement) document.createElement("canvas");
      canvas.width = internalCanvas.width;
      canvas.height = internalCanvas.height + 100;

      CanvasRenderingContext2D context = (CanvasRenderingContext2D) canvas.getContext("2d");

      context.scale(1,1);
      context.fillStyle = union("#ffffff");
      context.fillRect(0, 0, canvas.width, canvas.height);
      context.drawImage(internalCanvas, 0, 0);

      if (legend != null) {
        NodeList entries = legend.querySelectorAll(".gem-entries");

        int spacing = 120;
        int startX = 20;
        int startY = (int) internalCanvas.height + 40;

        for (int i = 0; i < entries.length; i++) {

          HTMLElement entry = (HTMLElement) entries.item(i);
          HTMLElement colorDiv = (HTMLElement) entry.querySelector(".gem-color");
          HTMLElement labelDiv = (HTMLElement) entry.querySelector(".gem-label");
          String color = colorDiv.getAttribute("style");
          String label = labelDiv.textContent;

          int x = startX + i * spacing;
          int y = startY;

          context.fillStyle = union(extractColor(color));
          context.beginPath();
          context.arc(x, y, 6, 0, 2 * Math.PI);
          context.fill();
          context.fillStyle = union("#000000");
          context.fillText(label, x + 15, y + 4);
        }
      }
      triggerCanvasDownload(canvas, this.getCell().getName());
    };
  }

  public String extractColor(String style) {
    int start = style.indexOf("rgb");
    if (start == -1) return "#000000";

    int end = style.indexOf(")", start);
    return style.substring(start, end + 1);
  }

  public void triggerCanvasDownload(HTMLCanvasElement canvas, String fileName) {
    String dataUrl = canvas.toDataURL("image/png");

    HTMLAnchorElement downloadLink = (HTMLAnchorElement) document.createElement("a");
    downloadLink.href = dataUrl;
    downloadLink.setAttribute("download", fileName + ".png");
    downloadLink.click();
  }

  //--------------------------------------------------------------------------------
  //reconstructing of the whole visualization svg
  //not 100% future-proof for any visualization; so to check if new ones are added
  //--------------------------------------------------------------------------------
  public Consumer<Void> downloadCellSvg() {
    return $ -> {
      Log.warn("Svg function called");
      String targetId = "cell-container-" + this.getCell().getGemId();
      HTMLElement element = (HTMLElement) document.getElementById(targetId);
      HTMLCanvasElement internalCanvas = (HTMLCanvasElement) element.querySelector("canvas");
      HTMLElement legend = (HTMLElement) element.querySelector(".gem-legend");

      if(internalCanvas == null) {
        this.setErrorMessage("The chosen cell's visualization is not supported currently for SVG downloading. Maybe try the download feature within the visualization.");
        this.setError(true);
        return;
      }

      int width = (int) internalCanvas.width;
      int height = (int) internalCanvas.height;

      String imageData = internalCanvas.toDataURL("image/png");
      StringBuilder svg = new StringBuilder();

      svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
          .append("width=\"").append(width).append("\" ")
          .append("height=\"").append(height + 80).append("\">");

      svg.append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>");

      svg.append("<image href=\"")
          .append(imageData)
          .append("\" x=\"0\" y=\"0\" width=\"")
          .append(width)
          .append("\" height=\"")
          .append(height)
          .append("\"/>");

      if (legend != null) {

        NodeList entries = legend.querySelectorAll(".gem-entries");

        int spacing = 120;
        int startX = 20;
        int y = height + 35;

        for (int i = 0; i < entries.length; i++) {

          HTMLElement entry = (HTMLElement) entries.item(i);
          HTMLElement colorDiv = (HTMLElement) entry.querySelector(".gem-color");
          HTMLElement labelDiv = (HTMLElement) entry.querySelector(".gem-label");
          String color = extractColor(colorDiv.getAttribute("style"));
          String label = labelDiv.textContent;

          int x = startX + i * spacing;

          svg.append("<circle cx=\"")
              .append(x)
              .append("\" cy=\"")
              .append(y)
              .append("\" r=\"6\" fill=\"")
              .append(color)
              .append("\"/>");

          svg.append("<text x=\"")
              .append(x + 15)
              .append("\" y=\"")
              .append(y + 5)
              .append("\" font-size=\"14\" ")
              .append("font-family=\"Arial\">")
              .append(label)
              .append("</text>");
        }
      }

      svg.append("</svg>");
      triggerSvgDownload(svg.toString(), this.getCell().getName());
    };
  }

  public void triggerSvgDownload(String svgContent, String filename) {
    String url = "data:image/svg+xml;charset=utf-8," + svgContent;
    HTMLAnchorElement link = (HTMLAnchorElement) document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", filename + ".svg");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  public Consumer<Void> closeDialog() {
    return $ -> {
      super.getClose().accept($);
    };
  }

}
