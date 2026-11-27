/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static de.monticore.cd.facade.CDModifier.PUBLIC;

public class DiagramObserverDecorator extends AbstractDecorator {

  public static final String SUFFIX = "Observer";
  public static final String PREFIX = "Empty";
  public static final String NOTIFY_ADD_TEMPLATE = "umlp.diagram.NotifyAdd";
  public static final String NOTIFY_DELETE_TEMPLATE = "umlp.diagram.NotifyDelete";
  public static final String MAYBE_NOTIFY_ADD_TEMPLATE = "umlp.diagram.MaybeNotifyAdd";
  public static final String MAYBE_NOTIFY_DELETE_TEMPLATE = "umlp.diagram.MaybeNotifyDelete";
  public static final String GET_SCOPE_GLOBAL_TEMPLATE = "umlp.common_observer.GetScopeGlobal";

  private Optional<GenerationSummary> generationSummary = Optional.empty();

  public DiagramObserverDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    ASTCDInterface inter = createInterface(origCD);
    astcdPackage.addCDElement(withOrigin(inter));
    astcdPackage.addCDElement(withOrigin(createEmptyImpl(origCD, inter)));
  }

  protected ASTCDInterface createInterface(ASTCDCompilationUnit origCD) {
    String name = origCD.getCDDefinition().getName() + SUFFIX;

    ASTCDInterface ast = UMLPMill.cDInterfaceBuilder()
            .setModifier(PUBLIC.build())
            .setName(name).build();

    GenerationSummary generationSummary = new GenerationSummary(new ArrayList<>(),
                                                                new ArrayList<>(),
                                                                new ArrayList<>());
    this.generationSummary = Optional.of(generationSummary);

    CD4C cd4C = getCD4C();

    // Methods
    getConcreteNonPojoDomainClasses(origCD).forEach(c -> {
      String cName = c.getName();
      generationSummary.addMethod(cd4C.addMethod(ast, NOTIFY_ADD_TEMPLATE, cName), NOTIFY_ADD_TEMPLATE, c);
      generationSummary.addMethod(cd4C.addMethod(ast, NOTIFY_DELETE_TEMPLATE, cName), NOTIFY_DELETE_TEMPLATE, c);
      generationSummary.addMethod(cd4C.addMethod(ast, MAYBE_NOTIFY_ADD_TEMPLATE, cName), MAYBE_NOTIFY_ADD_TEMPLATE, c);
      generationSummary.addMethod(cd4C.addMethod(ast, MAYBE_NOTIFY_DELETE_TEMPLATE, cName),
                                  MAYBE_NOTIFY_DELETE_TEMPLATE,
                                  c);
    });

    generationSummary
        .otherMethodSignatures()
        .add(new GeneratedMethodSignature(GET_SCOPE_GLOBAL_TEMPLATE,
                                          cd4C.addMethod(ast, GET_SCOPE_GLOBAL_TEMPLATE)));
    generationSummary
        .importStatements()
        .add(cd4C.addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope")));

    return ast;
  }

  /**
   * An empty implementation of the interface.
   * Useful in Typescript, since ts interfaces do not support default methods
   */
  protected ASTCDClass createEmptyImpl(ASTCDCompilationUnit origCD, ASTCDInterface i){
    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDInterfaceUsage(getInterfaceUsageFacade().createCDInterfaceUsage(i.getName()))
        .setName(PREFIX + i.getName()).build();

    for (ASTCDMethod astcdMethod : i.getCDMethodList()) {
      if(astcdMethod.getName().startsWith("maybe") || astcdMethod.getName().equals("getScope")) {
        continue;
      }
      ASTCDMethod element = astcdMethod.deepClone();
      element.setIsDefault(false);
      element.setModifier(PUBLIC.build());
      replaceMethodBody(element, new StringHookPoint(""), ast);
    }

    return ast;
  }

  public Optional<GenerationSummary> getGenerationSummary() {
    return generationSummary;
  }

  public static final class GenerationSummary {

    private final Collection<NotifyMethod> notifyMethodSignatures;
    private final Collection<GeneratedMethodSignature> otherMethodSignatures;
    private final Collection<ASTMCImportStatement> importStatements;

    public GenerationSummary(Collection<NotifyMethod> notifyMethodSignatures,
                             Collection<GeneratedMethodSignature> otherMethodSignatures,
                             Collection<ASTMCImportStatement> importStatements) {
      this.notifyMethodSignatures = notifyMethodSignatures;
      this.otherMethodSignatures = otherMethodSignatures;
      this.importStatements = importStatements;
    }

    void addMethod(ASTCDMethodSignature signature, String template, ASTCDClass changedType) {
      notifyMethodSignatures.add(new NotifyMethod(changedType,
                                                  new GeneratedMethodSignature(template,
                                                                               signature)));
    }

    public Collection<NotifyMethod> notifyMethodSignatures() {
      return notifyMethodSignatures;
    }

    public Collection<GeneratedMethodSignature> otherMethodSignatures() {
      return otherMethodSignatures;
    }

    public Collection<ASTMCImportStatement> importStatements() {
      return importStatements;
    }
  }

  public static final class NotifyMethod {

    private final ASTCDClass changedType;
    private final GeneratedMethodSignature generatedMethodSignature;

    public NotifyMethod(ASTCDClass changedType, GeneratedMethodSignature generatedMethodSignature) {
      this.changedType = changedType;
      this.generatedMethodSignature = generatedMethodSignature;
    }

    public ASTCDClass changedType() {
      return changedType;
    }

    public GeneratedMethodSignature generatedMethodSignature() {
      return generatedMethodSignature;
    }
  }
}
