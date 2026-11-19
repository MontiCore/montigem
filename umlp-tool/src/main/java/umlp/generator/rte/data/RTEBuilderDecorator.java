package umlp.generator.rte.data;

import de.monticore.ast.ASTNode;
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
import de.monticore.generating.templateengine.StringHookPoint;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.domain.decorator.ManagerDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static org.jsweet.transpiler.ModuleResolution.node;
import static umlp.generator.domain.decorator.GenService.createType;

public class RTEBuilderDecorator extends AbstractDecorator {

  public static final String BUILDER_SUFFIX = "Builder";

  public RTEBuilderDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    String name = origCD.getCDDefinition().getName();
    getConcreteDomainClasses(origCD).forEach(c -> astcdPackage.addCDElement(withOrigin(createClass(c,
        name,
        origCD))));
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

    // Constructors
    addConstructor(builderClass, domainClass, managerName, attrAssocLists,
        TagLookUp.isSingleton(domainClass) ? domainClass.getCDAttributeList().stream()
            .filter(ASTCDAttributeTOP::isPresentInitial)
            .map(a->new String[]{a.getName(), CD4CodeMill.prettyPrint(a.getInitial(), false)})
            .collect(Collectors.toList()) : new ArrayList<>());

    // Attributes
    ASTCDAttribute finishedAttr = getCD4C().addAttribute(builderClass, "protected boolean buildFinished = false");
    getCD4C().addAttribute(builderClass, String.format("protected %s realBuilder;", className));

    // Methods
    MethodDecorator methodDecorator = new MethodDecorator(glex);
    builderClass.addAllCDMembers(methodDecorator.decorate(finishedAttr));
    createSetAndCheckMethods(domainClass, builderClass, attrFields, attrAssocs);
    ASTNode buildMethod = getCD4C().addMethod(builderClass, getBuildTemplate(),
        domainClass.getName(),
        attrAssocLists,
        TagLookUp.isSingleton(domainClass)
    );
    setAnnotations(buildMethod, new StringHookPoint("@Override"));
    // TODO: filter by mandatory
    List<String> mandatoryAttrList = new ArrayList<>();
    attrFields.forEach(it -> mandatoryAttrList.add(it.getName()));
    attrAssocs.forEach(it -> mandatoryAttrList.add(it.getName()));
    attrAssocLists.forEach(it -> mandatoryAttrList.add(it.getName()));

    getCD4C().addMethod(builderClass, getIsBuildableTemplate(), mandatoryAttrList);

    // Imports
    addImports(domainClass, builderClass);
    attrAssocLists.forEach(assocSide -> addCorrectMangerImportForTargetType(origCD.getCDDefinition(), builderClass, assocSide.getSymbol().getType()));

    return builderClass;
  }

  protected void addConstructor(
      ASTCDClass builderClass, ASTCDClass domainClass,
      String managerName, List<ASTCDAssocSide> attrAssocLists,
      List<String[]> initialAttributes) {
    Map<ASTCDAssocSide, String> correctManagerToAttrAssocSides = new LinkedHashMap<>();
    attrAssocLists.forEach(assocSide -> correctManagerToAttrAssocSides.put(assocSide,
        getCorrectManager(assocSide.getSymbol().getAssoc().getAssociation().getAstNode())));
    getCD4C().addConstructor(builderClass, getConstructorTemplate(), domainClass.getName(), correctManagerToAttrAssocSides, initialAttributes);
  }

  protected String getConstructorTemplate() {
    return "rte.builder.Constructor";
  }

  protected String getBuildTemplate() {
    return "rte.builder.Build";
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
          false);
    }
    for (ASTCDAssocSide assoc : assocList) {
      boolean isList = GenService.isList(assoc);
      String type = GenService.getType(assoc.getSymbol().getType());
      String fullType = type;
      if(GenService.isList(assoc)) {
        fullType = getMCTypeFacade().createBasicGenericTypeOf("List",type).printType();
      } else if(GenService.isOptional(assoc)) {
        fullType = getMCTypeFacade().createBasicGenericTypeOf("Optional",type).printType();
      }
      getCD4C().addMethod(builderClass, "rte.builder.Assoc",
          assoc.getName(), fullType,
          getClassName(domainClass.getName()), isList);
      getCD4C().addMethod(builderClass, "umlp.builder.CheckAttr",
          assoc.getName(), domainClass.getName(),
          false);
    }
  }

  protected void addImports(ASTCDType domainType, ASTCDType clazz) {
    super.addImports(domainType, clazz);
    getCD4C().addImport(clazz, "java.util.List");
    getCD4C().addImport(clazz, "java.util.Optional");
  }
}
