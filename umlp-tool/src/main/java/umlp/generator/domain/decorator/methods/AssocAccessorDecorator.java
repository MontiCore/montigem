/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.CDGenerator;
import de.monticore.cd.codegen.methods.AbstractMethodDecorator;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cd.codegen.methods.accessor.OptionalAccessorDecorator;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.generator.domain.decorator.GenService;

public class AssocAccessorDecorator extends AccessorDecorator {

  protected final AbstractMethodDecorator mapMethodDecorator;

  public AssocAccessorDecorator(GlobalExtensionManagement glex, CDGenService service,
                                ASTCDType ast) {
    super(glex, new AssocMandatoryAccessorDecorator(glex, service),
            new OptionalAccessorDecorator(glex, service),
            new AssocListAccessorDecorator(glex, service, ast));
    this.mapMethodDecorator = new AssocMapAccessorDecorator(glex, service, ast);
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
    if (GenService.isGemMap(ast)) {
      return mapMethodDecorator;
    }
    if (GenService.isGemList(ast)) {
      return listMethodDecorator;
    }
    return mandatoryMethodDecorator;
  }
}
