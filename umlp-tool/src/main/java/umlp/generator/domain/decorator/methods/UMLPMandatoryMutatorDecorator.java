/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd.codegen.methods.mutator.MandatoryMutatorDecorator;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.se_rwth.commons.StringTransformations;

import java.util.List;

import static de.monticore.cd.codegen.CD2JavaTemplates.EMPTY_BODY;
import static de.monticore.cd.facade.CDModifier.PUBLIC;

public class UMLPMandatoryMutatorDecorator extends MandatoryMutatorDecorator {

  protected final ASTCDType ast;

  protected final CD4C cd4c = CD4C.getInstance();

  public UMLPMandatoryMutatorDecorator(GlobalExtensionManagement glex, CDGenService service,
                                       ASTCDType ast) {
    super(glex, service);
    this.ast = ast;
  }

  @Override
  public List<ASTCDMethod> decorate(ASTCDAttribute ast) {
    List<ASTCDMethod> methodList = Lists.newArrayList();
    if (!ast.getModifier().isDerived() && !ast.getModifier().isReadonly()) {
      methodList.add(createSetter(ast));
      methodList.add(createNotificationSetter(ast, false));
    }
    return methodList;
  }

  @Override
  protected ASTCDMethod createSetter(ASTCDAttribute ast) {
    ASTCDMethod method = super.createSetter(ast);
    String notificationScope = ast.getModifier().isLocal() ? "LOCAL" : "ALL";
    glex.replaceTemplate(EMPTY_BODY, method, new StringHookPoint("this." + method.getName() + "("
            + StringTransformations.uncapitalize(ast.getName()) + ", NotificationScope." + notificationScope + ");"));
    return method;
  }

  protected ASTCDMethod createNotificationSetter(ASTCDAttribute ast, boolean isGem) {
    ASTCDMethod method = super.createSetter(ast);
    method.addCDParameter(getCDParameterFacade().createParameter("NotificationScope", "scope"));
    glex.replaceTemplate(EMPTY_BODY, method, new TemplateHookPoint(getNotificationSetterTemplate(),
            this.ast.getName(), service.getNativeAttributeName(ast.getName()), new CdUtilsPrinter().printType(ast.getMCType()), isGem));
    return method;
  }

  protected ASTCDMethod createGemSetter(ASTCDAttribute ast) {
    String name = String.format(SET, StringTransformations.capitalize(service.getNativeAttributeName(ast.getName())+"Gem"));
    ASTCDMethod method3 = this.getCDMethodFacade().createMethod(PUBLIC.build(), name, this.getCDParameterFacade().createParameters(ast));
    String notificationScope = ast.getModifier().isLocal() ? "LOCAL" : "ALL";
    glex.replaceTemplate(EMPTY_BODY, method3, new StringHookPoint("this." + method3.getName() + "("
        + StringTransformations.uncapitalize(ast.getName()) + ", NotificationScope." + notificationScope + ");"));
    return method3;
  }

  protected String getNotificationSetterTemplate() {
    return "umlp.methods.NotificationSetter";
  }

}
