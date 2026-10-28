/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.gui;

// TODO AGe: move to frontend-rte

public enum TSImportMap {
  // vis libs
  CAD("mc.fenix.cad", "@umlp/cad"),
  ADVANCED("mc.fenix.advanced", "@umlp/advanced"),
  ARRANGE("mc.fenix.arrange", "@umlp/arrange"),
  BASIC("mc.fenix.basic", "@umlp/basic"),
  CHARTS("mc.fenix.charts", "@umlp/charts"),
  COMMON("mc.fenix.common", "@umlp/commonj2ts"),
  NAVIGATION("mc.fenix.navigation", "@umlp/navigation"),
  INPUT("mc.fenix.input", "@umlp/input"),
  TABLE("mc.fenix.table", "@umlp/table"),
  FISCHERTECHNIK("mc.fenix.fischertechnik", "@umlp/fischertechnik"),
  FUNCTIONDISPLAY("mc.fenix.functiondisplay", "@umlp/functiondisplay"),
  NETWORKGRAPH("mc.fenix.networkgraph", "@umlp/networkgraph"),
  EDITOR("mc.fenix.editor", "@umlp/editor"),
  LOGIN("loginmodule", "@montigem/login"),
  COMMENT("commentmodule", "@montigem/comment"),
  DASHBOARD("dashboardmodule", "@montigem/dashboard")
  ;

  private final String tsPackage;
  private final String javaPackage;

  TSImportMap(String javaPackage, String tsPackage) {
    this.javaPackage = javaPackage;
    this.tsPackage = tsPackage;
  }

  public String getJavaPackage() {
    return javaPackage;
  }

  public String getTSPackage() {
    return tsPackage;
  }

}
