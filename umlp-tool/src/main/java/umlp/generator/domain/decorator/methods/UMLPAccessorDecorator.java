/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.methods.AbstractMethodDecorator;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cd.codegen.methods.accessor.ListAccessorDecorator;
import de.monticore.cd.codegen.methods.accessor.MandatoryAccessorDecorator;
import de.monticore.cd.codegen.methods.accessor.OptionalAccessorDecorator;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.generator.domain.decorator.GenService;

public class UMLPAccessorDecorator extends AccessorDecorator {

  // map attributes with simple values, see MapAttrAccessorDecorator
  protected final AbstractMethodDecorator mapMethodDecorator;

  /**
   * without map attributes: a map is a plain field (RTE POJOs)
   */
  public UMLPAccessorDecorator(final GlobalExtensionManagement glex, final CDGenService service) {
    super(
        glex,
        new MandatoryAccessorDecorator(glex, service),
        new OptionalAccessorDecorator(glex, service),
        new ListAccessorDecorator(glex, service));
    this.mapMethodDecorator = this.mandatoryMethodDecorator;
  }

  public UMLPAccessorDecorator(final GlobalExtensionManagement glex, final CDGenService service,
                               final ASTCDType ast) {
    super(
        glex,
        new MandatoryAccessorDecorator(glex, service),
        new OptionalAccessorDecorator(glex, service),
        new ListAccessorDecorator(glex, service));
    this.mapMethodDecorator = new MapAttrAccessorDecorator(glex, service, ast);
  }

  @Override
  public void enableTemplates() {
    super.enableTemplates();
    mapMethodDecorator.enableTemplates();
  }

  @Override
  public void disableTemplates() {
    super.disableTemplates();
    mapMethodDecorator.disableTemplates();
  }

  @Override
  protected AbstractMethodDecorator determineMethodDecoratorStrategy(ASTCDAttribute ast) {
    if (GenService.isMap(ast)) {
      return mapMethodDecorator;
    }
    if (GenService.isList(ast)) {
      return listMethodDecorator;
    }
    return mandatoryMethodDecorator;
  }

}
