/* (c) https://github.com/MontiCore/monticore */
package umlp;

import de.monticore.cd4code.types3.CD4CodeTypeCheck3;

public class UMLPMill extends UMLPMillTOP {

  /**
   * {@inheritDoc}
   * Additionally inits the TypeCheck; UMLP adds no types or expressions to CD4Code
   */
  public static void init() {
    UMLPMillTOP.init();
    CD4CodeTypeCheck3.init();
  }

  public static void reset() {
    UMLPMillTOP.reset();
    CD4CodeTypeCheck3.reset();
  }

}
