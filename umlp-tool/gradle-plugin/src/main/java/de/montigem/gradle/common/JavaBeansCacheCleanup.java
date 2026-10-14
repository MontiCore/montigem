/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.beans.Introspector;

/**
 * FreeMarker introspects the generator classes through java.beans, whose JDK-wide cache keeps them softly.
 * That keeps the closed tool class loaders, with all their static data, alive until memory gets low.
 * Gradle closes this service after the last tool task of the build.
 */
public abstract class JavaBeansCacheCleanup implements BuildService<BuildServiceParameters.None>, AutoCloseable {

  public static Provider<? extends BuildService<?>> register(Gradle gradle) {
    return gradle.getSharedServices().registerIfAbsent("umlpJavaBeansCacheCleanup", JavaBeansCacheCleanup.class, spec -> {
    });
  }

  @Override
  public void close() {
    Introspector.flushCaches();
  }
}
