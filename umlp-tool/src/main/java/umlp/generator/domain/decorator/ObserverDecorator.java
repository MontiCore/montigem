/* (c) https://github.com/MontiCore/monticore */

package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDExtendUsage;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.types.check.SymTypeExpression;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.GenService.createType;

public class ObserverDecorator extends AbstractDecorator {

  public static final String SUFFIX = "Observer";
  public static final String NOTIFY_ADD_TEMPLATE = "umlp.observer.NotifyAdd";
  public static final String MAYBE_NOTIFY_ADD_TEMPLATE = "umlp.observer.MaybeNotifyAdd";
  public static final String NOTIFY_REMOVE_TEMPLATE = "umlp.observer.NotifyRemove";
  public static final String MAYBE_NOTIFY_REMOVE_TEMPLATE = "umlp.observer.MaybeNotifyRemove";
  public static final String NOTIFY_TEMPLATE = "umlp.observer.Notify";
  public static final String MAYBE_NOTIFY_TEMPLATE = "umlp.observer.MaybeNotify";

  protected Collection<GenerationSummary> generationDetails = List.of();

  public ObserverDecorator(final GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    this.generationDetails = new ArrayList<>();

    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);

    getNonDerivedDomainClasses(origCD)
        .forEach(c -> astcdPackage.addCDElement(withOrigin(createObserverInterface(c))));

    getNonDerivedDomainInterfaces(origCD)
        .forEach(c -> astcdPackage.addCDElement(withOrigin(createObserverInterface(c))));
  }

  protected ASTCDInterface createObserverInterface(ASTCDType domainClass) {
    String className = domainClass.getName();
    ASTCDInterface observerInterface = UMLPMill.cDInterfaceBuilder()
        .setModifier(PUBLIC.build())
        .setName(className + SUFFIX)
        .build();
    List<SymTypeExpression> superTypes = domainClass.getSymbol().getSuperTypesList();
    if (!superTypes.isEmpty()) {
      String[] superTypeObserverNames = superTypes.stream().map(a -> a.print() + SUFFIX).toArray(String[]::new);
      ASTCDExtendUsage cdExtendUsage = getExtendUsageFacade().createCDExtendUsage(superTypeObserverNames);
      observerInterface.setCDExtendUsage(cdExtendUsage);
    }
    GenerationSummary generationSummary = new GenerationSummary(domainClass,
                                                                observerInterface,
                                                                new ArrayList<>());
    this.generationDetails.add(generationSummary);

    CD4C cd4C = getCD4C();

    // Add Notify for attributes
    List<ASTCDAttribute> attributes = domainClass.getCDAttributeList().stream()
        .filter(a -> !a.getModifier().isDerived()).collect(Collectors.toList());
    attributes.forEach(a -> {
          String attributeName = a.getName();
          if (GenService.isList(a)) {
            String innerType = GenService.getInnerType(a.getMCType());
            generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                          NOTIFY_ADD_TEMPLATE,
                                                          attributeName,
                                                          className,
                                                          innerType), NOTIFY_ADD_TEMPLATE);
            generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                          MAYBE_NOTIFY_ADD_TEMPLATE,
                                                          attributeName,
                                                          className,
                                                          innerType), MAYBE_NOTIFY_ADD_TEMPLATE);
            generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                          NOTIFY_REMOVE_TEMPLATE,
                                                          attributeName,
                                                          className,
                                                          innerType), NOTIFY_REMOVE_TEMPLATE);
            generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                          MAYBE_NOTIFY_REMOVE_TEMPLATE,
                                                          attributeName,
                                                          className,
                                                          innerType), MAYBE_NOTIFY_REMOVE_TEMPLATE);
          }

          String fullType = getCDPrinter().prettyprint(createType(a));
          generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                        NOTIFY_TEMPLATE,
                                                        attributeName,
                                                        className,
                                                        fullType,
                                                        false), NOTIFY_TEMPLATE);
          generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                        MAYBE_NOTIFY_TEMPLATE,
                                                        attributeName,
                                                        className,
                                                        fullType,
                                                        false), MAYBE_NOTIFY_TEMPLATE);
        });
    addImport(observerInterface, attributes);

    // Add Notify for list and map associations
    CDSymbolTables.getAssociations(domainClass).stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .filter(GenService::isMultiValued)
        .forEach(a -> {
          generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                        NOTIFY_ADD_TEMPLATE,
                                                        a.getName(),
                                                        className,
                                                        "long"), NOTIFY_ADD_TEMPLATE);
          generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                        MAYBE_NOTIFY_ADD_TEMPLATE,
                                                        a.getName(),
                                                        className,
                                                        "long"), MAYBE_NOTIFY_ADD_TEMPLATE);
          generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                        NOTIFY_REMOVE_TEMPLATE,
                                                        a.getName(),
                                                        className,
                                                        "long"), NOTIFY_REMOVE_TEMPLATE);
          generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                        MAYBE_NOTIFY_REMOVE_TEMPLATE,
                                                        a.getName(),
                                                        className,
                                                        "long"), MAYBE_NOTIFY_REMOVE_TEMPLATE);
          if (GenService.isMap(a)) {
            // maps have no positions: no set at index
            return;
          }
          generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                        NOTIFY_TEMPLATE,
                                                        a.getName(),
                                                        className,
                                                        "long",
                                                        true), NOTIFY_TEMPLATE);
          generationSummary.addSignature(cd4C.addMethod(observerInterface,
                                                        MAYBE_NOTIFY_TEMPLATE,
                                                        a.getName(),
                                                        className,
                                                        "long",
                                                        true), MAYBE_NOTIFY_TEMPLATE);
        });

    // Add Notify for associations
    CDSymbolTables.getAssociations(domainClass).stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .filter(Predicate.not(GenService::isMultiValued))
        .forEach(a -> {
          generationSummary.addSignature(cd4C.addMethod(observerInterface, NOTIFY_TEMPLATE, a.getName(), className, "long", false),
                                         NOTIFY_TEMPLATE);
          generationSummary.addSignature(cd4C.addMethod(observerInterface, MAYBE_NOTIFY_TEMPLATE, a.getName(), className, "long", false),
                                         MAYBE_NOTIFY_TEMPLATE);
        });

    cd4C.addMethod(observerInterface, "umlp.observer.GetScope");
    cd4C.addMethod(observerInterface, "umlp.observer.NotifyBuild", className);
    cd4C.addMethod(observerInterface, "umlp.observer.MaybeNotifyBuild", className);

    cd4C.addImport(observerInterface, PathUtil.getBackendRTECommonPath("NotificationScope"));
    addImportForImportedObservers(domainClass, observerInterface);
    addImports(domainClass, observerInterface);

    return observerInterface;
  }

  protected void addImport(ASTCDType observerType, List<ASTCDAttribute> attributes) {
  }

  protected void addImportForImportedObservers(ASTCDType domainClass, ASTCDType observerType) {
    CDTypeSymbol symbol = domainClass.getSymbol();
    if (symbol.isPresentSuperClass()) {
      SymTypeExpression superclass = symbol.getSuperClass();
      ASTCDCompilationUnit scCD = UMLPSymbolTables.getDiagramFor((ASTCDType) superclass.getTypeInfo().getAstNode());
      getCD4C().addImport(observerType, getPackageName(scCD, DEFAULT_PACKAGE) + "." + superclass.print() + "Observer");
    }
  }

  public Collection<GenerationSummary> getGenerationSummaries() {
    return generationDetails;
  }

  //compiler can't work with records
  public static final class GenerationSummary {

    private final ASTCDType domainClass;
    private final ASTCDInterface observerInterface;
    private final Collection<GeneratedMethodSignature> notifyMethodSignatures;

    public GenerationSummary(ASTCDType domainClass,
                             ASTCDInterface observerInterface,
                             Collection<GeneratedMethodSignature> notifyMethodSignatures) {
      this.domainClass = domainClass;
      this.observerInterface = observerInterface;
      this.notifyMethodSignatures = notifyMethodSignatures;
    }

    private void addSignature(ASTCDMethodSignature signature, String template) {
      notifyMethodSignatures.add(new GeneratedMethodSignature(template, signature));
    }

    public ASTCDType domainClass() {
      return domainClass;
    }

    public ASTCDInterface observerInterface() {
      return observerInterface;
    }

    public Collection<GeneratedMethodSignature> notifyMethodSignatures() {
      return notifyMethodSignatures;
    }

  }
}
