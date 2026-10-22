/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.generator.domain.decorator.GenService;

import java.util.List;

/**
 * Mutators of a GemMap of an attribute qualified association, see GenService#isMap.
 * The key of a value is derived from its qualifying attribute, so values are put without a key.
 */
public class AssocMapMutatorDecorator extends AssocListMutatorDecorator {

  public AssocMapMutatorDecorator(GlobalExtensionManagement glex, CDGenService service,
                                  ASTCDType ast) {
    super(glex, service, ast);
  }

  @Override
  public List<ASTCDMethod> decorate(ASTCDAttribute ast) {
    List<ASTCDMethod> methodList = Lists.newArrayList();
    if (ast.getModifier().isDerived()) {
      return methodList;
    }
    String name = service.getNativeAttributeName(ast.getName());
    String keyType = AssocMapAccessorDecorator.getTypeArgument(ast, 0);
    String targetName = AssocMapAccessorDecorator.getTypeArgument(ast, 1);
    String sourceName = this.ast.getName();
    boolean isEntry = GenService.isGemEntryMap(ast);

    methodList.add(createMethod("umlp.methods.gemmap.Put", name, targetName, sourceName, isEntry));
    methodList.add(createMethod("umlp.methods.gemmap.Remove", name, keyType, isEntry));
    // put/remove without scope
    methodList.addAll(createNotifyMethods(methodList, ast.getModifier().isLocal()));

    methodList.add(createMethod("umlp.methods.gemmap.InternalRemoveId", name, sourceName));
    methodList.add(createMethod("umlp.methods.gemmap.RemoveByIdSure", name));
    methodList.add(createMethod("umlp.methods.assocs.RemoveByIdSureNoScope", name, ast.getModifier().isLocal() ? "LOCAL" : "ALL"));
    return methodList;
  }

  protected ASTCDMethod createMethod(String template, Object... args) {
    return (ASTCDMethod) cd4c.createMethod(this.ast, template, args).get();
  }
}
