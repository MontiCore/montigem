/* (c) https://github.com/MontiCore/monticore */

package umlp.generator.domain.decorator;

import com.google.common.collect.Lists;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.TopDecorator;
import de.monticore.cd.codegen.methods.MethodDecorator;
import de.monticore.cd4code.typescalculator.FullSynthesizeFromCD4Code;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cd4codebasis._ast.ASTCDParameter;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDClassBuilder;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDInterfaceUsage;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdinterfaceandenum._ast.ASTCDEnum;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.io.paths.MCPath;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.TypeCheck;
import de.monticore.types.check.TypeCheckResult;
import de.monticore.types.mcbasictypes.MCBasicTypesMill;
import de.monticore.types.mcbasictypes._ast.ASTConstantsMCBasicTypes;
import de.monticore.types.mcbasictypes._ast.ASTMCObjectType;
import de.monticore.umlmodifier._ast.ASTModifier;
import de.monticore.umlstereotype._ast.ASTStereotypeBuilder;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.SourcePosition;
import de.se_rwth.commons.StringTransformations;
import org.jetbrains.annotations.NotNull;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.commands.OwnCommands;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;
import umlp.generator.util.UMLPSymbolTables;
import umlp.generator.ocl.OCLAttributeExtractor;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import de.monticore.ocl.ocl._ast.ASTOCLConstraint;
import de.monticore.ocl.ocl._ast.ASTOCLInvariant;
import de.monticore.ocl.ocl._ast.ASTOCLContextDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static de.monticore.cd.facade.CDModifier.PUBLIC_ABSTRACT;

public class DomainClassDecorator extends DomainTypeDecorator implements OwnCommands {

  // TODO: add/remove methods for lists of basic types, e.g. List<String>

  protected final MCPath hwPath;
  protected final List<ASTOCLCompilationUnit> oclCompilationUnits;

  public DomainClassDecorator(final GlobalExtensionManagement glex,
                              MCPath hwPath,
                              List<ASTOCLCompilationUnit> oclCompilationUnits) {
    super(glex);
    this.hwPath = hwPath;
    this.oclCompilationUnits = new ArrayList<>(oclCompilationUnits);
  }

  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    getNonDerivedDomainClasses(origCD).forEach(c -> astcdPackage.addCDElement(withOrigin(createClass(
        origCD,
        decoratedCD,
        c
    ))));

    origCD.getCDDefinition().getCDEnumsList().stream().filter(c -> !c.getSymbol().isIsDerived())
        .forEach(c -> {
          ASTCDEnum enumCopy = c.deepClone();
          enumCopy.setModifier(PUBLIC.build());
          astcdPackage.addCDElement(withOrigin(enumCopy));
        });
  }

  protected ASTCDClass createClass(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD, ASTCDClass domainClass) {
    ASTCDInterfaceUsage implementsUsage = getInterfaceUsage(domainClass);
    ASTModifier modifier = domainClass.getModifier().isAbstract() ? PUBLIC_ABSTRACT.build() : PUBLIC.build();
    ASTCDClassBuilder builder = UMLPMill.cDClassBuilder()
        .setModifier(modifier)
        .setCDInterfaceUsage(implementsUsage)
        .setName(domainClass.getName());
    if (domainClass.isPresentCDExtendUsage()) {
      builder.setCDExtendUsage(getExtendUsageFacade()
          .createCDExtendUsage(domainClass.getCDExtendUsage().getSuperclass(0).deepClone()));
    }

    List<ASTCDAttribute> derivedAttribFields = addNeedsTopStereotypes(domainClass, modifier);
    //Build class
    ASTCDClass clazz = builder.build();

    // Constructors
    getCD4C().addConstructor(clazz, getConstructorTemplate(), domainClass.getName());

    // Attributes
    AttributesTuple attributes = getAllAttributes(domainClass, false, origCD);

    if (!domainClass.isPresentCDExtendUsage()) {
      // Attribute gemId and getter and setter
      ASTCDAttribute gemAttr = getCD4C().addAttribute(clazz, "protected readonly long gemId = -32;");
      clazz.addAllCDMembers(new MethodDecorator(glex).decorate(gemAttr));
    }

    // Attribute observerList
    ASTCDAttribute attrObserver = addAttribute(clazz,
                                               " protected List<" + domainClass.getName() + "Observer> " + StringTransformations.uncapitalize(
                                                   domainClass.getName()) + "Observers",
                                               defaultListValue());
    addTransientModifier(attrObserver);

    // Add attributes to class
    List<ASTCDAttribute> nonDerivedAttrs = getNonDerivedAttributes(attributes.attrList);
    clazz.addAllCDMembers(nonDerivedAttrs);
    for (ASTCDAttribute attr : nonDerivedAttrs) {
      if (GenService.isGem(attr) && !GenService.isGemList(attr)) {
        glex.replaceTemplate("cd2java.Value", attr, new StringHookPoint(getInitialGemValue()));
      } else if (GenService.isList(attr)) {
        glex.replaceTemplate("cd2java.Value", attr, new StringHookPoint(defaultListValue()));
      } else if (GenService.isBoolean(attr.getMCType())) {
        glex.replaceTemplate("cd2java.Value", attr, new StringHookPoint(defaultBooleanValue()));
      }
    }

    // General methods
    createObserverMethods(domainClass, clazz);
    createEqualsMethod(domainClass, clazz);
    getCD4C().addMethod(clazz, getBuildTemplate(), domainClass.getName());
    createIsBuildFinishedMethod(clazz);
    createIsBuildableMethod(clazz);
    createGetDeSerMethod(domainClass, clazz);
    createRegisterMeMethod(domainClass, clazz);

    // Methods for class attributes
    createGetterSetterForAttrs(domainClass, clazz, getNonDerivedAttributes(attributes.attrFields), false);
    createGetterSetterForAttrs(domainClass, clazz, derivedAttribFields, true);
    createCheckMethods(domainClass, clazz);

    // Methods for class assocs
    createGetterSetterForAssocs(domainClass, clazz, attributes.attrAssocs, false);
    createIsPresentForOptionalAssocs(
        domainClass,
        clazz,
        attributes.attrAssocs.stream().filter(attr -> attr.getModifier().isPresentStereotype()
            && attr.getModifier().getStereotype().streamValues().anyMatch(v -> v.getName().equals("Optional")))
            .collect(Collectors.toList())
    );

    createOCLValidationMethods(domainClass, clazz);

    // Derived but implemented(stereotype GenService.DERIVED_IMPL_TEMPLATE) attributes and assocs
    List<ASTCDAttribute> derivedImplementedAttributes = getDerivedImplementedAttributes(attributes.attrFields);
    for (ASTCDAttribute a : derivedImplementedAttributes) {
      String tmpl = a.getModifier().getStereotype().getValue(GenService.DERIVED_IMPL_TEMPLATE);
      ASTCDMethod m = (ASTCDMethod)  getCD4C().addMethod(clazz, tmpl,
          origCD,
          decoratedCD,
          a, // ast with the stereotype
          a.getMCType(), // return type of the method
          List.of() // parameters of the method
      );
    }

    List<ASTCDAssocSide> derivedImplementedAssocs = CDSymbolTables.getAssociationsInHierarchy(domainClass).stream()
        .filter(GenService::isSymbolDerived)
        .filter(GenService::hasImplTemplate)
        .collect(Collectors.toList());

    for (ASTCDAssocSide a : derivedImplementedAssocs) {
      String tmpl =  a.getSymbol().getAssoc().getAssociation().getAstNode()
          .getModifier().getStereotype().getValue(GenService.DERIVED_IMPL_TEMPLATE);

      ASTCDMethod m = (ASTCDMethod) getCD4C().addMethod(clazz, tmpl,
          origCD,
          decoratedCD,
          a, // ast with the stereotype
          a.getMCQualifiedType(), // return type of the method
          List.of() // parameters of the method
      );
    }

    if (TagLookUp.isSingleton(domainClass)) {
      domainClass.getCDMethodList().forEach(m -> createExeceuteCommandMethod(domainClass, clazz, m));
    }

    // Imports
    addImports(domainClass, clazz);
    addExtendedImport(origCD, clazz);
    addImportsForCommands(domainClass, clazz);
    return clazz;
  }

  protected @NotNull List<ASTCDAttribute> addNeedsTopStereotypes(ASTCDClass domainClass, ASTModifier modifier) {
    // Stereotype
    ASTStereotypeBuilder stereotype = UMLPMill.stereotypeBuilder();
    List<ASTCDAttribute> derivedAttribFields = getDerivedNonImplementedAttributes(domainClass.getCDAttributeList());
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
        .filter(side -> !GenService.hasImplTemplate(side))
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
    return derivedAttribFields;
  }

  protected void createIsPresentForOptionalAssocs(ASTCDClass domainClass, ASTCDClass clazz, List<ASTCDAttribute> optionalAttrs) {
    for (ASTCDAttribute optionalAttr : optionalAttrs) {
      getCD4C().addMethod(clazz, "umlp.domain.IsPresentOptional", optionalAttr.getName().substring(1));
    }
  }

  protected void addExtendedImport(ASTCDCompilationUnit origCD, ASTCDType ast) {
    UMLPSymbolTables.getExtendedCDs(origCD).forEach(diagramSymbol -> {
      String packageName = diagramSymbol.getPackageName().isEmpty() ? diagramSymbol.getName() : Joiners.DOT.join(diagramSymbol.getPackageName(), diagramSymbol.getName());
      String importName = Joiners.DOT.join(packageName.toLowerCase(), diagramSymbol.getName() + ManagerDecorator.SUFFIX);
      getCD4C().addImport(ast, importName);
    });
  }
  protected String getInitialGemValue() {
    return "= new Gem<>()";
  }

  protected void createIsBuildFinishedMethod(ASTCDClass clazz) {
  }

  protected void createIsBuildableMethod(ASTCDClass clazz) {
  }

  protected String getConstructorTemplate() {
    return "umlp.domain.Constructor";
  }

  protected String getBuildTemplate() {
    return "umlp.domain.Build";
  }

  protected void addTransientModifier(ASTCDAttribute attrObserver) {
    glex.replaceTemplate("cd2java.Annotations", attrObserver,
        new StringHookPoint("transient"));
  }

  protected String defaultListValue() {
    return "= new ArrayList<>()";
  }

  protected String defaultBooleanValue() {
    return "= false";
  }

  protected ASTCDInterfaceUsage getInterfaceUsage(ASTCDClass domainClass) {
    ASTCDInterfaceUsage interfaceUsage = getInterfaceUsageFacade()
        .createCDInterfaceUsage("GemIdentifiable", "GemBuildable");
    for (ASTMCObjectType superInterface : domainClass.getInterfaceList()) {
      interfaceUsage.addInterface(superInterface);
    }
    return interfaceUsage;
  }

  protected void createCheckMethods(ASTCDClass domainClass, ASTCDClass clazz) {
    for (ASTCDAttribute attr : CDSymbolTables.getAttributesInHierarchy(domainClass)
        .stream().filter(GenService::isVisibleNotDerived).collect(Collectors.toList())) {
      getCD4C().addMethod(clazz, "umlp.domain.Check",
          domainClass.getName(), attr.getName(),
          !GenService.isList(attr) && !GenService.isOptional(attr) && !attr.getModifier().isDerived(),
          false
      );
    }
    for (ASTCDAssocSide assoc : CDSymbolTables.getAssociationsInHierarchy(domainClass)) {
      if (GenService.isAStLeastOne(assoc)) {
        getCD4C().addMethod(clazz, "umlp.domain.CheckCard",
            domainClass.getName(),
            assoc.getName(),
            !GenService.isSymbolDerived(assoc),
            GenService.isList(assoc)
        );
      } else {
        getCD4C().addMethod(clazz, "umlp.domain.Check",
            domainClass.getName(),
            assoc.getName(),
            !GenService.isOptional(assoc) && !GenService.isSymbolDerived(assoc),
            GenService.isList(assoc)
        );
      }
    }
  }

  protected void createObserverMethods(ASTCDClass domainClass, ASTCDClass clazz) {
    getCD4C().addMethod(clazz, "umlp.domain.AddObserver", domainClass.getName(), clazz.isPresentCDExtendUsage());
    getCD4C().addMethod(clazz, "umlp.domain.RemoveObserver", domainClass.getName());
    getCD4C().addMethod(clazz, "umlp.domain.ContainsObserver", domainClass.getName());
  }

  protected void createEqualsMethod(ASTCDClass domainClass, ASTCDClass clazz) {

    List<String> primitives = new ArrayList<>();
    List<String> primitiveBooleans = new ArrayList<>();
    List<String> wrappedBooleans = new ArrayList<>();
    List<String> objects = new ArrayList<>();
    FullSynthesizeFromCD4Code synthesize = new FullSynthesizeFromCD4Code();

    for (ASTCDAttribute attribute : CDSymbolTables.getAttributesInHierarchy(domainClass)) {
      if (attribute.getModifier().isDerived() || attribute.getModifier().isAbstract()) {
        continue;
      }
      TypeCheckResult result = synthesize.synthesizeType(attribute.getMCType());
      if (result.isPresentResult()) {
        SymTypeExpression expression = result.getResult();
        boolean isBool = TypeCheck.isBoolean(expression);
        if (expression.isPrimitive()) {
          if (isBool) {
            primitiveBooleans.add(attribute.getName());
          } else {
            primitives.add(attribute.getName());
          }
        } else {
          if (isBool) {
            wrappedBooleans.add(attribute.getName());
          } else {
            objects.add(attribute.getName());
          }
        }
      }
    }

    getCD4C().addMethod(clazz, getEqualsTemplate(), primitives, objects, primitiveBooleans, wrappedBooleans, domainClass.getName());
    addDeepEqualsMethod(domainClass, clazz);
  }

  protected String getEqualsTemplate() {
    return "umlp.domain.Equals";
  }

  protected void createExeceuteCommandMethod(ASTCDClass domainClass, ASTCDClass clazz, ASTCDMethod command) {
    List<ASTCDParameter> adjustedParameters = Lists.newArrayList();
    command.getCDParameterList().forEach(p -> adjustedParameters.add(getAdjustedMethodParameter(p)));
    String params = Joiners.COMMA.join(adjustedParameters.stream().map(p ->
            UMLPMill.prettyPrint(p.getMCType(), false) + " " + p.getName()).collect(Collectors.toList()));
    String call = Joiners.COMMA.join(adjustedParameters.stream().map(p -> p.getName()).collect(Collectors.toList()));
    String returnType = buildReturnType(command, clazz).printType();
    String asType = returnType.contains("<")? returnType.substring(0, returnType.indexOf("<")): returnType;
    getCD4C().addMethod(clazz, getExecuteCommandTemplate(), StringTransformations.capitalize(command.getName()),
        params, returnType, call, asType);
  }

  protected void createGetDeSerMethod(ASTCDClass domainClass, ASTCDClass clazz) {
    // Abstract classes have no DeSer; they declare getDeSer() abstract so the
    // method is statically callable on any domain type (also after JSweet)
    getCD4C().addMethod(clazz, getGetDeSerTemplate(), domainClass.getName(),
        domainClass.getModifier().isAbstract());
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("JsonDeSer"));
  }

  protected String getGetDeSerTemplate() {
    return "umlp.domain.GetDeSer";
  }

  protected void createRegisterMeMethod(ASTCDClass domainClass, ASTCDClass clazz) {
    getCD4C().addMethod(clazz, getRegisterMeTemplate(),
        domainClass.getName(),
        getCorrectManager(domainClass),
        domainClass.getModifier().isAbstract(),
        TagLookUp.isSingleton(domainClass));
  }

  protected String getRegisterMeTemplate() {
    return "umlp.domain.RegisterMe";
  }

  protected void addImportsForCommands(ASTCDClass domainType, ASTCDClass cdType) {
    if (TagLookUp.isSingleton(domainType) && !domainType.getCDMethodList().isEmpty()) {
      getCD4C().addImport(cdType, PathUtil.getBackendRTECommandPath("CommandManager"));
      domainType.getCDMethodList().forEach(m -> {
        // Add import for generated command
        getCD4C().addImport(cdType,
            getPackageName(UMLPSymbolTables.getDiagramFor(domainType), "commands.") +
                StringTransformations.capitalize(m.getName()));

        // Add imports for parameters
        m.getCDParameterList().forEach(p -> {
          SymTypeExpression type = p.getSymbol().getType();
          addImport(type, cdType);
        });

        // Add import for return type
        SymTypeExpression returnType = m.getSymbol().getType();
        addImport(returnType, cdType);
      });
    }
  }

  protected void addImport(SymTypeExpression type, ASTCDClass ast) {
    if(type.isVoidType()) return;

    if (GenService.isJavaType(type)){
      addJavaImport(ast, type);
    }else if (GenService.isDomainType(type) || GenService.isPojoCDType(type)){
      getCD4C().addImport(ast, Joiners.DOT.join(type.getTypeInfo().getPackageName().toLowerCase(), type.print()));
    }
  }

  protected String getExecuteCommandTemplate() {
    return "umlp.domain.ExecuteCommand";
  }

  protected ASTCDParameter getAdjustedMethodParameter(ASTCDParameter param) {
    ASTCDParameter adjustedParam = param.deepClone();
    if (GenService.isCDOrInnerCDEnum(param.getSymbol().getType())) {
      // keep original
    } else if (isOptionalGem(param.getSymbol().getType())) {
      adjustedParam.setName(param.getName() + "GemId");
      adjustedParam.setMCType(MCTypeFacade.getInstance().createOptionalTypeOf("Long"));
    } else if (isGem(param.getSymbol().getType())) {
      adjustedParam.setName(param.getName() + "GemId");
      adjustedParam.setMCType(MCBasicTypesMill.mCPrimitiveTypeBuilder()
                                  .setPrimitive(ASTConstantsMCBasicTypes.LONG).build());
    } else if (isListOfGemIdentifiable(param.getSymbol().getType())) {
      adjustedParam.setName(param.getName() + "GemId");
      adjustedParam.setMCType(MCTypeFacade.getInstance().createListTypeOf("Long"));
    }
    return adjustedParam;
  }

  protected void createOCLValidationMethods(ASTCDClass domainClass, ASTCDClass clazz) {
    String typeName = domainClass.getName();
    List<InvariantInfo> typeInvariants = new ArrayList<>();
    Map<String, List<InvariantInfo>> attrInvariants = new HashMap<>();

    if (!oclCompilationUnits.isEmpty()) {
      String domainPackage = domainClass.getSymbol().getPackageName().toLowerCase();

      for (ASTOCLCompilationUnit ocl : oclCompilationUnits) {
        String oclClassName = ocl.getOCLArtifact().getName();
        String oclPackage = ocl.isPresentPackage() ? ocl.getPackage() : "";

        if (!oclPackage.isEmpty()) {
          if (!domainPackage.isEmpty() && !oclPackage.startsWith(domainPackage)) {
            oclClassName = domainPackage + "." + oclPackage + "." + oclClassName;
          } else {
            oclClassName = oclPackage + "." + oclClassName;
          }
        } else if (!domainPackage.isEmpty()) {
          oclClassName = domainPackage + "." + oclClassName;
        }


        int invCounter = 0;
        for (ASTOCLConstraint constraint : ocl.getOCLArtifact().getOCLConstraintList()) {
          if (constraint instanceof ASTOCLInvariant) {
            ASTOCLInvariant invariant = (ASTOCLInvariant) constraint;

            // Skip invariants with more than one parameter as our checkInv/getErrorInv calls currently only support 'this'
            if (invariant.getOCLContextDefinitionList().size() != 1) {
              continue;
            }

            Optional<String> contextParam = Optional.empty();

            for (ASTOCLContextDefinition ctx : invariant.getOCLContextDefinitionList()) {
              String ctxType = "";
              if (ctx.isPresentMCType()) {
                ctxType = ctx.getMCType().printType();
              } else if (ctx.isPresentOCLParamDeclaration()) {
                ctxType = ctx.getOCLParamDeclaration().getMCType().printType();
              }

              if (ctxType.equals(typeName) || ctxType.endsWith("." + typeName)) {
                if (ctx.isPresentMCType()) {
                  contextParam = Optional.of("this");
                } else {
                  contextParam = Optional.of(ctx.getOCLParamDeclaration().getName());
                }
                break;
              }
            }

            if (contextParam.isPresent()) {
              String invName = invariant.isPresentName() ? invariant.getName() : "Inv" + (invCounter++);

              String simpleOCLClassName = oclClassName;
              if (simpleOCLClassName.contains(".")) {
                simpleOCLClassName = simpleOCLClassName.substring(simpleOCLClassName.lastIndexOf(".") + 1);
              }

              InvariantInfo info = new InvariantInfo(simpleOCLClassName, invName);
              typeInvariants.add(info);

              // Add import for the OCL class
              getCD4C().addImport(clazz, oclClassName);

              OCLAttributeExtractor.Result extractionResult = OCLAttributeExtractor.extract(invariant, contextParam.get());

              if (extractionResult.hasErrorMessages) {
                for (String attr : extractionResult.usedAttributes) {
                  attrInvariants.computeIfAbsent(attr, k -> new ArrayList<>()).add(info);
                }
              }
            } else {
              // Even if not context, it might be anonymous but we don't have the context param name here
              // Increment counter to keep numbering consistent with OCLWithErrorMessagesJavaGenVisitor
              if (!invariant.isPresentName()) {
                invCounter++;
              }
            }
          }
        }
      }
    }

    getCD4C().addMethod(clazz, "umlp.domain.CheckOCLConstraints", typeInvariants, clazz.getName());

    Set<String> allAttributes = CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived())
        .map(ASTCDAttribute::getName)
        .collect(Collectors.toSet());

    // Also include associations as they might be validated or used in GUI
    CDSymbolTables.getAssociationsInHierarchy(domainClass).stream()
        .filter(a -> !GenService.isSymbolDerived(a))
        .map(ASTCDAssocSide::getName)
        .forEach(allAttributes::add);

    for (String attr : allAttributes) {
      List<InvariantInfo> invs = attrInvariants.getOrDefault(attr, new ArrayList<>());
      getCD4C().addMethod(clazz, "umlp.domain.GetValidationError", attr, invs, clazz.getName());
    }

    // Add imports
    getCD4C().addImport(clazz, "mc.fenix.input.geminputbasetypes.ValidationError");
  }

  public static class InvariantInfo {
    protected String className;
    protected String invName;

    public InvariantInfo(String className, String invName) {
      this.className = className;
      this.invName = invName;
    }

    public String getClassName() {
      return className;
    }

    public String getCheckMethodName() {
      return "check" + invName;
    }

    public String getErrorMethodName() {
      return "getError" + invName;
    }

    public String getMethodName() {
      return getCheckMethodName();
    }
  }

  protected List<ASTCDAttribute> getDerivedImplementedAttributes(List<ASTCDAttribute> attributes) {
    return attributes.stream().filter(attr ->
        attr.getModifier().isDerived()
            && (attr.getModifier().isPresentStereotype() && attr.getModifier().getStereotype().contains(GenService.DERIVED_IMPL_TEMPLATE))
    ).collect(Collectors.toList());
  }

  protected List<ASTCDAttribute> getDerivedNonImplementedAttributes(List<ASTCDAttribute> attributes) {
    return attributes.stream().filter(attr ->
        attr.getModifier().isDerived()
        && !(attr.getModifier().isPresentStereotype() && attr.getModifier().getStereotype().contains(GenService.DERIVED_IMPL_TEMPLATE))
    ).collect(Collectors.toList());
  }

  protected List<ASTCDAttribute> getNonDerivedAttributes(List<ASTCDAttribute> attributes) {
    return attributes.stream().filter(attr ->
        !(attr.getModifier().isDerived())
    ).collect(Collectors.toList());
  }
}
