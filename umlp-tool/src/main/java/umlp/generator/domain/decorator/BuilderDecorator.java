/* (c) https://github.com/MontiCore/monticore */

package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.methods.MethodDecorator;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDAttributeTOP;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.test_infrastructure.domain.decorator.MonitorDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;

import com.google.common.collect.Iterables;
import de.se_rwth.commons.Splitters;
import de.se_rwth.commons.Joiners;
import umlp.generator.util.UMLPSymbolTables;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.GenService.createType;

public class BuilderDecorator extends AbstractDecorator {

  public static final String BUILDER_SUFFIX = "Builder";

  private ASTCDCompilationUnit origCD;

  public BuilderDecorator(final GlobalExtensionManagement glex) {
    super(glex);
  }

  public Stream<ASTCDClass> getClassesToGenerateBuilderFor(ASTCDCompilationUnit origCD) {
    return getConcreteDomainClasses(origCD).stream();
  }

  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    this.origCD = origCD;
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    String name = origCD.getCDDefinition().getName();
    getClassesToGenerateBuilderFor(origCD).forEach(c -> astcdPackage.addCDElement(withOrigin(
        createClass(c, name, origCD))));
  }

  public static String getClassName(String domainClassName) {
    return domainClassName + BUILDER_SUFFIX;
  }

  protected ASTCDClass createClass(ASTCDClass domainClass, String name, ASTCDCompilationUnit origCD) {
    String className = getClassName(domainClass.getName());
    String managerName = name + ManagerDecorator.SUFFIX;

    ASTCDClass builderClass = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(domainClass.getName()))
        .setName(className).build();

    MonitorDecorator.Stereotype.NEEDS_MONITOR.addTo(builderClass);

    List<ASTCDAttribute> attrFields = CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(GenService::isVisibleNotDerived)
        .collect(Collectors.toList());
    List<ASTCDAssocSide> attrAssocs = CDSymbolTables.getAssociationsInHierarchy(domainClass).stream()
        .filter(GenService::isVisibleNotDerived)
        .filter(Predicate.not(GenService::isSymbolDerived))
        .collect(Collectors.toList());
    List<ASTCDAssocSide> attrAssocLists = attrAssocs.stream()
        .filter(GenService::isList)
        .collect(Collectors.toList());
    List<ASTCDAssocSide> attrAssocMaps = attrAssocs.stream()
        .filter(GenService::isMap)
        .collect(Collectors.toList());

    // Constructors
    addConstructor(builderClass, domainClass, managerName, attrAssocLists, attrAssocMaps,
        TagLookUp.isSingleton(domainClass) ? domainClass.getCDAttributeList().stream()
            .filter(ASTCDAttributeTOP::isPresentInitial)
            .map(a->new String[]{a.getName(), CD4CodeMill.prettyPrint(a.getInitial(), false)})
            .collect(Collectors.toList()) : new ArrayList<>());

    // Attributes
    ASTCDAttribute finishedAttr = getCD4C().addAttribute(builderClass, "protected boolean buildFinished = false");
    ASTCDAttribute internalAttr = getCD4C().addAttribute(builderClass, "protected boolean INTERNAL_deser_mode = false");
    getCD4C().addAttribute(builderClass, String.format("protected %s realBuilder;", className));

    // Methods
    MethodDecorator methodDecorator = new MethodDecorator(glex);
    builderClass.addAllCDMembers(methodDecorator.decorate(finishedAttr));
    builderClass.addAllCDMembers(methodDecorator.decorate(internalAttr));
    createSetAndCheckMethods(domainClass, builderClass, attrFields, attrAssocs);
    getCD4C().addMethod(builderClass, getBuildTemplate(),
        domainClass.getName(),
        managerName,
        attrAssocs,
        TagLookUp.isSingleton(domainClass)
    );
    // TODO: filter by mandatory
    List<String> mandatoryAttrList = new ArrayList<>();
    attrFields.forEach(it -> mandatoryAttrList.add(it.getName()));
    attrAssocs.forEach(it -> mandatoryAttrList.add(it.getName()));
    attrAssocLists.forEach(it -> mandatoryAttrList.add(it.getName()));

    getCD4C().addMethod(builderClass, getIsBuildableTemplate(), mandatoryAttrList);

    // Imports
    addImports(domainClass, builderClass);
    attrAssocLists.forEach(assocSide -> addCorrectMangerImportForTargetType(origCD.getCDDefinition(), builderClass, assocSide.getSymbol().getType()));
    attrAssocMaps.forEach(assocSide -> getCD4C().addImport(builderClass, getGemMapClass(assocSide)));

    return builderClass;
  }

  protected void addConstructor(
      ASTCDClass builderClass, ASTCDClass domainClass,
      String managerName, List<ASTCDAssocSide> attrAssocLists, List<ASTCDAssocSide> attrAssocMaps,
      List<String[]> initialAttributes) {
    Map<ASTCDAssocSide, String> correctManagerToAttrAssocSides = new LinkedHashMap<>();
    attrAssocLists.forEach(assocSide -> correctManagerToAttrAssocSides.put(assocSide,
        getCorrectManager(assocSide.getSymbol().getAssoc().getAssociation().getAstNode())));
    Map<ASTCDAssocSide, String> gemMapClassToAttrAssocSides = new LinkedHashMap<>();
    attrAssocMaps.forEach(assocSide -> gemMapClassToAttrAssocSides.put(assocSide,
        Iterables.getLast(Splitters.DOT.split(getGemMapClass(assocSide)))));
    getCD4C().addConstructor(builderClass, getConstructorTemplate(), domainClass.getName(), correctManagerToAttrAssocSides, initialAttributes,
        gemMapClassToAttrAssocSides);
  }

  /**
   * @return the qualified name of the GemMap class of the map association side, generated for the declaring class
   */
  protected String getGemMapClass(ASTCDAssocSide assocSide) {
    ASTCDType declaringType = (ASTCDType) assocSide.getSymbol().getOtherSide().getType().getTypeInfo().getAstNode();
    return Joiners.DOT.join(getPackageName(UMLPSymbolTables.getDiagramFor(declaringType), GEMMAP_PACKAGE),
        GemMapAssocDecorator.getName(declaringType, assocSide));
  }

  protected String getConstructorTemplate() {
    return "umlp.builder.Constructor";
  }

  protected String getBuildTemplate() {
    return "umlp.builder.Build";
  }

  protected String getIsBuildableTemplate() {
    return "umlp.builder.IsBuildable";
  }

  protected void createSetAndCheckMethods(ASTCDClass domainClass, ASTCDClass builderClass, List<ASTCDAttribute> attrList, List<ASTCDAssocSide> assocList) {
    for (ASTCDAttribute attribute : attrList) {
      getCD4C().addMethod(builderClass, "umlp.builder.Attr",
          attribute.getName(), getCDPrinter().prettyprint(createType(attribute)),
          getClassName(domainClass.getName()), GenService.isList(attribute));
      getCD4C().addMethod(builderClass, "umlp.builder.CheckAttr",
          attribute.getName(), domainClass.getName(),
          !GenService.isList(attribute) && !GenService.isOptional(attribute));
    }
    for (ASTCDAssocSide assoc : assocList) {
      getCD4C().addMethod(builderClass, "umlp.builder.CheckAssoc",
          assoc.getName(), domainClass.getName(),
          !(GenService.isOptional(assoc) || GenService.isMultiValued(assoc)), GenService.isMultiValued(assoc));
      if (GenService.isMap(assoc)) {
        for (String tmplt : List.of(
            "umlp.builder.AssocMapPut",
            "umlp.builder.AssocGemMapIds"
        )) {
          getCD4C().addMethod(builderClass, tmplt,
              assoc.getName(), GenService.getType(assoc.getSymbol().getType()),
              getClassName(domainClass.getName()));
        }
      } else if (GenService.isList(assoc)) {
        if (!GenService.isSymbolDerived(assoc)) {
          for(String tmplt : List.of(
              "umlp.builder.AssocAdd",
              "umlp.builder.AssocGemList",
              "umlp.builder.AssocList"
          )){
            getCD4C().addMethod(builderClass, tmplt,
                assoc.getName(), GenService.getType(assoc.getSymbol().getType()),
                getClassName(domainClass.getName()));
          }
        }
      } else {
        getCD4C().addMethod(builderClass, "umlp.builder.AssocOne",
            assoc.getName(), GenService.getType(assoc.getSymbol().getType()),
            getClassName(domainClass.getName()));
        getCD4C().addMethod(builderClass, "umlp.builder.AssocOneGem",
            assoc.getName(), GenService.getType(assoc.getSymbol().getType()),
            getClassName(domainClass.getName()));
      }
    }
  }

  protected void addImports(ASTCDType domainType, ASTCDType clazz) {
    super.addImports(domainType, clazz);
    getCD4C().addImport(clazz, "java.util.List");
    getCD4C().addImport(clazz, "java.util.Optional");
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("GemBuildable"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("LoadingState"));
  }

}
