/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.methods.accessor.MandatoryAccessorDecorator;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.generator.domain.decorator.GenService;

import java.util.List;

/**
 * Accessors of a map attribute with simple values ({@code Map<String, Integer> scores}, see GenService#isMap), named
 * like those of the GemMap of a qualified association (AssocMapAccessorDecorator). Like get<Attr>List() of list
 * attributes, get<Attr>Map() returns the map itself: changes through it are not synchronized (methods/mapattrs/GetMap)
 */
public class MapAttrAccessorDecorator extends MandatoryAccessorDecorator {

  protected final ASTCDType ast;

  protected final CD4C cd4c = CD4C.getInstance();

  public MapAttrAccessorDecorator(GlobalExtensionManagement glex, CDGenService service, ASTCDType ast) {
    super(glex, service);
    this.ast = ast;
  }

  @Override
  public List<ASTCDMethod> decorate(ASTCDAttribute ast) {
    List<ASTCDMethod> methodList = Lists.newArrayList();
    String name = service.getNativeAttributeName(ast.getName());
    String keyType = GenService.getMapAttributeType(ast, 0);
    String valueType = GenService.getMapAttributeType(ast, 1);

    methodList.add(createMethod("umlp.methods.mapattrs.GetByKey", name, keyType, valueType));
    methodList.add(createMethod("umlp.methods.mapattrs.GetMap", name, keyType, valueType));
    methodList.add(createMethod("umlp.methods.mapattrs.ContainsKey", name, keyType));
    methodList.add(createMethod("umlp.methods.mapattrs.Size", name));
    cd4c.addImport(this.ast, "java.util.Map");
    return methodList;
  }

  protected ASTCDMethod createMethod(String template, Object... args) {
    return (ASTCDMethod) cd4c.createMethod(this.ast, template, args).get();
  }
}
