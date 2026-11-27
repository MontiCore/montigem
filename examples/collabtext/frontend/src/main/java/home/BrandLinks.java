package home;

import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

import static def.dom.Globals.window;

@Component(styleUrls = {"...config.styleUrls"})
public class BrandLinks extends BrandLinksTOP {

  @Override
  public Consumer<Void> openMontiGem() {
    return event -> window.location.assign(getMontiGemUrl());
  }

  @Override
  public Consumer<Void> openSoftwareEngineering() {
    return event -> window.location.assign(getSoftwareEngineeringUrl());
  }
}
