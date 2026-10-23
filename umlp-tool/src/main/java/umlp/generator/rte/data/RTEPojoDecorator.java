package umlp.generator.rte.data;

import com.google.common.collect.Lists;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.TopDecorator;
import de.monticore.cd.codegen.methods.MethodDecorator;
import de.monticore.cd.codegen.methods.mutator.OptionalMutatorDecorator;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDClassBuilder;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdinterfaceandenum._ast.ASTCDEnum;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.io.paths.MCPath;
import de.monticore.umlmodifier._ast.ASTModifier;
import de.monticore.umlstereotype._ast.ASTStereotypeBuilder;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.SourcePosition;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.DomainTypeDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.domain.decorator.ManagerDecorator;
import umlp.generator.domain.decorator.methods.MethodGenService;
import umlp.generator.domain.decorator.methods.UMLPAccessorDecorator;
import umlp.generator.domain.decorator.methods.UMLPMutatorDecorator;
import umlp.generator.rte.data.methods.RTEListMutatorDecorator;
import umlp.generator.rte.data.methods.RTEMandatoryMutatorDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PROTECTED;
import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static de.monticore.cd.facade.CDModifier.PUBLIC_ABSTRACT;


public class RTEPojoDecorator extends DomainTypeDecorator {

  protected final MCPath hwPath;

  public RTEPojoDecorator(final GlobalExtensionManagement glex, MCPath hwPath) {
    super(glex);
    this.hwPath = hwPath;
  }

  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    getNonDerivedDomainClasses(origCD).forEach(c -> astcdPackage.addCDElement(withOrigin(createClass(
        origCD,
        c))));

    origCD.getCDDefinition().getCDEnumsList().stream().filter(c -> !c.getSymbol().isIsDerived())
        .forEach(c -> {
          ASTCDEnum enumCopy = c.deepClone();
          enumCopy.setModifier(PUBLIC.build());
          astcdPackage.addCDElement(withOrigin(enumCopy));
        });
  }

  protected ASTCDClass createClass(ASTCDCompilationUnit origCD, ASTCDClass domainClass) {
    ASTModifier modifier = domainClass.getModifier().isAbstract() ? PUBLIC_ABSTRACT.build() : PUBLIC.build();
    ASTCDClassBuilder builder = UMLPMill.cDClassBuilder()
        .setModifier(modifier)
        .setName(domainClass.getName());
    if (domainClass.isPresentCDExtendUsage()) {
      builder.setCDExtendUsage(getExtendUsageFacade()
          .createCDExtendUsage(domainClass.getCDExtendUsage().getSuperclass(0).deepClone()));
    }

    // Stereotype
    ASTStereotypeBuilder stereotype = UMLPMill.stereotypeBuilder();
    List<ASTCDAttribute> derivedAttribFields = filterDerivedAttributes(domainClass.getCDAttributeList(), true);
    if (!derivedAttribFields.isEmpty()) {
      String derivedAttribs = derivedAttribFields.stream()
          .map(attr -> {
            SourcePosition sourcePosition = attr.get_SourcePositionStart();
            return String.format(
                "%s in %s:%s:%s",
                attr.getName(),
                sourcePosition.getFileName().orElse("unknown"),
                sourcePosition.getLine(),
                sourcePosition.getColumn()
            );
          }).collect(Collectors.joining(", "));
      stereotype.addValues(
          TopDecorator.NEEDS_TOP_STEREO_BUILDER.apply(
              String.format("Custom implementation for class: %s required - use the TOP mechanism. Contains derived attribute(s): %s", "%s", derivedAttribs)
          )
      );
    }
    List<ASTCDAssocSide> derivedAssocs = CDSymbolTables.getAssociationsInHierarchy(domainClass).stream()
        .filter(GenService::isSymbolDerived)
        .collect(Collectors.toList());
    if (!derivedAssocs.isEmpty()) {
      String derivedAssocsStr = derivedAssocs.stream()
          .map(assoc -> {
            SourcePosition sourcePosition = assoc.get_SourcePositionStart();
            return String.format(
                "%s in %s:%s:%s",
                assoc.getName(),
                sourcePosition.getFileName().orElse("unknown"),
                sourcePosition.getLine(),
                sourcePosition.getColumn()
            );
          }).collect(Collectors.joining(", "));
      stereotype.addValues(
          TopDecorator.NEEDS_TOP_STEREO_BUILDER.apply(
              String.format("Custom implementation for class: %s required - use the TOP mechanism. Contains derived association(s): %s", "%s", derivedAssocsStr)
          )
      );
    }
    modifier.setStereotype(stereotype.build());
    //Build class
    ASTCDClass clazz = builder.build();

    // Attributes
    DomainTypeDecorator.AttributesTuple attributes = getAllAttributes(domainClass, false, origCD);

    // Add attributes to class
    List<ASTCDAttribute> nonDerivedAttrs = filterDerivedAttributes(attributes.attrList, false);
    clazz.addAllCDMembers(nonDerivedAttrs);
    for (ASTCDAttribute attr : nonDerivedAttrs) {
      if (GenService.isList(attr)) {
        glex.replaceTemplate("cd2java.Value", attr, new StringHookPoint(defaultListValue()));
      }
    }

    // General methods
    getCD4C().addMethod(clazz, getBuildTemplate(), domainClass.getName());
    createIsBuildFinishedMethod(clazz);
    createIsBuildableMethod(clazz);

    // Methods for class attributes
    createGetterSetterForAttrs(domainClass, clazz, filterDerivedAttributes(attributes.attrFields, false), false);
    createGetterSetterForAttrs(domainClass, clazz, derivedAttribFields, false);

    createGetterSetterForAttrs(domainClass, clazz,  attributes.attrAssocs, false);

    // Constructors
    getCD4C().addConstructor(clazz, getConstructorTemplate(), domainClass.getName(), nonDerivedAttrs);
    getCD4C().addConstructor(clazz, getBuilderConstructorTemplate(), domainClass.getName());

    createCheckMethods(domainClass, clazz);

    // Imports
    addImports(domainClass, clazz);
    addExtendedImport(origCD, clazz);
    return clazz;
  }

  protected void addExtendedImport(ASTCDCompilationUnit origCD, ASTCDType ast) {
    UMLPSymbolTables.getExtendedCDs(origCD).forEach(diagramSymbol -> {
      String packageName = diagramSymbol.getPackageName().isEmpty() ? diagramSymbol.getName() : Joiners.DOT.join(diagramSymbol.getPackageName(), diagramSymbol.getName());
      String importName = Joiners.DOT.join(packageName.toLowerCase(), diagramSymbol.getName() + ManagerDecorator.SUFFIX);
      getCD4C().addImport(ast, importName);
    });
  }

  protected void createIsBuildFinishedMethod(ASTCDClass clazz) {
  }

  protected void createIsBuildableMethod(ASTCDClass clazz) {
  }

  protected String getConstructorTemplate() {
    return "rte.data.PublicConstructorWithParameters";
  }

  protected String getBuilderConstructorTemplate() {
    return "rte.data.PublicConstructor";
  }

  protected String getBuildTemplate() {
    return "umlp.domain.Build";
  }

  protected String defaultListValue() {
    return "= new ArrayList<>()";
  }

  protected String defaultBooleanValue() {
    return "= false";
  }

  /**
   * Filters the derived attributes from the list of attributes
   *
   * @param attributes list of attributes
   * @param getDerived whether to return derived attributes or non-derived attributes
   * @return list of attributes (only derived ones if getDerived is true else only non-derived ones)
   */
  protected List<ASTCDAttribute> filterDerivedAttributes(List<ASTCDAttribute> attributes, boolean getDerived) {
    return attributes.stream().filter(attr -> !getDerived ^ attr.getModifier().isDerived()).collect(Collectors.toList());
  }

  @Override
  protected void createGetterSetterForAttrs(ASTCDType domainType, ASTCDType cdType,
                                            List<ASTCDAttribute> attrList, boolean isInterface) {
    CDGenService service = new MethodGenService();
    MethodDecorator methodDecorator = new MethodDecorator(glex,
        new UMLPAccessorDecorator(glex, service),
        new UMLPMutatorDecorator(glex, new RTEMandatoryMutatorDecorator(glex, service, domainType),
            new OptionalMutatorDecorator(glex, service),
            new RTEListMutatorDecorator(glex, service, domainType)), service);
    for (ASTCDAttribute attribute : attrList) {
      if (GenService.isList(attribute)) {
        getCD4C().addImport(cdType, "java.util.function.Predicate");
        getCD4C().addImport(cdType, "java.util.Spliterator");
        getCD4C().addImport(cdType, "java.util.ListIterator");
        getCD4C().addImport(cdType, "java.util.function.Consumer");
        getCD4C().addImport(cdType, "java.util.function.UnaryOperator");
        getCD4C().addImport(cdType, "java.util.Comparator");
      }
      List<ASTCDMethod> methods = methodDecorator.decorate(attribute);
      methods.forEach(m -> m.getModifier().setAbstract(isInterface));
      cdType.addAllCDMembers(methods);
      getCD4C().addMethod(cdType, "umlp.domain.SetPresent", service.getNativeAttributeName(attribute.getName()));
    }
  }


  @Override
  protected List<ASTCDAttribute> getAttributesForAssociations(ASTCDType domainType, boolean isInterface, ASTCDCompilationUnit cd) {
    List<ASTCDAttribute> attrList = Lists.newLinkedList();
    List<ASTCDAssocSide> associations = (isInterface ?
        CDSymbolTables.getAssociations(domainType) :
        CDSymbolTables.getAssociationsInHierarchy(domainType))
        .stream()
        .filter(GenService::isVisible)
        .collect(Collectors.toList());

    // handle associations
    for (ASTCDAssocSide assocSide : associations) {
      String type = assocSide.getSymbol().getType().print();
      ASTCDAttribute attribute;
      if(GenService.isList(assocSide)) {
        attribute = getCDAttributeFacade().createAttribute(
            PROTECTED.build(),
            getMCTypeFacade().createBasicGenericTypeOf( "List", type),
            GenService.createName(assocSide.getName())
        );
      } else if(GenService.isOptional(assocSide)) {
        attribute = getCDAttributeFacade().createAttribute(
            PROTECTED.build(),
            getMCTypeFacade().createBasicGenericTypeOf("Optional" , type),
            GenService.createName(assocSide.getName())
        );
      } else {
        attribute = getCDAttributeFacade().createAttribute(
            PROTECTED.build(),
            type,
            GenService.createName(assocSide.getName())
        );
      }

      if (GenService.isSymbolDerived(assocSide)) {
        attribute.getModifier().setDerived(true);
      }
      if (GenService.isBidirectional(assocSide)) {
        addStereotype(attribute.getModifier(), STEREOTYPE_OTHERSIDE, assocSide.getSymbol().getOtherSide().getName());
        if (GenService.isList(assocSide.getSymbol().getOtherSide().getAssocSide())) {
          addStereotype(attribute.getModifier(), STEREOTYPE_OTHERSIDE_ISLIST, "");
        }
      }
      attrList.add(attribute);
    }
    return attrList;
  }

  protected void createCheckMethods(ASTCDClass domainClass, ASTCDClass clazz) {
    for (ASTCDAttribute attr : CDSymbolTables.getAttributesInHierarchy(domainClass)
        .stream().filter(GenService::isVisibleNotDerived).collect(Collectors.toList())) {
      getCD4C().addMethod(clazz, "umlp.domain.Check",
          domainClass.getName(), attr.getName(),
          false,
          false
      );
    }
    for (ASTCDAssocSide assoc : CDSymbolTables.getAssociationsInHierarchy(domainClass)) {
      if (GenService.isAStLeastOne(assoc)) {
        getCD4C().addMethod(clazz, "umlp.domain.CheckCard",
            domainClass.getName(),
            assoc.getName(),
            false,
            GenService.isList(assoc)
        );
      } else {
        getCD4C().addMethod(clazz, "umlp.domain.Check",
            domainClass.getName(),
            assoc.getName(),
            false,
            GenService.isList(assoc)
        );
      }
    }
  }

  @Override
  protected void addImports(ASTCDType domainType, ASTCDType cdType) {
    CDSymbolTables.getAttributesInHierarchy(domainType).forEach(a -> addJavaImport(cdType, a.getSymbol().getType()));
    CDSymbolTables.getAssociationsInHierarchy(domainType).forEach(assocSide -> {
      getPackageFromFQNString(assocSide.getSymbol().getType().printFullName())
          .ifPresent(cdPackage ->
              getCD4C().addImport(cdType, Joiners.DOT.join(cdPackage.toLowerCase(),
                  getTypeFromFQNString(assocSide.getSymbol().getType().printFullName()))));
    });
    domainType.getInterfaceList().forEach(cdInterface ->
        UMLPSymbolTables.getTypeSymbol(cdInterface).ifPresent(definingSymbol ->
            getCD4C().addImport(cdType, Joiners.DOT.join(definingSymbol.getPackageName().toLowerCase(),
                definingSymbol.getName()))));
    domainType.getSuperclassList().forEach(cdExtension ->
        UMLPSymbolTables.getTypeSymbol(cdExtension).ifPresent(definingSymbol ->
            getCD4C().addImport(cdType, Joiners.DOT.join(definingSymbol.getPackageName().toLowerCase(),
                definingSymbol.getName()))));

    addDBAnnotationInfo(cdType);
    if (cdType.getModifier().isPresentStereotype()&& cdType.getModifier().getStereotype().contains(DomainTypeDecorator.STEREOTYPE_MANAGER)) {
      getCD4C().addImport(cdType, getPackageName(UMLPSymbolTables.getDiagramFor(domainType), DEFAULT_PACKAGE+"."
          +cdType.getModifier().getStereotype().getValue(DomainTypeDecorator.STEREOTYPE_MANAGER)));
    }
  }
}
