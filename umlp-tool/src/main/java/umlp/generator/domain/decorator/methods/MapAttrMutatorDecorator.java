/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.methods.mutator.MandatoryMutatorDecorator;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.TemplateHookPoint;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.PathUtil;

import java.util.List;

import static de.monticore.cd.codegen.CD2JavaTemplates.EMPTY_BODY;

/**
 * Mutators of a map attribute with simple values ({@code Map<String, Integer> scores}, see GenService#isMap): put and
 * remove of an entry, with and without notification scope like the add and remove of list attributes
 * (UMLPListMutatorDecorator); there is no setter of the whole map
 */
public class MapAttrMutatorDecorator extends MandatoryMutatorDecorator {

  protected final ASTCDType ast;

  protected final CD4C cd4c = CD4C.getInstance();

  public MapAttrMutatorDecorator(GlobalExtensionManagement glex, CDGenService service, ASTCDType ast) {
    super(glex, service);
    this.ast = ast;
  }

  @Override
  public List<ASTCDMethod> decorate(ASTCDAttribute ast) {
    List<ASTCDMethod> methodList = Lists.newArrayList();
    if (ast.getModifier().isDerived() || ast.getModifier().isReadonly()) {
      return methodList;
    }
    String name = service.getNativeAttributeName(ast.getName());
    String keyType = GenService.getMapAttributeType(ast, 0);
    String valueType = GenService.getMapAttributeType(ast, 1);
    String sourceName = this.ast.getName();
    boolean isLongKey = GenService.isLongType(keyType);

    methodList.add(createMethod("umlp.methods.mapattrs.Put", sourceName, name, keyType, valueType, isLongKey,
        GenService.isNanoTimeType(keyType)));
    methodList.add(createMethod("umlp.methods.mapattrs.Remove", sourceName, name, keyType, valueType));
    // put/remove without scope
    methodList.addAll(createDelegatesWithoutScope(methodList));

    cd4c.addImport(this.ast, "java.util.LinkedHashMap");
    cd4c.addImport(this.ast, "java.util.Objects");
    if (isLongKey) {
      cd4c.addImport(this.ast, PathUtil.getBackendRTECommonPath("GemValueMap"));
    }
    return methodList;
  }

  /**
   * @return the methods without their last parameter, the notification scope, delegating with NotificationScope.ALL
   */
  protected List<ASTCDMethod> createDelegatesWithoutScope(List<ASTCDMethod> methodList) {
    List<ASTCDMethod> delegates = Lists.newArrayList();
    for (ASTCDMethod method : methodList) {
      ASTCDMethod delegate = method.deepClone();
      delegate.removeCDParameter(delegate.sizeCDParameters() - 1);
      glex.replaceTemplate(EMPTY_BODY, delegate, new TemplateHookPoint(getDelegateWithScopeTemplate(), delegate,
          delegate.getMCReturnType().isPresentMCType()));
      delegates.add(delegate);
    }
    return delegates;
  }

  protected String getDelegateWithScopeTemplate() {
    return "umlp.methods.DelegateWithScope";
  }

  protected ASTCDMethod createMethod(String template, Object... args) {
    return (ASTCDMethod) cd4c.createMethod(this.ast, template, args).get();
  }
}
