/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd.methodtemplates.CD4CTemplateHelper;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.*;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.io.paths.MCPath;
import de.monticore.types.check.SymTypeOfGenerics;
import de.se_rwth.commons.Joiners;
import umlp.UMLPMill;
import umlp.generator.util.PathUtil;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC_ABSTRACT;

public class DeSerDecorator extends DomainClassDecorator {

  public static final String SUFFIX = "DeSer";

  private final Set<String> supportedDesers;

  public DeSerDecorator(GlobalExtensionManagement glex, MCPath hwPath,
                        Set<String> supportedDesers) {
    super(glex, hwPath, List.of());
    this.supportedDesers = supportedDesers;
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    String cdName = origCD.getCDDefinition().getName();
    getConcreteDomainClasses(origCD).forEach(cdClass -> astcdPackage.addCDElement(withOrigin(createDeSer(origCD,
                                                                                                         cdName,
                                                                                                         cdClass))));
  }

  protected ASTCDElement createDeSer(ASTCDCompilationUnit origCD, String cdName, ASTCDClass cdClass) {
    String originalClassName = cdClass.getName();

    ASTCDClass deSer = UMLPMill.cDClassBuilder()
        .setName(cdClass.getName() + SUFFIX)
        .setModifier(CDModifier.PUBLIC.build())
        .setCDInterfaceUsage(getInterfaceUsageFacade().createCDInterfaceUsage("JsonDeSer<" + cdClass.getName() + ">"))
        .build();

    // TODO: also use for other templates!
    List<ASTCDAttribute> attributes = CDSymbolTables.getAttributesInHierarchy(cdClass).stream()
        .filter(attr -> !attr.getModifier().isDerived())
        .collect(Collectors.toList());
    attributes.forEach(a -> addSerAndDeserMethod(origCD.getCDDefinition().getName(), originalClassName, deSer, a));
    List<ASTCDAssocSide> assocs = CDSymbolTables.getAssociationsInHierarchy(cdClass).stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .collect(Collectors.toList());
    assocs.forEach(a -> addSerAndDeserMethod(cdName, originalClassName, deSer, a));


    getCD4C().addMethod(deSer, getSerializeTemplate(), originalClassName);
    getCD4C().addMethod(deSer, getDeserializeTemplate(),
        cdName,
        originalClassName,
        attributes.stream().map(ASTCDAttributeTOP::getName).collect(Collectors.toList()),
        assocs.stream().filter(Predicate.not(GenService::isMultiValued)).map(ASTCDAssocSide::getName).collect(Collectors.toList()),
        assocs.stream().filter(GenService::isList).map(ASTCDAssocSide::getName).collect(Collectors.toList()),
        assocs.stream().filter(GenService::isMap).map(ASTCDAssocSide::getName).collect(Collectors.toList())
    );
    getCD4C().addMethod(deSer, getBuildJsonTemplate(),
        originalClassName,
        attributes.stream().map(ASTCDAttributeTOP::getName).collect(Collectors.toList()),
        assocs.stream().map(ASTCDAssocSide::getName).collect(Collectors.toList())
    );

    getCD4C().addMethod(deSer, getFromJsonTemplate(), originalClassName);

    // Same key format as the CommandResultDeSer switches, e.g. "cdbase.ConcreteBaseClass"
    getCD4C().addMethod(deSer, getGetTypeIdentifierTemplate(),
        Joiners.DOT.join(getPackageName(origCD, DEFAULT_PACKAGE), originalClassName));

    addDeSerAddons(deSer, cdClass);

    addImports(cdClass, deSer);
    assocs.forEach(assocSide -> addCorrectMangerImportForTargetType(origCD.getCDDefinition(), deSer, assocSide.getSymbol().getType()));

    return deSer;
  }

  protected void addDeSerAddons(ASTCDClass deSer, ASTCDClass cdClass) {
    CD4CTemplateHelper th = new CD4CTemplateHelper();
    th.method("protected void serialize_Addons(" + cdClass.getName() + " obj, JsonPrinter jsonPrinter)");
    deSer.addCDMember(th.getMethod().get());
    th.method("protected void deserialize_Addons(" + cdClass.getName() + "Builder builder, JsonObject json)");
    deSer.addCDMember(th.getMethod().get());
  }

  protected String getSerializeTemplate() {
    return "umlp.deser.serialize";
  }

  protected String getDeserializeTemplate() {
    return "umlp.deser.deserialize";
  }

  protected String getBuildJsonTemplate() {
    return "umlp.deser.BuildJson";
  }

  protected String getFromJsonTemplate() {
    return "umlp.deser.FromJson";
  }

  protected String getGetTypeIdentifierTemplate() {
    return "umlp.deser.GetTypeIdentifier";
  }

  protected void addSerAndDeserMethod(String cdName, String originalClassName, ASTCDClass deSer, ASTCDAttribute attr) {
    boolean isList = false;
    boolean isOptional = false;
    boolean useDeSer = false;
    String type = GenService.createType(attr.getSymbol().getType()).printType();
    String innerTypeFull = "";
    String innerType = "";
    String jsonStr = "";
    if (GenService.isList(attr)) {
      isList = true;
      jsonStr = GenService.calculateJSon(((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0));
      innerTypeFull = ((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0).printFullName();
      useDeSer = supportedDesers.contains(innerTypeFull);
    } else if (GenService.isOptional(attr)) {
      isOptional = true;
      jsonStr = GenService.calculateJSon(((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0));
      innerTypeFull = ((SymTypeOfGenerics) attr.getSymbol().getType()).getArgument(0).printFullName();
      useDeSer = supportedDesers.contains(innerTypeFull);
    } else {
      jsonStr = GenService.calculateJSon(attr.getSymbol().getType());
      useDeSer = supportedDesers.contains(type);
    }
    boolean makeAbstract = jsonStr.isEmpty() && !useDeSer;

    getCD4C().addMethod(deSer, getSerializeSimpleFieldTemplate(),
        originalClassName, attr, type, jsonStr, isList, innerTypeFull, isOptional,
        useDeSer, cdName + BaseClassObjectMapperDecorator.SUFFIX);
    getCD4C().addMethod(deSer, getDeserializeSimpleFieldTemplate(),
        originalClassName, attr, type, jsonStr, isList, innerTypeFull, isOptional,
        useDeSer, cdName + BaseClassObjectMapperDecorator.SUFFIX);

    if (makeAbstract) {
      deSer.setModifier(PUBLIC_ABSTRACT.build());
    }
  }

  protected void addSerAndDeserMethod(String cdName, String originalClassName, ASTCDClass deSer, ASTCDAssocSide attr) {
    if (GenService.isMap(attr)) {
      // the ids of the values, their keys are derived from them
      getCD4C().addMethod(deSer, "umlp.deser.serializeGemMapField", originalClassName, attr.getName());
      getCD4C().addMethod(deSer, getDeserializeGemListFieldTemplate(), cdName, originalClassName, attr.getName());
    } else if (GenService.isList(attr)) {
      getCD4C().addMethod(deSer, getSerializeGemListFieldTemplate(), originalClassName, attr.getName());
      getCD4C().addMethod(deSer, getDeserializeGemListFieldTemplate(), cdName, originalClassName, attr.getName());
    } else {
      getCD4C().addMethod(deSer, getSerializeGemFieldTemplate(), originalClassName, attr.getName());
      getCD4C().addMethod(
          deSer,
          getDeserializeGemFieldTemplate(),
          getCorrectManager(attr),
          originalClassName,
          attr.getName(),
          attr.getSymbol().getType().print());
    }
  }

  protected String getSerializeSimpleFieldTemplate() {
    return "umlp.deser.serializeSimpleField";
  }

  protected String getDeserializeSimpleFieldTemplate() {
    return "umlp.deser.deserializeSimpleField";
  }

  protected String getSerializeGemListFieldTemplate() {
    return "umlp.deser.serializeGemListField";
  }

  protected String getDeserializeGemListFieldTemplate() {
    return "umlp.deser.deserializeGemListField";
  }

  protected String getSerializeGemFieldTemplate() {
    return "umlp.deser.serializeGemField";
  }

  protected String getDeserializeGemFieldTemplate() {
    return "umlp.deser.deserializeGemField";
  }

  protected void addImports(ASTCDType domainType, ASTCDType clazz) {
    super.addImports(domainType, clazz);
    getCD4C().addImport(clazz, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(clazz, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(clazz, "de.monticore.symboltable.serialization.json.JsonElement");
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("JsonDeSer"));
  }

}
