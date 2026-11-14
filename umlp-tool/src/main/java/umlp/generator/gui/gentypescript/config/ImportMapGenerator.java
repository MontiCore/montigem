/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.gentypescript.config;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdinterfaceandenum._ast.ASTCDEnum;
import de.monticore.generating.GeneratorEngine;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.io.paths.MCPath;
import umlp.generator.gui.GUIGeneratorUtil;

import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ImportMapGenerator {

  protected static final GUIGeneratorUtil util = GUIGeneratorUtil.getInstance();
  protected final GeneratorEngine generatorEngine;
  protected GeneratorSetup setup;
  protected MCPath hwcPath;

  public ImportMapGenerator(GeneratorSetup generatorSetup) {
    generatorSetup.setTracing(false);
    this.generatorEngine = new GeneratorEngine(generatorSetup);
    this.setup = generatorSetup;
    this.hwcPath = generatorSetup.getHandcodedPath();
  }

  public void generate(String tslib, ASTCDCompilationUnit cd) {
    List<String> qNames = new ArrayList<>();
    String pkg = "";
    if (cd.isPresentMCPackageDeclaration()) {
      pkg = cd.getMCPackageDeclaration().getMCQualifiedName().getQName() + ".";
    }
    pkg += cd.getCDDefinition().getName();
    qNames.add(pkg);
    for (ASTCDType ast : cd.getCDDefinition().getCDEnumsList()) {
      qNames.add(pkg + "." + ast.getName());
    }
    for (ASTCDType ast : cd.getCDDefinition().getCDClassesList()) {
      qNames.add(pkg + "." + ast.getName());
    }
    for (ASTCDType ast : cd.getCDDefinition().getCDInterfacesList()) {
      qNames.add(pkg + "." + ast.getName());
    }
    // one file per class diagram: a library usually has several class diagrams, and a shared
    // file would only keep the types of the class diagram generated last
    String out = "cd_" + toFileName(tslib) + "_" + toFileName(pkg) + "_import.map";
    generatorEngine.generateNoA("umlp.GUI2ImportMap", Paths.get(out), tslib, qNames);
    Reporting.reportFileCreation(
        Paths.get(setup.getOutputDirectory().getPath(), out).toString());
  }

  public void generate(String tslib, Collection<ASTGUICompilationUnit> asts) {
    List<String> qNames = new ArrayList<>();
    for (ASTGUICompilationUnit ast : asts) {
      qNames.add(ast.getComponentQName());
    }
    String out = "gui_" + toFileName(tslib) + "_import.map";
    generatorEngine.generateNoA("umlp.GUI2ImportMap", Paths.get(out), tslib, qNames);
    Reporting.reportFileCreation(
        Paths.get(setup.getOutputDirectory().getPath(), out).toString());
  }

  public static String toFileName(String libraryName) {
    // Replace non-alphanumeric characters with underscores
    String fileName = libraryName.replaceAll("[^a-zA-Z0-9\\-]", "_");

    // Generate a unique hash to ensure uniqueness
    String hash = generateHash(fileName);

    // Return the sanitized file name with the unique hash appended
    return fileName + "_" + hash;
  }

  private static String generateHash(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashBytes = md.digest(input.getBytes());
      StringBuilder sb = new StringBuilder();
      for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }
      // Return the first 8 characters of the hash for uniqueness
      return sb.substring(0, 8);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("0x00029 Hashing algorithm not found", e);
    }
  }

}
