package umlp.generator.gui.gentypescript.java2ts;

import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._visitor.GUIDSLTraverser;
import de.monticore.guidsl._visitor.GUIDSLVisitor2;
import de.monticore.io.paths.MCPath;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.logging.Log;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.gui.gentypescript.decorator.TSImportHelper;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class GUIComponentAnnotationDecoratorAndStandaloneConfig implements GUIDSLVisitor2 {

  protected final GUIDSLTraverser traverser;
  protected static final GUIGeneratorUtil util = GUIGeneratorUtil.getInstance();

  protected final GlobalExtensionManagement glex;
  protected final MCPath hwPath;
  protected final Path outPath;
  protected final MCPath importsPath;

  protected ASTGUICompilationUnit gui;
  protected TSImportHelper importHelper;

  protected Set<String> imports = new LinkedHashSet<>();

  public GUIComponentAnnotationDecoratorAndStandaloneConfig(
      final GlobalExtensionManagement glex, MCPath hwPath, Path outPath, MCPath importsPath) {
    traverser = GUIDSLMill.traverser();
    traverser.add4GUIDSL(this);
    this.glex = glex;
    this.hwPath = hwPath;
    this.outPath = outPath;
    this.importsPath = importsPath;
  }

  public void transform(ASTCDCompilationUnit ast, ASTGUICompilationUnit gui) {
    String qName = ast.getMCPackageDeclaration().getMCQualifiedName().getQName();
    this.gui = gui;
    for (ASTCDClass clazz : ast.getCDDefinition().getCDClassesList()) {
      importHelper = new TSImportHelper(glex, clazz, hwPath, outPath, importsPath);
      String originalName = GUIGeneratorUtil.getInstance().getOriginalName(clazz);
      String qClassName = qName + "." + originalName;
      if (!GUIGeneratorUtil.getInstance().isComponent(clazz)) {
        glex.replaceTemplate(
            GUI2ComponentTemplates.ANNOTATIONS,
            clazz,
            new TemplateHookPoint(GUI2ComponentTemplates.ANNOTATION_DIRECTIVE)
        );
      }
      else {
        boolean noHWC = !GUIGeneratorUtil.getInstance().existsHandwrittenClass(hwPath, qClassName);
        String htmlPath = "./" + originalName + ".html";
        List<String> scssPaths = new ArrayList<>();

        if (noHWC) {
          importHelper.addImportString("import { Component } from '@angular/core';");
          Optional<String> hwHtmlPath = util.getRelativeImportPath(
              hwPath, outPath, qClassName, "html");
          if (hwHtmlPath.isPresent()) {
            htmlPath = hwHtmlPath.get();
          }
          Optional<String> hwScssPath = util.getRelativeImportPath(
              hwPath, outPath, qClassName, "scss");
          if (hwScssPath.isPresent()) {
            scssPaths.add(hwScssPath.get());
          }
        }
        else {
          importHelper.addImportString("import { Directive } from '@angular/core';");
          // TODO #3768
          Optional<String> hwHtmlPath = util.getRelativeImportPath(
              hwPath, outPath, qClassName, "html");
          if (hwHtmlPath.isPresent()) {
            if (existsTSHandwrittenFile(hwPath, gui.getComponentQName())) {
              htmlPath = "./" + originalName + ".html";
            }
            else {
              htmlPath = hwHtmlPath.get();
            }
          }
          else {
            Optional<Path> hwTS = getTSHandwrittenFile(hwPath, gui.getComponentQName());
            if (hwTS.isPresent()) {
              htmlPath = util.predictRelativeImportPath(
                  outPath, hwTS.get(), Names.getPathFromPackage(qClassName) + ".html"
              );
            }
            else {
              htmlPath = "./" + originalName + ".html";
            }
          }
          Optional<String> hwScssPath = util.getRelativeImportPath(
              hwPath, outPath, qClassName, "scss");
          if (hwScssPath.isPresent()) {
            if (existsTSHandwrittenFile(hwPath, gui.getComponentQName())) {
              scssPaths.add("./" + originalName + ".scss");
            }
            else {
              scssPaths.add(hwScssPath.get());
            }
          }
        }
        String selector = GUIGeneratorUtil.getInstance().pascal2KebabCase(originalName);
        imports = new LinkedHashSet<>();
        gui.accept(traverser);
        glex.replaceTemplate(
            GUI2ComponentTemplates.ANNOTATIONS,
            clazz,
            new TemplateHookPoint(GUI2ComponentTemplates.ANNOTATION_STANDALONECONFIG_COMPONENT, htmlPath, scssPaths, selector, imports, hwPath, noHWC)
        );
      }
    }
  }

  public boolean existsTSHandwrittenFile(MCPath targetPath, String qName) {
    return getTSHandwrittenFile(targetPath, qName).isPresent();
  }

  public Optional<Path> getTSHandwrittenFile(MCPath targetPath, String qName) {
    String hwFile = Names.getPathFromPackage(qName) + ".ts";
    Optional<URL> hwFilePath = targetPath.find(hwFile);
    if (hwFilePath.isPresent()) {
      return MCPath.toPath(hwFilePath.get());
    }
    return Optional.empty();
  }

  @Override
  public void visit(ASTGUIComponent component) {
    String componentQName = gui.getComponentQName();
    if (!component.isPresentNameSymbol()) {
      Log.error("0x0008B Name symbol of " + component.getName() + " not present.");
    }

    // Avoid circular import dependencies for recursive component calls
    if (!component.getNameSymbol().getFullName().equals(componentQName)) {
      String importedQName = component.getNameSymbol().getFullName();
      importHelper.addImport(importedQName, Optional.empty(), true, componentQName);
      imports.add(importedQName);
    }
  }

}
