// (c) https://github.com/MontiCore/monticore
package umlp.backendrte.common;

import java.util.Optional;

// Solely used in GemListBuilder
// SumType of a domain-objects GemID and builder
// as the builder may not have an ID that can be used to get the element from the manager
class IDOrBuilder<E extends GemIdentifiable & GemBuildable> {

  Optional<Long> gemIDOpt = Optional.empty();

  Optional<E> builderOpt = Optional.empty();

  public IDOrBuilder(long gemID) {
    gemIDOpt = Optional.of(gemID);
  }

  public IDOrBuilder(E builder) {
    if (builder.isBuildFinished()) {
      gemIDOpt = Optional.of(builder.getGemId());
    }
    else {
      builderOpt = Optional.of(builder);
    }
  }

  public boolean isPresentID() {
    return gemIDOpt.isPresent();
  }

  public boolean isPresentBuilder() {
    return builderOpt.isPresent();
  }

  public long getID() {
    return gemIDOpt.get();
  }

  public E getBuilder() {
    return builderOpt.get();
  }

  /**
   * @return the gemId of the element. Unlike {@link #getIDAndMaybeBuild()} this never builds;
   *         builders carry their gemId from construction, they are just not known to the manager yet
   */
  public long getGemId() {
    return isPresentID() ? gemIDOpt.get() : builderOpt.get().getGemId();
  }

  /**
   * builds the element if necessary and returns its id
   * @return the id of the element, it can be used to retrieve the object from the manager
   */
  public long getIDAndMaybeBuild() {
    if (isPresentID()) {
      return gemIDOpt.get();
    }
    else {
      E builder = builderOpt.get();
      if (!builder.isBuildFinished()) {
        builder.build();
      }
      return builder.getGemId();
    }
  }

}
