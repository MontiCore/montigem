package umlp_intellij_plugin;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.codeInsight.codeVision.CodeVisionHost;
import com.intellij.codeInsight.codeVision.CodeVisionInitializer;
import com.intellij.codeInsight.codeVision.EditorCodeVisionContextKt;
import com.intellij.codeInsight.codeVision.ui.model.TextCodeVisionEntry;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicIntelliJFeatureTest extends AbstractIntelliJPluginTest {

  @Test
  public void testUMLPHighlightingWork() {
    openFile("models/Mydomain.umlp");
    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty(), "Highlights can not be loaded");
  }

  @Test
  public void testGUIHighlightingWork() {
    openFile("frontend/src/main/gui/gui/Start.gui");
    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty(), "Highlights can not be loaded");
  }

  @Test
  public void testUMLPSymtabErrorWorks() throws IOException {
    String umlpModelPath = "models/Mydomain.umlp";
    copyExternalFileToProject("src/test/resources/symtab_errors/Mydomain.umlp", umlpModelPath);
    openFile(umlpModelPath);

    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty());
    List<HighlightInfo> errors = highlights.stream().filter(it -> it.type.equals(HighlightInfoType.ERROR)).toList();
    assertFalse(errors.isEmpty(), "No symtab errors found");
  }

  @Test
  public void testGUISymtabErrorWorks() throws IOException {
    String guiModelPath = "frontend/src/main/gui/gui/Start.gui";
    copyExternalFileToProject("src/test/resources/symtab_errors/Start.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty());
    List<HighlightInfo> errors = highlights.stream().filter(it -> it.type.equals(HighlightInfoType.ERROR)).toList();
    assertFalse(errors.isEmpty(), "No symtab errors found");
  }

  @Test
  public void testUMLPCoCoErrorWorks() throws IOException {
    String umlpModelPath = "models/Mydomain.umlp";
    copyExternalFileToProject("src/test/resources/coco_errors/Mydomain.umlp", umlpModelPath);
    openFile(umlpModelPath);

    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty());
    List<HighlightInfo> errors = highlights.stream().filter(it -> it.type.equals(HighlightInfoType.ERROR)).toList();
    assertFalse(errors.isEmpty(), "No coco errors found");
  }

  @Test
  public void testGUICoCoErrorWorks() throws IOException {
    String guiModelPath = "frontend/src/main/gui/gui/Start.gui";
    copyExternalFileToProject("src/test/resources/coco_errors/Start.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty());
    List<HighlightInfo> errors = highlights.stream().filter(it -> it.type.equals(HighlightInfoType.ERROR)).toList();
    assertFalse(errors.isEmpty(), "No coco errors found");
  }

  @Test
  public void testUMLPCompletion() throws IOException {
    String umlpModelPath = "models/Mydomain.umlp";
    copyExternalFileToProject("src/test/resources/completion/Mydomain.umlp", umlpModelPath);
    openFile(umlpModelPath);

    waitForLspIndexing();

    LookupElement[] result = getCompletion();
    System.out.println(Arrays.toString(result));
    assertNotNull(result);
    assertNotEquals(0, result.length);
  }

  @Test
  public void testGUICompletion() throws IOException {
    String baseBath = "frontend/src/main/gui/gui/";
    String guiModelPath = baseBath + "Start.gui";
    copyExternalFileToProject("src/test/resources/completion/Another.gui", baseBath + "Another.gui");
    copyExternalFileToProject("src/test/resources/completion/Start.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    LookupElement[] result = getCompletion();
    System.out.println(Arrays.toString(result));
    assertNotNull(result);
    assertNotEquals(0, result.length);

    assertTrue(Arrays.stream(result).anyMatch(it -> it.getLookupString().equals("@Another")));
    assertTrue(Arrays.stream(result).anyMatch(it -> it.getLookupString().equals("@GemText")));

    assertFalse(Arrays.stream(result).anyMatch(it -> it.getLookupString().equals("@Start")));
  }

  @Test
  public void testGUICompletionInParameters() throws IOException {
    String baseBath = "frontend/src/main/gui/gui/";
    String guiModelPath = baseBath + "Start.gui";
    copyExternalFileToProject("src/test/resources/completion/CompletionInParameters.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    LookupElement[] result = getCompletion();
    System.out.println(Arrays.toString(result));
    assertNotNull(result);
    assertNotEquals(0, result.length);

    assertTrue(Arrays.stream(result).anyMatch(it -> it.getLookupString().equals("SomeClass")));
  }

  @Test
  public void testUMLPNavToDefinition() throws IOException {
    String umlpModelPath = "models/Mydomain.umlp";
    copyExternalFileToProject("src/test/resources/navigation/Mydomain.umlp", umlpModelPath);
    openFile(umlpModelPath);

    waitForLspIndexing();

    PsiElement[] result = getDeclarations();

    assertNotNull(result);
    assertEquals(1, result.length);

    PsiElement elementAtCaret = getElementReferencedAtCaret();
    PsiElement definition = result[0];
    assertEquals("SomeClass", ReadAction.compute(definition::getText));
    assertNotEquals(elementAtCaret, definition);
  }

  @Test
  public void testGUINavToDefinition() throws IOException {
    String guiModelPath = "frontend/src/main/gui/gui/Another.gui";
    copyExternalFileToProject("src/test/resources/navigation/Another.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    PsiElement[] result = getDeclarations();
    assertNotNull(result);
    assertEquals(1, result.length);

    PsiElement elementAtCaret = getElementReferencedAtCaret();
    PsiElement definition = result[0];
    assertEquals("Start", ReadAction.compute(definition::getText));
    assertNotEquals(elementAtCaret, definition);
  }

  @Test
  public void testGUIToUmlpNavToDefinition() throws IOException {
    String guiModelPath = "frontend/src/main/gui/gui/Start.gui";
    copyExternalFileToProject("src/test/resources/navigation/Start.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    PsiElement[] result = getDeclarations();
    assertNotNull(result);
    assertEquals(1, result.length);

    PsiElement elementAtCaret = getElementReferencedAtCaret();
    PsiElement definition = result[0];
    assertTrue(definition.getContainingFile().getName().endsWith("Mydomain.umlp"));
    assertEquals("SomeClass", ReadAction.compute(definition::getText));
    assertNotEquals(elementAtCaret, definition);
  }

  @Test
  public void testUMLPNavToDefinitionExtends() throws IOException {
    String umlpModelPath = "models/Mydomain.umlp";
    copyExternalFileToProject("src/test/resources/navigation/ExtendsNavigation.umlp", umlpModelPath);
    openFile(umlpModelPath);

    waitForLspIndexing();

    PsiElement[] result = getDeclarations();

    assertNotNull(result);
    assertEquals(1, result.length);

    PsiElement elementAtCaret = getElementReferencedAtCaret();
    PsiElement definition = result[0];
    assertEquals("B", ReadAction.compute(definition::getText));
    assertNotEquals(elementAtCaret, definition);
  }

  @Test
  public void testGUIIterateTypecheckIsCorrect() throws IOException {
    String guiModelPath = "frontend/src/main/gui/gui/Start.gui";
    copyExternalFileToProject("src/test/resources/typecheck/IterateStart.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty());
    List<HighlightInfo> errors = highlights.stream().filter(it -> it.type.equals(HighlightInfoType.ERROR)).toList();
    assertTrue(errors.isEmpty(), "Error(s) found for correct model:\n" + errors.stream().map(Objects::toString).collect(Collectors.joining("\n")));
  }

  @Test
  public void testGUIEventTypecheckIsCorrect() throws IOException {
    String guiModelPath = "frontend/src/main/gui/gui/Start.gui";
    copyExternalFileToProject("src/test/resources/typecheck/EventStart.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty());
    List<HighlightInfo> errors = highlights.stream().filter(it -> it.type.equals(HighlightInfoType.ERROR)).toList();
    assertTrue(errors.isEmpty(), "Error(s) found for correct model:\n" + errors.stream().map(Objects::toString).collect(Collectors.joining("\n")));
  }

  @Test
  public void testGUIGuardTypecheckIsCorrect() throws IOException {
    String guiModelPath = "frontend/src/main/gui/gui/Start.gui";
    copyExternalFileToProject("src/test/resources/typecheck/GuardStart.gui", guiModelPath);
    openFile(guiModelPath);

    waitForLspIndexing();

    List<HighlightInfo> highlights = getHighlights();
    assertFalse(highlights.isEmpty());
    List<HighlightInfo> errors = highlights.stream().filter(it -> it.type.equals(HighlightInfoType.ERROR)).toList();
    assertTrue(errors.isEmpty(), "Error(s) found for correct model:\n" + errors.stream().map(Objects::toString).collect(Collectors.joining("\n")));
  }

  @Test
  public void testGUICreateFileCodeLens() throws Exception {
    openFile("frontend/src/main/gui/gui/Start.gui");
    waitForLspIndexing();

    String newGuiRelativePath = "frontend/src/main/gui/gui/NewComponent.gui";

    PsiFile newFile = EdtTestUtil.runInEdtAndGet(() -> {
      return codeInsightFixture.addFileToProject(newGuiRelativePath, "");
    });

    openFile(newGuiRelativePath);

    EdtTestUtil.runInEdtAndWait(() -> {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
      PsiDocumentManager.getInstance(codeInsightFixture.getProject()).commitAllDocuments();
    });

    String emptyContent = getDocumentText(newFile);
    assertTrue(emptyContent.isEmpty(), "A newly created GUI file must stay empty before the code lens is clicked");

    CodeVisionLens codeLens = getOnlyCodeVisionLens("Add basic GUI content");
    clickCodeVisionLens(codeLens);

    EdtTestUtil.runInEdtAndWait(() -> {
      PlatformTestUtil.waitWithEventsDispatching("Content not filled", () -> {
        return getDocumentText(newFile).contains("package gui;");
      }, 30);
    });

    String content = getDocumentText(newFile);
    assertTrue(content.contains("package gui;"), "Package statement missing. Content: " + content);
    assertTrue(content.contains("page NewComponent"), "Component name missing. Content: " + content);

    Thread.sleep(1000L);
    assertTrue(getCodeVisionLenses("Add basic GUI content").isEmpty(),
        "The GUI initialization code lens must disappear after it was executed");
  }

  @Test
  public void testUMLPCreateFileCodeLens() throws Exception {
    openFile("models/Mydomain.umlp");
    waitForLspIndexing();

    String newUmlpRelativePath = "models/newpackage/NewModel.umlp";

    PsiFile newFile = EdtTestUtil.runInEdtAndGet(() -> {
      return codeInsightFixture.addFileToProject(newUmlpRelativePath, "");
    });

    openFile(newUmlpRelativePath);

    EdtTestUtil.runInEdtAndWait(() -> {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
      PsiDocumentManager.getInstance(codeInsightFixture.getProject()).commitAllDocuments();
    });

    String emptyContent = getDocumentText(newFile);
    assertTrue(emptyContent.isEmpty(), "A newly created UML/P file must stay empty before the code lens is clicked");

    CodeVisionLens codeLens = getOnlyCodeVisionLens("Add basic classdiagram");
    clickCodeVisionLens(codeLens);

    EdtTestUtil.runInEdtAndWait(() -> {
      PlatformTestUtil.waitWithEventsDispatching("Content not filled", () -> {
        return getDocumentText(newFile).contains("package newpackage;");
      }, 30);
    });

    String content = getDocumentText(newFile);
    assertTrue(content.contains("package newpackage;"), "Package statement missing. Content: " + content);
    assertTrue(content.contains("classdiagram NewModel {"), "Umlp model name missing. Content: " + content);

    Thread.sleep(1000L);
    assertTrue(getCodeVisionLenses("Add basic classdiagram").isEmpty(),
        "The UML/P initialization code lens must disappear after it was executed");
  }

  protected CodeVisionLens getOnlyCodeVisionLens(String title) {
    List<CodeVisionLens> codeLenses = getCodeVisionLenses(title);
    assertEquals(1, codeLenses.size(), "Expected exactly one initialization code lens");
    return codeLenses.getFirst();
  }

  protected List<CodeVisionLens> getCodeVisionLenses(String title) {
    List<CodeVisionLens> result = new ArrayList<>();
    EdtTestUtil.runInEdtAndWait(() -> {
      CodeVisionHost host = CodeVisionInitializer.Companion
          .getInstance(codeInsightFixture.getProject())
          .getCodeVisionHost();
      host.calculateCodeVisionSync(
          codeInsightFixture.getEditor(),
          codeInsightFixture.getProjectDisposable()
      );

      var rangeMarkers = EditorCodeVisionContextKt
          .getLensContext(codeInsightFixture.getEditor())
          .getValidResult()
          .iterator();
      while (rangeMarkers.hasNext()) {
        RangeMarker rangeMarker = rangeMarkers.next();
        CodeVisionEntry entry = EditorCodeVisionContextKt.getCodeVisionEntryOrThrow(rangeMarker);
        if (entry instanceof TextCodeVisionEntry textEntry && title.equals(textEntry.getText())) {
          result.add(new CodeVisionLens(
              host,
              new TextRange(rangeMarker.getStartOffset(), rangeMarker.getEndOffset()),
              entry
          ));
        }
      }
    });
    return result;
  }

  protected void clickCodeVisionLens(CodeVisionLens codeLens) {
    EdtTestUtil.runInEdtAndWait(() -> codeLens.host.handleLensClick(
        codeInsightFixture.getEditor(),
        codeLens.range,
        codeLens.entry
    ));
  }

  protected String getDocumentText(PsiFile file) {
    return ReadAction.compute(() -> {
      Document document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
      assertNotNull(document);
      return document.getText();
    });
  }

  protected record CodeVisionLens(CodeVisionHost host, TextRange range, CodeVisionEntry entry) {
  }
}
