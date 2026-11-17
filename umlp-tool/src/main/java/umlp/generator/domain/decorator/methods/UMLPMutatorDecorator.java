/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd.codegen.methods.AbstractMethodDecorator;
import de.monticore.cd.codegen.methods.MutatorDecorator;
import de.monticore.cd.codegen.methods.mutator.ListMutatorDecorator;
import de.monticore.cd.codegen.methods.mutator.MandatoryMutatorDecorator;
import de.monticore.cd.codegen.methods.mutator.OptionalMutatorDecorator;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.generator.domain.decorator.GenService;

public class UMLPMutatorDecorator extends MutatorDecorator {

  // map attributes with simple values, see MapAttrMutatorDecorator
  protected final AbstractMethodDecorator mapMethodDecorator;

  /**
   * without map attributes: a map is a plain field (RTE POJOs)
   */
  public UMLPMutatorDecorator(
      final GlobalExtensionManagement glex,
      final AbstractMethodDecorator mandatoryMethodDecorator,
      final AbstractMethodDecorator optionalMethodDecorator,
      final AbstractMethodDecorator listMethodDecorator) {
    this(glex, mandatoryMethodDecorator, optionalMethodDecorator, listMethodDecorator, mandatoryMethodDecorator);
  }

  public UMLPMutatorDecorator(
      final GlobalExtensionManagement glex,
      final AbstractMethodDecorator mandatoryMethodDecorator,
      final AbstractMethodDecorator optionalMethodDecorator,
      final AbstractMethodDecorator listMethodDecorator,
      final AbstractMethodDecorator mapMethodDecorator) {
    super(glex, mandatoryMethodDecorator, optionalMethodDecorator, listMethodDecorator);
    this.mapMethodDecorator = mapMethodDecorator;
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
