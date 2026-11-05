/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;/* (c) https://github.com/MontiCore/monticore */

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._ast.ASTCDBiDir;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.domain.decorator.ManagerDecorator;
import umlp.generator.util.PathUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

public class AssocCommandDecorator extends AbstractDeSerDecorator {

  public static final String COMMAND_ASSOC_PACKAGE = COMMAND_PACKAGE + ".associations";
  public static final String ADD_SUFFIX = "_add";
  public static final String REMOVE_SUFFIX = "_remove";
  public static final String SET_SUFFIX = "_set";


  public AssocCommandDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_ASSOC_PACKAGE);
    origCD.getCDDefinition().getCDAssociationsList().stream()
            .filter(a -> !a.getModifier().isDerived())
            .filter(a -> a.getCDAssocDir() instanceof ASTCDBiDir)
            // a bidirectional qualified association is synchronized by its GemMap's commands, the value side mirrors it
            .filter(a -> !GenService.isMap(a.getLeft()) && !GenService.isMap(a.getRight())).
            forEach(a -> astcdPackage.addAllCDElements(withOrigin(createClass(origCD, a, astcdPackage.getName()))));
  }

  protected Collection<ASTCDClass> createClass(ASTCDCompilationUnit origCD, ASTCDAssociation assoc, String packageName) {
    List<ASTCDClass> retList = Lists.newArrayList();
    ASTCDClass clazz = createAddClass(origCD, assoc, packageName);
    retList.add(createDeSer(origCD, clazz));
    retList.add(clazz);
    clazz = createRemoveClass(origCD, assoc, packageName);
    retList.add(createDeSer(origCD, clazz));
    retList.add(clazz);
    clazz = createSetClass(origCD, assoc, packageName);
    retList.add(createDeSer(origCD, clazz));
    retList.add(clazz);
    return retList;
  }

  protected ASTCDClass createAddClass(ASTCDCompilationUnit origCD, ASTCDAssociation assoc,
                                      String packageName) {
    String classname = StringTransformations.capitalize(assoc.getName()) + ADD_SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setName(classname)
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass()))
            .build();

    String sourceName = assoc.getLeft().getName();
    String targetName = assoc.getRight().getName();
    String sourceType = assoc.getLeft().getSymbol().getType().print();
    String targetType = assoc.getRight().getSymbol().getType().print();

    // attributes and getter
    AccessorDecorator accessor = new AccessorDecorator(glex);
    ASTCDAttribute attr = getCD4C().addAttribute(ast, "protected long " + StringTransformations.uncapitalize(sourceName) + "GemId");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected long " + StringTransformations.uncapitalize(targetName) + "GemId");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected int "+sourceName+"Index");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected int "+targetName+"Index");
    ast.addAllCDMembers(accessor.decorate(attr));

    // constructor
    addConstructor(ast, getConstructorTemplate(), classname, packageName, sourceName, targetName);

    // methods
    getCD4C().addMethod(ast, getAdd_DoRunTemplate(), assoc.getName(),
            sourceName, targetName, sourceType, targetType,
            GenService.isList(assoc.getLeft()), GenService.isList(assoc.getRight()),
            getCorrectManager(assoc), hasOCL);
    getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(),
            Lists.newArrayList(sourceName+"GemId", targetName+"GemId"));
    getCD4C().addMethod(ast, getAssocsCheckAccessPolicyTemplate(), sourceType, targetType, "Create");

    // Importe
    addImports(origCD, ast, sourceType, targetType);

    return ast;
  }

  protected String getAssocsCheckAccessPolicyTemplate() {
    return "umlp.commands.assocs.AssocsCheckAccessPolicy";
  }

  protected String getAdd_DoRunTemplate() {
    return "umlp.commands.assocs.Add_DoRun";
  }

  protected ASTCDClass createRemoveClass(ASTCDCompilationUnit origCD, ASTCDAssociation assoc,
                                         String packageName) {
    String classname = StringTransformations.capitalize(assoc.getSymbol().getName()) + REMOVE_SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setName(classname)
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass()))
            .build();

    String sourceName = assoc.getLeft().getName();
    String targetName = assoc.getRight().getName();
    String sourceType = assoc.getLeft().getSymbol().getType().print();
    String targetType = assoc.getRight().getSymbol().getType().print();

    // attributes and getter
    AccessorDecorator accessor = new AccessorDecorator(glex);
    ASTCDAttribute attr = getCD4C().addAttribute(ast, "protected long " + StringTransformations.uncapitalize(sourceName) + "GemId");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected long " + StringTransformations.uncapitalize(targetName) + "GemId");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected int "+sourceName+"Index");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected int "+targetName+"Index");
    ast.addAllCDMembers(accessor.decorate(attr));

    // constructor
    addConstructor(ast, getConstructorTemplate(), classname, packageName, sourceName, targetName);

    // methods
    getCD4C().addMethod(ast, getRemove_DoRunTemplate(), assoc.getName(),
            sourceName, targetName, sourceType, targetType,
            GenService.isList(assoc.getLeft()), GenService.isList(assoc.getRight()),
            getCorrectManager(assoc), hasOCL);
    getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(),
            Lists.newArrayList(sourceName+"GemId", targetName+"GemId"));
    getCD4C().addMethod(ast, getAssocsCheckAccessPolicyTemplate(), sourceType, targetType, "Delete");

    // Importe
    addImports(origCD, ast, sourceType, targetType);

    return ast;
  }

  protected String getConstructorTemplate() {
    return "umlp.commands.assocs.Constructor";
  }

  protected String getRemove_DoRunTemplate() {
    return "umlp.commands.assocs.Remove_DoRun";
  }

  protected ASTCDClass createSetClass(ASTCDCompilationUnit origCD, ASTCDAssociation assoc,
                                      String packageName) {
    String classname = StringTransformations.capitalize(assoc.getName() + SET_SUFFIX);

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setName(classname)
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass()))
            .build();

    String sourceName = assoc.getLeft().getName();
    String targetName = assoc.getRight().getName();
    String sourceType = assoc.getLeft().getSymbol().getType().print();
    String targetType = assoc.getRight().getSymbol().getType().print();

    // attributes and getter
    AccessorDecorator accessor = new AccessorDecorator(glex);
    ASTCDAttribute attr = getCD4C().addAttribute(ast, "protected long old" + StringTransformations.capitalize(sourceName) + "GemId");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected long old" + StringTransformations.capitalize(targetName) + "GemId");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected long " + sourceName + "GemId");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected long " + targetName + "GemId");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected int " + sourceName +"Index");
    ast.addAllCDMembers(accessor.decorate(attr));
    attr = getCD4C().addAttribute(ast, "protected int " + targetName+"Index");
    ast.addAllCDMembers(accessor.decorate(attr));

    addConstructor(ast, getSet_ConstructorTemplate(), classname, packageName, sourceName, targetName);

    getCD4C().addMethod(ast, getSet_DoRunTemplate(),
            sourceName, targetName);
    getCD4C().addMethod(ast, getSet_Source_ExchangeTemplate(), assoc.getName(),
            targetName, sourceName, targetType, sourceType,
            GenService.isList(assoc.getRight()), GenService.isList(assoc.getLeft()),
            getCorrectManager(assoc), hasOCL);
    getCD4C().addMethod(ast, getSet_Source_ExchangeTemplate(), assoc.getName(),
            sourceName, targetName, sourceType, targetType,
            GenService.isList(assoc.getLeft()), GenService.isList(assoc.getRight()),
            getCorrectManager(assoc), hasOCL);
    getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(),
            Lists.newArrayList(sourceName+"GemId", targetName+"GemId"));
    getCD4C().addMethod(ast, getAssocsCheckAccessPolicyTemplate(), sourceType, targetType, "Update");

    addImports(origCD, ast, sourceType, targetType);

    return ast;
  }

  protected void addConstructor(
      ASTCDClass ast, String template, String classname, String packageName, String sourceName, String targetName
  ) {
    getCD4C().addConstructor(ast, template, classname, packageName,
        sourceName, targetName);
  }

  protected String extendedClass() {
    return "Command<Void>";
  }

  protected String getSet_ConstructorTemplate() {
    return "umlp.commands.assocs.Set_Constructor";
  }

  protected String getGetEffectedGemIdsTemplate() {
    return "umlp.commands.GetEffectedGemIds";
  }

  protected String getSet_Source_ExchangeTemplate() {
    return "umlp.commands.assocs.Set_Source_Exchange";
  }

  protected String getSet_DoRunTemplate() {
    return "umlp.commands.assocs.Set_DoRun";
  }

  protected ASTCDClass createDeSer(ASTCDCompilationUnit origCD, ASTCDClass cmdClass) {
    String cmdName = cmdClass.getName();

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
            .setName(cmdName+DE_SER)
            .build();

    getCD4C().addMethod(ast, getFromJsonObjectTemplate(), cmdName,
            cmdClass.getCDAttributeList().stream().map(a -> a.getName()).collect(Collectors.toList()));
    getCD4C().addMethod(ast, getBuildJsonTemplate(), cmdName,
            cmdClass.getCDAttributeList().stream().map(a -> a.getName()).collect(Collectors.toList()));

    addDeSerImports(origCD, ast, cmdName);

    addedDeSers.add(ast);
    return ast;
  }

  protected void addDeSerImports(ASTCDCompilationUnit origCD, ASTCDClass ast, String cmdName) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
  }

  protected String getBuildJsonTemplate() {
    return "umlp.commands.assocs.BuildJson";
  }

  protected String getFromJsonObjectTemplate() {
    return "umlp.commands.assocs.FromJsonObject";
  }

  protected void addImports(ASTCDCompilationUnit origCD, ASTCDClass ast, String sourceType, String targetType) {
    // Importe
    getCD4C().addImport(ast, "com.google.common.collect.Lists");
    getCD4C().addImport(ast, "com.google.common.collect.Sets");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("StatusResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("Command"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandManager"));
    getCD4C().addImport(ast, PathUtil.getBackendRTEUtilPath("LogUtil"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("GeneralErrorCollector"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+origCD.getCDDefinition().getName()+ ManagerDecorator.SUFFIX);
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+sourceType);
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+targetType);
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+ sourceType + "AccessPolicy");
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+ targetType + "AccessPolicy");

    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("ValidationResult"));
  }
}
