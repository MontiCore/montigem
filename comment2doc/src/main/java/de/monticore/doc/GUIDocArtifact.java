package de.monticore.doc;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.se_rwth.commons.Names;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GUIDocArtifact {
  protected ASTGUICompilationUnit componentAST;
  protected Path outputDir;
  protected Path outputFile;
  protected String componentModel;
  protected String componentLibraryName;
  protected Map<String, String> modelExamples = new HashMap<>();
  protected Map<String, Path> imageExamples = new HashMap<>();
  protected Map<String, String> codeExamples = new HashMap<>();
  protected Map<String, String> cdTypesCode = new HashMap<>();
  protected Map<String, ASTCDCompilationUnit> cdTypes = new HashMap<>();

  protected List<String> relatedComponents = new ArrayList<>();
  protected List<String> cdTypesDisplay = new ArrayList<>();

  public GUIDocArtifact(Path outDir) {
    outputDir = outDir;
  }

  public ASTGUICompilationUnit getComponentAST() {
    return componentAST;
  }

  public void setComponentAST(ASTGUICompilationUnit componentAST) {
    this.componentAST = componentAST;
    setOutputFile();
  }

  public String getComponentModel() {
    Pattern pattern = Pattern.compile("/\\*.*?\\*/\\s*\n?", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(componentModel);

    // Replace all matches with an empty string
    String noComment = matcher.replaceAll("");

    Pattern pattern1 = Pattern.compile("(component\\s+\\w+\\(.*?\\))", Pattern.DOTALL);
    Matcher matcher1 = pattern1.matcher(noComment);

    if (matcher1.find()) {
      return matcher1.group(1) + "\n";
    }
    return noComment;
  }

  public void setComponentModel(String componentModel) {
    this.componentModel = componentModel;
  }

  public String getComponentLibraryName() {
    return componentLibraryName;
  }

  public void setComponentLibraryName(String componentLibraryName) {
    this.componentLibraryName = componentLibraryName;
  }

  public Map<String, String> getModelExamples() {
    return modelExamples;
  }

  public Map<String, String> getCodeExamples() {
    return codeExamples;
  }

  public Map<String, Path> getImageExamples() {
    return imageExamples;
  }

  public Set<String> getExampleList() {
    return this.modelExamples.keySet();
  }

  public Set<String> getImageList() {
    return this.imageExamples.keySet();
  }

  public boolean hasExampleModel(String example) {
    return this.modelExamples.containsKey(example);
  }

  public String getExampleModel(String example) {
    return this.modelExamples.get(example);
  }

  public boolean hasExampleImage(String example) {
    return this.imageExamples.containsKey(example);
  }

  public Path getExampleImage(String example) {
    return this.imageExamples.get(example);
  }

  public boolean hasExampleCode(String example) {
    return this.codeExamples.containsKey(example);
  }

  public String getExampleCode(String example) {
    return this.codeExamples.get(example);
  }

  public List<String> getRelatedComponents() {
    return this.relatedComponents;
  }

  public List<String> getCdTypesDisplay() {
    return this.cdTypesDisplay;
  }

  public void addIfModelExample(String modelString, String componentName) {
    if (isExample(componentName)) {
      this.modelExamples.put(componentName, modelString);
    }
  }

  public void addIfImageExample(Path imagePath, String componentName) {
    if (isExample(componentName)) {
      this.imageExamples.put(componentName, imagePath);
    }
  }

  public void addIfCodeExample(String codeString, String componentName) {
    if (isExample(componentName)) {
      this.codeExamples.put(componentName, codeString);
    }
  }

  public void addCD(String typeName, ASTCDCompilationUnit ast, String codeString) {
    this.cdTypes.put(typeName, ast);
    this.cdTypesCode.put(typeName, codeString);
  }

  public void addRelatedComponent(String componentName) {
    this.relatedComponents.add(componentName);
  }

  public void addCdTypesDisplay(String typeName) {
    this.cdTypesDisplay.add(typeName);
  }

  public Set<String> getCDList() {
    return this.cdTypesCode.keySet();
  }

  public boolean hasCDAST(String typeName) {
    return this.cdTypes.containsKey(typeName);
  }

  public ASTCDCompilationUnit getCDAST(String typeName) {
    return this.cdTypes.get(typeName);
  }

  public boolean hasCDModel(String typeName) {
    return this.cdTypesCode.containsKey(typeName);
  }

  public String getCDModel(String typeName) {
    Pattern pattern = Pattern.compile("/\\*.*?\\*/\\s*\n?", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(this.cdTypesCode.get(typeName));

    // Replace all matches with an empty string
    String noComment = matcher.replaceAll("");

    Pattern pattern1 = Pattern.compile("(component\\s+\\w+\\(.*?\\))", Pattern.DOTALL);
    Matcher matcher1 = pattern1.matcher(noComment);

    if (matcher1.find()) {
      return matcher1.group(1) + "\n";
    }
    return noComment;
  }

  public void setOutputFile() {
    this.outputFile = outputDir.resolve(calcRelativeOutputPath());
  }

  public Path calcRelativeOutputPath() {
    return Paths.get(
        Names.getPathFromQualifiedName(getComponentAST().getComponentQName()),
        getComponentAST().getComponentName() + ".md"
    );
  }

  public Path getOutputFile() {
    return outputFile;
  }

  public String getOutputFilePath() {
    return "./" + getOutputFile().toString().replace(File.separatorChar, '/');
  }

  public boolean isExample(String componentName) {
    Pattern pattern = Pattern.compile(
        "^My" + Pattern.quote(getComponentAST().getComponentName()) + "Example.*$"
    );
    return pattern.matcher(componentName).matches();
  }
}
