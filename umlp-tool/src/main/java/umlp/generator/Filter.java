package umlp.generator;

import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.umlmodifier._ast.ASTModifier;
import umlp.generator.domain.decorator.GenService;

import java.util.function.Predicate;

/**
 * Filters which can be used with {@link AbstractDecorator#getDomainClasses(ASTCDCompilationUnit, Predicate)}
 */
public interface Filter {
  Predicate<Wrapper> DERIVED = c -> c.hasSymbol() ? c.getSymbol().isIsDerived() : c.getModifier()
      .isDerived(); //TODO is this really the intended behaviour if a symbol is not present?
  Predicate<Wrapper> ABSTRACT = c -> c.getModifier().isAbstract();
  Predicate<Wrapper> SINGLETON = c -> GenService.hasStereotype(c.getModifier(), "singleton");
  Predicate<Wrapper> POJO = c -> GenService.hasStereotype(c.getModifier(), "pojo");

  //bundles the properties of
  interface Wrapper {
    /**
     * Checks whether {@link #getSymbol()} will be successful or result in an error.
     *
     * @return {@code true} if {@link #getSymbol()} will be successful, else {@code false}
     * @see #getSymbol()
     */
    boolean hasSymbol();

    /**
     * Gets the symbol of the wrapped type. May throw an error if {@link #hasSymbol()} returns {@code false}!
     *
     * @return The {@link CDTypeSymbol}, if present
     * @see #hasSymbol()
     */
    CDTypeSymbol getSymbol();

    ASTModifier getModifier();

    static Wrapper wrap(ASTCDClass type) {
      return new Wrapper() {
        @Override
        public boolean hasSymbol() {
          return type.isPresentSymbol();
        }

        @Override
        public CDTypeSymbol getSymbol() {
          return type.getSymbol();
        }

        @Override
        public ASTModifier getModifier() {
          return type.getModifier();
        }
      };
    }

    static Wrapper wrap(ASTCDInterface type) {
      return new Wrapper() {
        @Override
        public boolean hasSymbol() {
          return type.isPresentSymbol();
        }

        @Override
        public CDTypeSymbol getSymbol() {
          return type.getSymbol();
        }

        @Override
        public ASTModifier getModifier() {
          return type.getModifier();
        }
      };
    }
  }
}
