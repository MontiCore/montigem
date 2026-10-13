/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cd4codebasis._ast.ASTCDParameterTOP;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.mccollectiontypes._ast.ASTMCGenericType;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.StringTransformations;
import umlp.generator.domain.decorator.DomainTypeDecorator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.monticore.cd.codegen.CD2JavaTemplates.EMPTY_BODY;

public class AssocListMutatorDecorator extends UMLPMandatoryMutatorDecorator {

  public AssocListMutatorDecorator(GlobalExtensionManagement glex, CDGenService service,
                                   ASTCDType ast) {
    super(glex, service, ast);
  }

  @Override
  public List<ASTCDMethod> decorate(ASTCDAttribute ast) {
    List<ASTCDMethod> methodList = Lists.newArrayList();
    if(ast.getModifier().isDerived()){
      return methodList;
    }
    String targetName = (new CdUtilsPrinter()).printType(((ASTMCGenericType) ast.getMCType()).getMCTypeArgument(0).getMCTypeOpt().get());
    String sourceName = this.ast.getName();
    String otherSide = ast.getModifier().isPresentStereotype() && ast.getModifier().getStereotype().contains(DomainTypeDecorator.STEREOTYPE_OTHERSIDE)?
        ast.getModifier().getStereotype().getValue(DomainTypeDecorator.STEREOTYPE_OTHERSIDE):
        "";
    String managerName = ast.getModifier().isPresentStereotype()&& ast.getModifier().getStereotype().contains(DomainTypeDecorator.STEREOTYPE_MANAGER)?
        ast.getModifier().getStereotype().getValue(DomainTypeDecorator.STEREOTYPE_MANAGER):
        "";
    boolean isList = ast.getModifier().isPresentStereotype() && ast.getModifier().getStereotype().contains(DomainTypeDecorator.STEREOTYPE_OTHERSIDE_ISLIST);

    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.Add", service.getNativeAttributeName(ast.getName()), targetName).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.AddId", service.getNativeAttributeName(ast.getName()), targetName).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.AddAll", service.getNativeAttributeName(ast.getName()), targetName).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.AddAllId", service.getNativeAttributeName(ast.getName()), targetName).get());
    String getter = "";
    if (!otherSide.isEmpty()) {
       getter = (isList ? "add" : "set") + StringTransformations.capitalize(otherSide);
    }
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.InternalAdd", service.getNativeAttributeName(ast.getName()), getter, targetName, sourceName, managerName).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.InternalAddId", service.getNativeAttributeName(ast.getName()), targetName, sourceName).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.InternalSet", service.getNativeAttributeName(ast.getName()), targetName, sourceName).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.Remove", service.getNativeAttributeName(ast.getName()), targetName, sourceName).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.Remove_Id", service.getNativeAttributeName(ast.getName()), targetName, sourceName).get());
    String remove = "";
    if (!otherSide.isEmpty()) {
      remove = (isList ? "remove" : "set") + StringTransformations.capitalize(otherSide);
    }

    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.InternalRemove", service.getNativeAttributeName(ast.getName()), remove, targetName, sourceName, managerName).get());
    List<ASTCDMethod> notifyMethods = createNotifyMethods(methodList, ast.getModifier().isLocal());

    methodList.addAll(notifyMethods);
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.RemoveByIdSure", service.getNativeAttributeName(ast.getName())).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.RemoveByIdSureNoScope", service.getNativeAttributeName(ast.getName()), ast.getModifier().isLocal()?"LOCAL":"ALL").get());

    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.Clear", service.getNativeAttributeName(ast.getName()), targetName).get());
    methodList.add((ASTCDMethod) cd4c.createMethod(this.ast, "umlp.methods.assocs.RetainAll", service.getNativeAttributeName(ast.getName()), targetName).get());

    cd4c.addImport(this.ast, "com.google.common.collect.Sets");
    cd4c.addImport(this.ast, "java.util.Map");
    cd4c.addImport(this.ast, "java.util.HashMap");

    return methodList;
  }

  protected List<ASTCDMethod> createNotifyMethods(List<ASTCDMethod> methodList, boolean isLocal) {
    ArrayList<ASTCDMethod> retList = Lists.newArrayList();
    for (ASTCDMethod method : methodList) {
      ASTCDMethod newMethod = method.deepClone();
      newMethod.removeCDParameter(newMethod.sizeCDParameters() - 1);
      String body = (newMethod.getMCReturnType().isPresentMCVoidType()) ? "" : "return ";
      String notificationScope = isLocal ? "LOCAL" : "ALL";
      body += method.getName() + "(" +
              Joiners.COMMA.join(newMethod.streamCDParameters().map(ASTCDParameterTOP::getName).collect(Collectors.toList())) +
              ", NotificationScope." + notificationScope + "); /* AssocListMutatorDecorator#createNotifyMethods */";
      glex.replaceTemplate(EMPTY_BODY, newMethod, new StringHookPoint(body));
      retList.add(newMethod);
    }
    return retList;
  }
}
