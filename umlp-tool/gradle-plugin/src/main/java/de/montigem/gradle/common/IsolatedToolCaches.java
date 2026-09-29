/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import java.beans.Introspector;

/**
 * FreeMarker introspects the generator classes through java.beans, whose cache keeps them softly.
 * That keeps the closed tool class loaders, with all their static data, alive until memory gets low.
 */
public final class IsolatedToolCaches {

  private IsolatedToolCaches() {
  }

  public static void release() {
    Introspector.flushCaches();
  }
}
