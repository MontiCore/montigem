/* (c) https://github.com/MontiCore/monticore */
package common;

import umlp.jsweet.extension.annotation.Component;

/**
  Companion of the landing page: only provides the model snippet shown in the
  "Write Models, Generate UI" section - the GUI DSL source of the small page
  whose rendered result is shown next to it.
*/
@Component
public class LandingPage extends LandingPageTOP {

  @Override
  public void init() {
      super.init();
      setDemoCode(getDemoCodeContent());
  }

  private String getDemoCodeContent() {
      return "package demo;\n" +
             "\n" +
             "import mc.fenix.arrange.GemColumn;\n" +
             "import mc.fenix.basic.GemButton;\n" +
             "import mc.fenix.basic.GemText;\n" +
             "import mc.fenix.input.GemTextInput;\n" +
             "\n" +
             "page MyPage() {\n" +
             "  @GemColumn(\n" +
             "    hAlign = \"center\",\n" +
             "    rowGap = \"15px\",\n" +
             "    components = [\n" +
             "      @GemText(value = \"Hello World\", size = \"1.5rem\"),\n" +
             "      @GemTextInput(labelText = \"Name\"),\n" +
             "      @GemButton(label = \"Send\")\n" +
             "    ]\n" +
             "  );\n" +
             "}";
  }
}
