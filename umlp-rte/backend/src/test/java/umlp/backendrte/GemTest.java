// (c) https://github.com/MontiCore/monticore
package umlp.backendrte;

import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import umlp.backendrte.common.Gem;
import umlp.backendrte.common.LoadingState;
import umlp.backendrte.lazyloadTestUtil.Elem;
import umlp.backendrte.lazyloadTestUtil.ListBasedServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GemTest extends AbstractUMLPRTETest {

  List<Elem> allElems;

  ListBasedServer listServer0;

  Gem<Elem> gemLoadable0;

  Gem<Elem> gemLoadable1;

  Gem<Elem> gemNotAuthorized0;

  Gem<Elem> gemDoesNotExist0;

  @BeforeEach
  public void init() {
    allElems = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      allElems.add(new Elem());
    }

    listServer0 = new ListBasedServer(allElems);
    listServer0.setIdsNotAuthorized(Collections.singleton(getElem(2).getGemId()));

    gemLoadable0 = listServer0.createGem(getElem(0).getGemId());
    gemLoadable1 = listServer0.createGem(getElem(1).getGemId());
    gemNotAuthorized0 = listServer0.createGem(getElem(2).getGemId());
    gemDoesNotExist0 = listServer0.createGem(8934583458973489435L);
  }

  @Test
  public void GemLoadTest() {
    assertSame(LoadingState.NOT_YET_LOADED, gemLoadable0.getLoadingState());
    assertSame(LoadingState.NOT_YET_LOADED, gemLoadable1.getLoadingState());
    assertTrue(gemLoadable0.isPresent());
    assertTrue(gemLoadable1.isPresent());
    assertSame(LoadingState.LOADED, gemLoadable0.getLoadingState());
    assertSame(LoadingState.LOADED, gemLoadable1.getLoadingState());
    assertElemNr(gemLoadable0.get(), 0);
    assertElemNr(gemLoadable1.get(), 1);
    assertFalse(gemLoadable0.get().equals(gemLoadable1.get()));
    assertNoErrors();
  }

  @Test
  public void GemNotAuthorizedTest() {
    assertFalse(gemNotAuthorized0.isPresent());
    assertSame(LoadingState.NOT_AUTHORIZED, gemNotAuthorized0.getLoadingState());
    assertNoErrors();
  }

  @Test
  public void GemNotAuthorizedGetTest() {
    gemNotAuthorized0.get();
    //warn: this assumes Log based error reporting
    assertFalse(Log.getFindings().isEmpty());
  }

  @Test
  public void GemDoesNotExistTest() {
    assertFalse(gemDoesNotExist0.isPresent());
    assertSame(LoadingState.FAILED_TO_LOAD, gemDoesNotExist0.getLoadingState());
    assertNoErrors();
  }

  @Test
  public void GemDoesNotExistGetTest() {
    gemDoesNotExist0.get();
    //warn: this assumes Log based error reporting
    assertFalse(Log.getFindings().isEmpty());
  }

  @Test
  public void asOptReturnsLoadedObject() {
    assertSame(getElem(0), new Gem<>(getElem(0)).asOpt().get());
    assertNoErrors();
  }

  @Test
  public void asOptLoadsObjectIfRequired() {
    assertSame(LoadingState.NOT_YET_LOADED, gemLoadable0.getLoadingState());
    assertSame(getElem(0), gemLoadable0.asOpt().get());
    assertSame(LoadingState.LOADED, gemLoadable0.getLoadingState());
    assertSame(getElem(0), gemLoadable0.asOpt().get());
    assertNoErrors();
  }

  @Test
  public void asOptReturnsEmptyForAbsentOrUnavailableObjects() {
    assertFalse(new Gem<Elem>().asOpt().isPresent());
    assertFalse(gemNotAuthorized0.asOpt().isPresent());
    assertFalse(gemDoesNotExist0.asOpt().isPresent());
    assertSame(LoadingState.NOT_AUTHORIZED, gemNotAuthorized0.getLoadingState());
    assertSame(LoadingState.FAILED_TO_LOAD, gemDoesNotExist0.getLoadingState());
    assertNoErrors();
  }

  protected Elem getElem(int idx) {
    return allElems.get(idx);
  }

  protected void assertElemNr(Elem elem, int idx) {
    assertSame(allElems.get(idx), elem);
  }

}
