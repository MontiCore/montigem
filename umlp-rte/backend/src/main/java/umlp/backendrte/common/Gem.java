// (c) https://github.com/MontiCore/monticore
package umlp.backendrte.common;

// RTE

import de.se_rwth.commons.logging.Log;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

//alpha, this class is not fully developed,
//only to try it out and might change

// kann man dies machen?: Gem<E> extends E
// weil dies m.W. nicht der Fall ist, dass ist es wharscheinlich doch notwendig
// bei den aufrufenden Stellen Gem<E> Funktionalität reingeneriert werden
// -> this might be realized using the realthis pattern?
//    however, this assumes that E is written accordingly...

public class Gem<E extends GemIdentifiable> implements GemIdentifiable {

  // todo: Beschreiben, in welchen Modi die Attribute wie besetzt (oder irrelevant sind)

  /**
   * The object stored
   * is not null iff the object has been loaded ({@link Gem#loadingState} == {@link LoadingState#LOADED})
   */
  protected E object = null;
  // Wird gebraucht, um das Gem zu (de-)serialisieren
  /**
   * Contains the gemId of the object. If the object is loaded, gemId == object.getGemId() must hold.
   */
  protected long gemId;

  /**
   * whether the object has been loaded or (why) not
   * the object can be {@link LoadingState#LOADED} or {@link LoadingState#NOT_YET_LOADED}
   * alternatively the state indicates that the attempt to load
   * failed for some reason (s. {@link LoadingState})
   */
  protected LoadingState loadingState;

  // todo: warum doppelt geschachtelt? Supplier<LoadResult ?
  // -> LoadResult may return the loaded object
  //    for type safety the value is typed
  protected Supplier<LoadResult<E>> loadFunction = null;

  /*
   * Constructor for: Object exists and is loaded
   */
  public Gem(E object) {
    this.object = object;
    this.gemId = object.getGemId();
    this.loadingState = LoadingState.LOADED;
  }

  /*
   * Constructor for: Object is not yet loaded, but we know the loading supplier
   */
  public Gem(long gemId, Supplier<LoadResult<E>> supplier) {
    this.gemId = gemId;
    this.loadFunction = Objects.requireNonNull(supplier);
    this.loadingState = LoadingState.NOT_YET_LOADED;
  }

  /*
   * Constructor for: Object is not present.
   */
  public Gem(){
    this.gemId = -1;
    this.loadFunction = () -> { throw new IllegalStateException("Can not load empty Gem!"); };
    this.loadingState = LoadingState.EMPTY;
  }

  /*
   * Constructor for: Object is not yet loaded
   */
  public Gem(long gemID, LoadingState loadingState) {
    this.gemId = gemID;
    if (loadingState == LoadingState.LOADED
        // || loadingState == LoadingState.NOT_YET_LOADED // TODO AHe: check removed for deser, check if still needed
    ) {
      Log.error("0xE2921 Object created with unexpected state");
    }
    this.loadingState = loadingState;
  }

  /**********************************************************/
  /* The following functions resemble
     an Optional with their 6 functions, aber können nicht von Optiona<E> erben
     weil Java das nicht hergibt.
    */
  public LoadingState getLoadingState() {
    return loadingState;
  }

  public boolean isPresent() {
    loadIfRequired();
    return loadingState == LoadingState.LOADED && object != null;
  }

  public E get() {
    loadIfRequired();
    if (!isPresent()) {
      Log.error("0xE2921 Object was not loaded: " + loadingState.toString());
      // tod: wir müssen allgemein mal über den Umgang mit fatalen Fehlern reden
      // und was wir daraus machen. Ein Websystem ist ja was anderes als ein Batch-Generator
    }
    return object;
  }

  /**
   * Loads the object if required and returns it as an Optional.
   * Returns an empty Optional if the object is absent or could not be loaded.
   */
  public Optional<E> asOpt() {
    return isPresent() ? Optional.of(object) : Optional.empty();
  }

  public void set(E object) {
    if (object != null) {
      this.object = object;
      this.gemId = object.getGemId();
      this.loadingState = LoadingState.LOADED;
    }
  }

  /*
   * tries to load a yet unloaded object.
   * Returns only when the load was completed.
   *  (If the load is unsuccesful a lower mechanism checks all the
   *   communicative aspects)
   */
  protected void loadIfRequired() {
    if (loadingState == LoadingState.NOT_YET_LOADED) {
      LoadResult<E> result = loadFunction.get();
      if (result.isPresent()) {
        this.set(result.get());
      }
      else {
        loadingState = result.loadingState;
      }
    }
  }

  @Override
  public long getGemId() {
    return gemId;
  }
}
