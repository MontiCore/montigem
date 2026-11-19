package umlp.generator.rte.data;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDAttributeTOP;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDElement;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.check.SymTypeOfGenerics;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.domain.decorator.BaseClassObjectMapperDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.PathUtil;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.monticore.cd.codegen.CD2JavaTemplates.EMPTY_BODY;
import static de.monticore.cd.facade.CDModifier.PROTECTED;
import static de.monticore.cd.facade.CDModifier.PROTECTED_STATIC;
import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static de.monticore.cd.facade.CDModifier.PUBLIC_ABSTRACT;

public class RTEDeSerDecorator extends AbstractDecorator {

  public static final String SUFFIX = "DeSer";

  private final Set<String> supportedDesers;

  public RTEDeSerDecorator(GlobalExtensionManagement glex, Set<String> supportedDesers) {
    super(glex);
    this.supportedDesers = supportedDesers;
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);

    getNonDerivedDomainClasses(origCD).stream().map(c ->
        c.getSymbol().getPackageName().toLowerCase()+"."+c.getSymbol().getName()).forEach(supportedDesers::add);

    getNonDerivedDomainClasses(origCD).forEach(c -> astcdPackage.addCDElement(withOrigin(createDeSer(
        origCD, origCD.getCDDefinition().getName(), c))));

    astcdPackage.addCDElement(createDiagramDeSer(origCD, getNonDerivedDomainClasses(origCD)));
  }

  protected ASTCDElement createDiagramDeSer(ASTCDCompilationUnit origCD, List<ASTCDClass> classes) {
    String deSerName = origCD.getCDDefinition().getName() + SUFFIX;
    ASTCDClass deSerRepo = UMLPMill.cDClassBuilder()
        .setName(deSerName)
        .setModifier(CDModifier.PUBLIC.build())
        .setCDInterfaceUsage(getInterfaceUsageFacade().createCDInterfaceUsage("DeSerRepository"))
        .build();

    ASTCDAttribute attr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createBasicGenericTypeOf("Map", "String", "JsonDeSer<?>"),
        StringTransformations.uncapitalize("deSers"));
    deSerRepo.addCDMember(attr);

    ASTCDAttribute attrInstance = getCDAttributeFacade().createAttribute(PROTECTED_STATIC.build(),
        deSerRepo.getName(),
        "_" + StringTransformations.uncapitalize(deSerRepo.getName()));
    deSerRepo.addCDMember(attrInstance);

    ASTCDMethod method = getCDMethodFacade().createMethod(PUBLIC.build(),
        getMCTypeFacade().createBasicGenericTypeOf("Map", "String", "JsonDeSer"),
        "getDeSerMap");
    glex.replaceTemplate(EMPTY_BODY, method, new StringHookPoint("return this._" + attr.getName()));


    List<String> classNames = classes.stream().map(c -> c.getSymbol().getPackageName().toLowerCase() + "." + c.getName()).collect(Collectors.toList());

    getCD4C().addMethod(deSerRepo, getDeSerMapMethod(), classNames);

    getCD4C().addMethod(deSerRepo, getInstaceMethod(), deSerRepo.getName());

    glex.replaceTemplate("cd2java.Value", attr, new StringHookPoint(" = new HashMap<String, JsonDeSer<?>>();"));

    getCD4C().addImport(deSerRepo, "java.util.Map");
    getCD4C().addImport(deSerRepo, "java.util.HashMap");
    getCD4C().addImport(deSerRepo, "mc.fenix.commonj2ts.deser.DeSerRepository");
    getCD4C().addImport(deSerRepo, "umlp.backendrte.command.JsonDeSer");
    getCD4C().addImport(deSerRepo, "de.se_rwth.commons.logging.Log");
    getCD4C().addImport(deSerRepo, "umlp.backendrte.common.BaseClassObjectMapper");

    return deSerRepo;
  }

  protected String getDeSerMapMethod() {
    return "rte.deser.GetDeSerMap";
  }

  protected String getObjectMapperSerialize() {
    return "rte.deser.ObjectMapperSerialize";
  }

  protected String getObjectMapperDeserialize() {
    return "rte.deser.ObjectMapperDeserialize";
  }

  protected String getInstaceMethod() {
    return "rte.deser.GetInstance";
  }

  protected String getConstructorTemplate() {
    return "rte.deser.DiagramDeSerConstructor";
  }

  protected ASTCDElement createDeSer(ASTCDCompilationUnit origCD, String cdName, ASTCDClass cdClass) {
    String originalClassName = cdClass.getName();

    ASTCDClass deSer = UMLPMill.cDClassBuilder()
        .setName(cdClass.getName() + SUFFIX)
        .setModifier(CDModifier.PUBLIC.build())
        .setCDInterfaceUsage(getInterfaceUsageFacade().createCDInterfaceUsage("PojoJsonDeSer<" + cdClass.getName() + ">"))
        .build();

    // TODO: also use for other templates!
    List<ASTCDAttribute> attributes = CDSymbolTables.getAttributesInHierarchy(cdClass).stream()
        .filter(attr -> !attr.getModifier().isDerived())
        .collect(Collectors.toList());
    attributes.forEach(a -> addSerAndDeserMethod(cdName, originalClassName, deSer, a));
    List<ASTCDAssocSide> assocs = CDSymbolTables.getAssociationsInHierarchy(cdClass).stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .collect(Collectors.toList());
    assocs.forEach(a -> addSerAndDeserMethod(cdName, originalClassName, deSer, a));


    getCD4C().addMethod(deSer, getSerializeTemplate(), originalClassName);
    getCD4C().addMethod(deSer, "rte.deser.CreateObject", originalClassName);
    getCD4C().addMethod(deSer, "rte.deser.FinishObject", originalClassName);
    ASTCDMethod typeIdentifier = getCDMethodFacade().createMethod(PUBLIC.build(),
        getMCTypeFacade().createStringType(), "getTypeIdentifier");
    glex.replaceTemplate(EMPTY_BODY, typeIdentifier, new StringHookPoint("return \""
        + GenService.getCorrectJavaFQN(cdClass.getSymbol().getFullName()) + "\";"));
    deSer.addCDMember(typeIdentifier);
    getCD4C().addMethod(deSer, getIsPojoDeSerTemplate(), true);
    getCD4C().addMethod(deSer, getDeserializeTemplate(),
        cdName,
        originalClassName,
        Stream.concat(attributes.stream().map(ASTCDAttributeTOP::getName),
            assocs.stream().map(ASTCDAssocSide::getName)).collect(Collectors.toList()));
    getCD4C().addMethod(deSer, "rte.deser.ReadJson", originalClassName,
        Stream.concat(attributes.stream().map(ASTCDAttributeTOP::getName),
            assocs.stream().map(ASTCDAssocSide::getName)).collect(Collectors.toList()));
    getCD4C().addMethod(deSer, "rte.deser.WriteJson", originalClassName,
        Stream.concat(attributes.stream().map(ASTCDAttributeTOP::getName),
            assocs.stream().map(ASTCDAssocSide::getName)).collect(Collectors.toList()));
    getCD4C().addMethod(deSer, getBuildJsonTemplate(),
        originalClassName,
        attributes.stream().map(ASTCDAttributeTOP::getName).collect(Collectors.toList()),
        assocs.stream().map(ASTCDAssocSide::getName).collect(Collectors.toList())
    );

    getCD4C().addMethod(deSer, getFromJsonTemplate(), originalClassName);
    addImports(cdClass, deSer);
    getCD4C().addImport(deSer, "umlp.backendrte.common.BaseClassObjectMapper");
    assocs.forEach(assocSide -> addCorrectMangerImportForTargetType(origCD.getCDDefinition(), deSer, assocSide.getSymbol().getType()));

    return deSer;
  }

  protected void addSerAndDeserMethod(String cdName, String originalClassName, ASTCDClass deSer, ASTCDAttribute attr) {
    boolean isList = false;
    boolean isOptional = false;
    boolean useDeSer = false;
    String type = GenService.createType(attr.getSymbol().getType()).printType();
    String innerTypeFull = "";
    String jsonStr = "";
    if (GenService.isList(attr)) {
      isList = true;
      jsonStr = GenService.calculateJSon(((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0));
      innerTypeFull = GenService.getCorrectJavaFQN(
          ((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0).printFullName());
      useDeSer = supportedDesers.contains(innerTypeFull);
    } else if (GenService.isOptional(attr)) {
      isOptional = true;
      jsonStr = GenService.calculateJSon(((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0));
      innerTypeFull = GenService.getCorrectJavaFQN(
          ((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0).printFullName());
      useDeSer = supportedDesers.contains(innerTypeFull);
    } else {
      jsonStr = GenService.calculateJSon(attr.getSymbol().getType());
      type = GenService.getCorrectJavaFQN(attr.getSymbol().getType().printFullName());
      if(attr.getSymbol().getType().isGenericType()) {
        innerTypeFull = GenService.getCorrectJavaFQN(
            ((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0).printFullName());
        type = GenService.createType(attr.getSymbol().getType()).printType();
      }
      if(type.contains(".")) {
        useDeSer = supportedDesers.contains(type);
      } else {
        useDeSer = supportedDesers.contains(attr.getSymbol().getPackageName().toLowerCase()+"."+type);
      }
    }
    boolean makeAbstract = jsonStr.isEmpty() && !useDeSer;

    getCD4C().addMethod(deSer, getSerializeSimpleFieldTemplate(),
        originalClassName, attr, type, jsonStr, isList, innerTypeFull, isOptional,
        useDeSer, "context");
    getCD4C().addMethod(deSer, getDeserializeSimpleFieldTemplate(),
        originalClassName, attr, type, jsonStr, isList, innerTypeFull, isOptional,
        useDeSer, "context");

    if (!useDeSer) {
      addContextDelegates(deSer, originalClassName, attr.getName(), type);
    }

    if (makeAbstract) {
      deSer.setModifier(PUBLIC_ABSTRACT.build());
    }
  }

  protected void addSerAndDeserMethod(String cdName, String originalClassName, ASTCDClass deSer, ASTCDAssocSide attr) {
    boolean isList = false;
    boolean isOptional = false;
    boolean useDeSer;
    String type = attr.getSymbol().getType().print();
    String innerTypeFull = "";
    String jsonStr = "";
    if (GenService.isList(attr)) {
      isList = true;
      jsonStr = GenService.calculateJSon(attr.getSymbol().getType());
      innerTypeFull = GenService.getCorrectJavaFQN(attr.getSymbol().getType().printFullName());
      useDeSer = supportedDesers.contains(innerTypeFull);
      type = getMCTypeFacade().createBasicGenericTypeOf("List",
          GenService.createType(attr.getSymbol().getType()).printType()).printType();
    } else if (GenService.isOptional(attr)) {
      isOptional = true;
      jsonStr = GenService.calculateJSon(attr.getSymbol().getType());
      innerTypeFull = GenService.getCorrectJavaFQN(attr.getSymbol().getType().printFullName());
      useDeSer = supportedDesers.contains(innerTypeFull);
      type = getMCTypeFacade().createBasicGenericTypeOf("Optional",
          GenService.createType(attr.getSymbol().getType()).printType()).printType();
    } else {
      jsonStr = GenService.calculateJSon(attr.getSymbol().getType());
      type = GenService.getCorrectJavaFQN(attr.getSymbol().getType().printFullName());
      useDeSer = supportedDesers.contains(type);
    }

    boolean makeAbstract = jsonStr.isEmpty() && !useDeSer;

    getCD4C().addMethod(deSer, getSerializeSimpleFieldTemplate(),
        originalClassName, attr, type, jsonStr, isList, innerTypeFull, isOptional,
        useDeSer, "context");
    getCD4C().addMethod(deSer, getDeserializeSimpleFieldTemplate(),
        originalClassName, attr, type, jsonStr, isList, innerTypeFull, isOptional,
        useDeSer, "context");

    if (!useDeSer) {
      addContextDelegates(deSer, originalClassName, attr.getName(), type);
    }

    if (makeAbstract) {
      deSer.setModifier(PUBLIC_ABSTRACT.build());
    }
  }

  protected String getIsPojoDeSerTemplate() {
    return "umlp.deser.IsPojoDeSer";
  }

  protected String getSerializeSimpleFieldTemplate() {
    return "umlp.deser.serializeSimpleField";
  }

  protected void addContextDelegates(ASTCDClass deSer, String className, String name, String type) {
    // Preserve handwritten serializers for non-serializable fields (e.g. callbacks).
    getCD4C().addMethod(deSer, "rte.deser.SerializeFieldWithContext", className, name);
    getCD4C().addMethod(deSer, "rte.deser.DeserializeFieldWithContext", name, type);
  }

  protected String getDeserializeSimpleFieldTemplate() {
    return "umlp.deser.deserializeSimpleField";
  }

  protected void addImports(ASTCDType domainType, ASTCDType clazz) {
    super.addImports(domainType, clazz);
    getCD4C().addImport(clazz, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(clazz, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(clazz, "de.monticore.symboltable.serialization.json.JsonElement");
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("JsonDeSer"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("PojoJsonDeSer"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("PojoGraphContext"));
  }

  protected String getSerializeTemplate() {
    return "rte.deser.Serialize";
  }

  protected String getDeserializeTemplate() {
    return "rte.deser.deserialize";
  }

  protected String getBuildJsonTemplate() {
    return "rte.deser.BuildJson";
  }

  protected String getFromJsonTemplate() {
    return "umlp.deser.FromJson";
  }

}
