/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import umlp.generator.util.UMLPSymbolTables;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
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
import de.monticore.io.paths.MCPath;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.util.TagLookUp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ShellAPIClassDecorator extends DomainClassDecorator {
  protected Set<String> supportedDesers;

  public static final String SUFFIX = "ShellAPI";

  public ShellAPIClassDecorator(GlobalExtensionManagement glex, MCPath hwPath, Set<String> supportedDesers) {
    super(glex, hwPath, List.of());
    this.supportedDesers = supportedDesers;
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, SHELLAPI_PACKAGE);
    String cdName = origCD.getCDDefinition().getName();
    getConcreteDomainClasses(origCD).forEach(cdClass -> astcdPackage.addCDElement(withOrigin(createAPI(origCD,
                                                                                                       cdName,
                                                                                                       cdClass))));
  }

  protected ASTCDElement createAPI(ASTCDCompilationUnit origCD, String cdName, ASTCDClass cdClass) {
    String originalClassName = cdClass.getName();

    ASTCDClass shellapiClass = UMLPMill.cDClassBuilder()
        .setName(cdClass.getName() + SUFFIX)
        .setModifier(CDModifier.PUBLIC.build())
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(origCD.getCDDefinition().getName() + ShellAPIDecorator.SUFFIX))
        .build();
    glex.replaceTemplate("cd2java.Annotations", shellapiClass, new StringHookPoint("@ShellComponent\n@jsweet.lang.Erased"));

    addBasicImports(cdName, origCD, shellapiClass, originalClassName);
    addImports(cdClass, shellapiClass);
    List<ASTCDAttribute> domainAttributes = CDSymbolTables.getAttributesInHierarchy(cdClass).stream()
        .filter(attr -> !attr.getModifier().isDerived())
        .collect(Collectors.toList());
    List<ASTCDAssocSide> assocs = CDSymbolTables.getAssociationsInHierarchy(cdClass).stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .collect(Collectors.toList());

    boolean isSingleton = TagLookUp.isSingleton(cdClass);

    if (!isSingleton) {
      addCreateMethod(cdName, originalClassName, shellapiClass, domainAttributes, assocs);
    }
    addGetMethod(cdName, originalClassName, shellapiClass);
    addGetAllMethod(cdName, originalClassName, shellapiClass);
    if (!isSingleton) {
      addDeleteMethod(cdName, originalClassName, shellapiClass);
    }
    domainAttributes.stream().filter(Predicate.not(GenService::isList)).forEach(a -> addAttributeSetMethod(cdName, originalClassName, shellapiClass, a));
    domainAttributes.stream().filter(GenService::isList).forEach(a -> {
      // TODO Not yet supported
//      addAttributeAddMethod(cdName, originalClassName, shellapiClass, a);
//      addAttributeRemoveMethod(cdName, originalClassName, shellapiClass, a);
    });
    assocs.stream()
        .filter(Predicate.not(GenService::isList))
        .filter(Predicate.not(GenService::isBidirectional))
        .forEach(a -> {
          addAssocImport(origCD, shellapiClass, a);
          addAssociationSetMethod(cdName, originalClassName, shellapiClass, a);
        });
    assocs.stream()
        .filter(GenService::isList)
        .filter(Predicate.not(GenService::isBidirectional))
        .forEach(a -> {
          addAssocImport(origCD, shellapiClass, a);
          addAssociationAddMethod(cdName, originalClassName, shellapiClass, a);
          addAssociationRemoveMethod(cdName, originalClassName, shellapiClass, a);
        });
    CDSymbolTables.getAttributesInHierarchy(cdClass).forEach(attribute -> addAttributeGetMethod(cdName, originalClassName, shellapiClass, attribute));

    return shellapiClass;
  }

  private void addCreateMethod(String cdName, String originalClassName, ASTCDClass api,
                               List<ASTCDAttribute> domainAttributes, List<ASTCDAssocSide> assocs) {
    List<String> parameters = new ArrayList<>();
    ArrayList<String> annotations = new ArrayList<>();
    for (ASTCDAttribute attribute : domainAttributes) {
      String type = attribute.getMCType().printType();
      Optional<TypeSymbol> typeSymbol = UMLPSymbolTables.getTypeSymbol(attribute.getMCType());
      if(typeSymbol.isPresent()){
        String fullName = typeSymbol.get().getFullName();
        if(!fullName.isEmpty() && Character.isUpperCase(fullName.charAt(0))){
          // Domain import
        }else{
          type = fullName;
        }
      }

      if (supportedDesers.contains(type)) {
        parameters.add("String " + attribute.getName());
      } else {
        parameters.add(type + " " + attribute.getName());
      }
      annotations.add(String.format("@ShellOption(value = \"--%s\")", attribute.getName()));
    }
    for (ASTCDAssocSide association : assocs) {
      if (!GenService.isList(association) && !GenService.isOptional(association)) {
        parameters.add("Long " + association.getName());
        annotations.add(String.format("@ShellOption(value = \"--%s\")", association.getName()));
      }
    }
    List<String> domainAttributNames = domainAttributes.stream()
        .filter(Predicate.not(this::mustBeDeserialized))
        .map(ASTCDAttributeTOP::getName)
        .collect(Collectors.toList());

    Map<String, String> deserAttributeNamesAndDeSer = domainAttributes.stream()
        .filter(this::mustBeDeserialized)
        .collect(Collectors.toMap(ASTCDAttributeTOP::getName, a -> "umlp.backendrte.common." + a.getMCType().printType().replaceAll(".*[.]([^.]*)", "$1") + "DeSer"));

    List<String> assocNames = assocs.stream().filter(a -> !GenService.isList(a) && !GenService.isOptional(a)).map(ASTCDAssocSide::getName).collect(Collectors.toList());
    ASTCDMethodSignature m = getCD4C().addMethod(api, "umlp.shellapi.Create", cdName,
        originalClassName, String.join(", ", parameters), domainAttributNames, deserAttributeNamesAndDeSer, assocNames);
    String annotation = String.format("@ShellMethod(key = \"%s-create\", value = \"Create %s\") @ShellMethodAvailability(\"isConnected\")",
        StringTransformations.uncapitalize(originalClassName), originalClassName);
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint(annotation));
    addAnnotationToParameters(m, annotations);
  }

  private boolean mustBeDeserialized(ASTCDAttribute a) {
    String type;
    if (GenService.isList(a)) {
      type = a.getSymbol().getType().printFullName().replace("List<", "").replace(">", "");
    } else {
      type = a.getSymbol().getType().printFullName();
    }

    return supportedDesers.contains(type);
  }

  private void addGetMethod(String cdName, String originalClassName, ASTCDClass api) {
    ASTCDMethodSignature m = getCD4C().addMethod(api, "umlp.shellapi.Get", cdName, originalClassName);
    String annotation = String.format("@ShellMethod(key = \"%s-get\", value = \"Get %s\") @ShellMethodAvailability(\"isConnected\")",
        StringTransformations.uncapitalize(originalClassName), originalClassName);
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint(annotation));
    addAnnotationToParameters(m, "@ShellOption(value = \"--id\")");
  }

  private void addGetAllMethod(String cdName, String originalClassName, ASTCDClass api) {
    ASTCDMethodSignature m = getCD4C().addMethod(api, "umlp.shellapi.GetAll", cdName, originalClassName);
    String annotation = String.format("@ShellMethod(key = \"%s-get-all\", value = \"Get all %ss\") @ShellMethodAvailability(\"isConnected\")",
        StringTransformations.uncapitalize(originalClassName), originalClassName);
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint(annotation));
  }

  private void addDeleteMethod(String cdName, String originalClassName, ASTCDClass api) {
    ASTCDMethodSignature m = getCD4C().addMethod(api, "umlp.shellapi.Delete", cdName,
        originalClassName);
    String annotation = String.format("@ShellMethod(key = \"%s-delete\", value = \"Delete %s\") @ShellMethodAvailability(\"isConnected\")",
        StringTransformations.uncapitalize(originalClassName), originalClassName);
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint(annotation));
    addAnnotationToParameters(m, "@ShellOption(value = \"--id\")");
  }

  protected void addAttributeSetMethod(String cdName, String originalClassName, ASTCDClass api, ASTCDAttribute attr) {
    addAttributeMethod(attr, api, "umlp.shellapi.Attribute_Set", cdName, originalClassName,
        "@ShellMethod(key = \"%s-set-%s\", value = \"Set %s of %s\") @ShellMethodAvailability(\"isConnected\")");
  }

  protected void addAttributeGetMethod(String cdName, String originalClassName, ASTCDClass api, ASTCDAttribute attr) {
    String getterName = GenService.getGetterName(attr.getName(), attr.getSymbol().getType());
    if (GenService.isList(attr)) {
      getterName += "List";
    }
    ASTCDMethodSignature m = getCD4C().addMethod(api, "umlp.shellapi.Attribute_Get", cdName,
        originalClassName, attr.getName(), attr.getMCType().printType(), GenService.createType(attr).printType(), getterName);
    String annotation = String.format("@ShellMethod(key = \"%s-get-%s\", value = \"Get %s of %s\") @ShellMethodAvailability(\"isConnected\")",
        StringTransformations.uncapitalize(originalClassName), attr.getName(), attr.getName(), originalClassName);
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint(annotation));
    addAnnotationToParameters(m, "@ShellOption(value = \"--id\")");
  }

  private void addAttributeAddMethod(String cdName, String originalClassName, ASTCDClass api, ASTCDAttribute attr) {
    addAttributeMethod(attr, api, "umlp.shellapi.Attribute_Add", cdName, originalClassName,
        "@ShellMethod(key = \"%s-add-%s\", value = \"Add %s to %s\") @ShellMethodAvailability(\"isConnected\")");
  }

  private void addAttributeRemoveMethod(String cdName, String originalClassName, ASTCDClass api, ASTCDAttribute attr) {
    addAttributeMethod(attr, api, "umlp.shellapi.Attribute_Remove", cdName, originalClassName,
        "@ShellMethod(key = \"%s-remove-%s\", value = \"Remove %s from %s\") @ShellMethodAvailability(\"isConnected\")");
  }

  private void addAttributeMethod(ASTCDAttribute attr, ASTCDClass api, String originalTemplate,
                                  String cdName, String originalClassName, String methodAnnotation) {
    String type;
    if (GenService.isList(attr)) {
      type = attr.getMCType().printType();
    } else if(GenService.isOptional(attr)) {
      type = attr.getMCType().printType();
    } else {
      type = attr.getSymbol().getType().printFullName();
    }

    String template;
    if (mustBeDeserialized(attr)) {
      template = originalTemplate + "_Deser";
    } else {
      template = originalTemplate;
    }

    ASTCDMethodSignature m = getCD4C().addMethod(api, template, cdName, cdName,originalClassName, attr.getName(), type, false);
    String annotation = String.format(methodAnnotation,
        StringTransformations.uncapitalize(originalClassName), attr.getName(), attr.getName(),
        originalClassName);
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint(annotation));
    addAnnotationToParameters(m, "@ShellOption(value = \"--id\")",
        "@ShellOption(value = \"--value\")");
  }


  protected void addAssociationSetMethod(String cdName, String originalClassName, ASTCDClass api, ASTCDAssocSide assoc) {
    addAssociationMethod(assoc, api, "umlp.shellapi.Attribute_Set", cdName, originalClassName,
        "@ShellMethod(key = \"%s-set-%s\", value = \"Set %s of %s\") @ShellMethodAvailability(\"isConnected\")");
  }

  private void addAssociationAddMethod(String cdName, String originalClassName, ASTCDClass api, ASTCDAssocSide assoc) {
    addAssociationMethod(assoc, api, "umlp.shellapi.Attribute_Add", cdName, originalClassName,
        "@ShellMethod(key = \"%s-add-%s\", value = \"Add %s to %s\") @ShellMethodAvailability(\"isConnected\")");
  }

  private void addAssociationRemoveMethod(String cdName, String originalClassName, ASTCDClass api, ASTCDAssocSide assoc) {
    addAssociationMethod(assoc, api, "umlp.shellapi.Attribute_Remove", cdName, originalClassName,
        "@ShellMethod(key = \"%s-remove-%s\", value = \"Remove %s from %s\") @ShellMethodAvailability(\"isConnected\")");
  }

  private void addAssociationMethod(ASTCDAssocSide assoc, ASTCDClass api, String template, String cdName,
                                    String originalClassName, String methodAnnotation) {
    if (GenService.isBidirectional(assoc)) {
      // TODO MH not supported yet
      return;
    }
    String type;
    if (GenService.isList(assoc)) {
      type = assoc.getMCQualifiedType().printType();
    } else if(GenService.isOptional(assoc)) {
      type = assoc.getMCQualifiedType().printType();
    } else {
      type = assoc.getSymbol().getType().print();
    }

    String assocTypeCdName = getClassDiagramName(assoc);

    ASTCDMethodSignature m = getCD4C().addMethod(api, template, cdName, assocTypeCdName,
        originalClassName, assoc.getName(), type, true);
    String annotation = String.format(methodAnnotation,
        StringTransformations.uncapitalize(originalClassName), assoc.getName(), assoc.getName(),
        originalClassName);
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint(annotation));
    addAnnotationToParameters(m, "@ShellOption(value = \"--id\")",
        "@ShellOption(value = \"--value\")");
  }

  protected void addBasicImports(String cdName, ASTCDCompilationUnit origCD, ASTCDType clazz, String baseClassName) {
    getCD4C().addImport(clazz, "org.springframework.shell.standard.ShellComponent");
    getCD4C().addImport(clazz, "org.springframework.shell.standard.ShellMethod");
    getCD4C().addImport(clazz, "org.springframework.shell.standard.ShellOption");
    getCD4C().addImport(clazz, "org.springframework.shell.standard.ShellMethodAvailability");
    getCD4C().addImport(clazz, getPackageName(origCD, DEFAULT_PACKAGE) + "." + baseClassName);
    getCD4C().addImport(clazz, getPackageName(origCD, DEFAULT_PACKAGE) + "." + cdName + ManagerDecorator.SUFFIX);
    getCD4C().addImport(clazz, getPackageName(origCD, DEFAULT_PACKAGE) + "." + cdName + BaseClassObjectMapperDecorator.SUFFIX);
    getCD4C().addImport(clazz, getPackageName(origCD, DEFAULT_PACKAGE) + "." + baseClassName + DeSerDecorator.SUFFIX);
    getCD4C().addImport(clazz, getPackageName(origCD, DEFAULT_PACKAGE) + "." + baseClassName + BuilderDecorator.BUILDER_SUFFIX);
    getCD4C().addImport(clazz, getPackageName(origCD, DEFAULT_PACKAGE) + ".*");
  }

  private void addAssocImport(ASTCDCompilationUnit origCD, ASTCDClass clazz,
                              ASTCDAssocSide assoc) {
    // do not add imports for types that are injected by a module
    if(getClassDiagramName(assoc).equals(origCD.getCDDefinition().getName())) {
      String rightType = assoc.getMCQualifiedType().printType();
      getCD4C().addImport(clazz, getPackageName(origCD, DEFAULT_PACKAGE) + "." + rightType);
    }
  }

  private void addAnnotationToParameters(ASTCDMethodSignature m, String... annotations) {
    addAnnotationToParameters(m, Arrays.asList(annotations));
  }

  private void addAnnotationToParameters(ASTCDMethodSignature m, List<String> annotations) {
    if (annotations.size() != m.getCDParameterList().size()) {
      return;
    }
    for (int i = 0; i < m.getCDParameterList().size(); i++) {
      m.getCDParameter(i).setAnnotation(Optional.of(annotations.get(i)));
    }
  }
}
