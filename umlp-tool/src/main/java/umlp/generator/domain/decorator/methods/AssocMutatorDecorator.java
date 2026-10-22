/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.methods.AbstractMethodDecorator;
import de.monticore.cd.codegen.methods.MutatorDecorator;
import de.monticore.cd.codegen.methods.mutator.OptionalMutatorDecorator;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.generator.domain.decorator.GenService;

public class AssocMutatorDecorator extends MutatorDecorator {

  protected final AbstractMethodDecorator mapMethodDecorator;

  public AssocMutatorDecorator(GlobalExtensionManagement glex, CDGenService service,
                               ASTCDType ast) {
    super(glex, new AssocMandatoryMutatorDecorator(glex, service, ast), new OptionalMutatorDecorator(glex, service), new AssocListMutatorDecorator(glex, service, ast));
    this.mapMethodDecorator = new AssocMapMutatorDecorator(glex, service, ast);
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
