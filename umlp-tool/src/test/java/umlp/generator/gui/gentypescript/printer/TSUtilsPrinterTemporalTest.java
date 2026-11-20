/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.gentypescript.printer;

import de.monticore.types.MCTypeFacade;
import de.monticore.types.mcbasictypes.MCBasicTypesMill;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import umlp.UMLPMill;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** java.time types print as Temporal types in a namespace, also as type arguments and in imports */
public class TSUtilsPrinterTemporalTest {

  @BeforeAll
  public static void init() {
    UMLPMill.init();
  }

  /** an EventEmitter of a list of dates (a ref List<LocalDate> of a GUI component) */
  @Test
  public void testNamespacedTypeArgument() {
    MCTypeFacade facade = MCTypeFacade.getInstance();
    assertEquals("EventEmitter<Array<Temporal.PlainDate>>",
        new TSUtilsPrinter().printType(facade.createBasicGenericTypeOf("EventEmitter", "Array<Temporal.PlainDate>")));
    assertEquals("EventEmitter<Temporal.PlainDate>",
        new TSUtilsPrinter().printType(facade.createBasicGenericTypeOf("EventEmitter", "Temporal.PlainDate")));
  }

  /** an import of a java.time type imports the namespace Temporal */
  @Test
  public void testImportOfTemporalType() {
    ASTMCImportStatement importStatement = MCBasicTypesMill.mCImportStatementBuilder()
        .setMCQualifiedName(MCBasicTypesMill.mCQualifiedNameBuilder().setPartsList(List.of("java", "time", "LocalDate"))
            .build())
        .build();
    assertEquals("import { Temporal } from '@umlp/commonj2ts';",
        new TSUtilsPrinter().printImportList(List.of(importStatement)).trim());
  }
}
