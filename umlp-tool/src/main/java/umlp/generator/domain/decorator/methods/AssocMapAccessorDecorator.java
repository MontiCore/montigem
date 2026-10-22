/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd.codegen.methods.accessor.MandatoryAccessorDecorator;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.types.mccollectiontypes._ast.ASTMCGenericType;
import umlp.generator.domain.decorator.DomainTypeDecorator;

import java.util.List;

/**
 * Accessors of a GemMap of an attribute qualified association, see GenService#isMap
 */
public class AssocMapAccessorDecorator extends MandatoryAccessorDecorator {

  protected final ASTCDType ast;

  protected final CD4C cd4c = CD4C.getInstance();

  public AssocMapAccessorDecorator(GlobalExtensionManagement glex, CDGenService service,
                                   ASTCDType ast) {
    super(glex, service);
    this.ast = ast;
  }

  @Override
  public List<ASTCDMethod> decorate(ASTCDAttribute ast) {
    List<ASTCDMethod> methodList = Lists.newArrayList();
    String name = service.getNativeAttributeName(ast.getName());
    String keyType = getTypeArgument(ast, 0);
    String targetName = getTypeArgument(ast, 1);
    String mapType = new CdUtilsPrinter().printType(ast.getMCType());
    boolean isOne = ast.getModifier().isPresentStereotype()
        && ast.getModifier().getStereotype().contains(DomainTypeDecorator.STEREOTYPE_QUALIFIED_ONE);

    methodList.add(createMethod("umlp.methods.gemmap.InternalGetGemMap", name, mapType));
    methodList.add(createMethod("umlp.methods.gemmap.GetByKey", name, keyType, targetName, isOne));
    methodList.add(createMethod("umlp.methods.gemmap.GetMap", name, keyType, targetName));
    methodList.add(createMethod("umlp.methods.gemmap.ContainsKey", name, keyType));
    methodList.add(createMethod("umlp.methods.gemmap.Contains", name, targetName));
    methodList.add(createMethod("umlp.methods.gemmap.Size", name));
    return methodList;
  }

  protected ASTCDMethod createMethod(String template, Object... args) {
    return (ASTCDMethod) cd4c.createMethod(this.ast, template, args).get();
  }

  protected static String getTypeArgument(ASTCDAttribute ast, int idx) {
    return new CdUtilsPrinter().printType(((ASTMCGenericType) ast.getMCType()).getMCTypeArgument(idx).getMCTypeOpt().get());
  }
}
