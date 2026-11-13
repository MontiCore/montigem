package de.montigem.gradle;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public class FrontendPluginExtension {
  private final Property<Boolean> withCD2GUI;
  private final Property<Boolean> publishDomainModels;

  @Inject
  public FrontendPluginExtension(ObjectFactory objects) {
    this.withCD2GUI = objects.property(Boolean.class);
    this.withCD2GUI.convention(false);
    this.publishDomainModels = objects.property(Boolean.class);
    // the assets are public, the domain model is not published unless requested
    this.publishDomainModels.convention(false);
  }

  public Property<Boolean> getWithCD2GUI() {
    return withCD2GUI;
  }

  /**
   * Copy the domain model into the assets of the app (assets/models)
   */
  public Property<Boolean> getPublishDomainModels() {
    return publishDomainModels;
  }
}
