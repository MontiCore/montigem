package aas.helpers;

import aas.AAS2UMLPModes;
import aas.utils.AssociationFinder;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.types.mcbasictypes._ast.ASTMCObjectType;
import de.monticore.types.mcbasictypes._ast.ASTMCQualifiedType;
import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class AAS2UMLPDecoratorTestHelper {

  public static Stream<Arguments> smcArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 2),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1)
    );
  }

  public static Stream<Arguments> smlArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 4),
        Arguments.of(AAS2UMLPModes.HYBRID, 4),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 3)
    );
  }

  public static Stream<Arguments> mlpArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 2),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1)
    );
  }

  public static Stream<Arguments> fileArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 2),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1)
    );
  }

  public static Stream<Arguments> entityArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 2),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1)
    );
  }

  public static Stream<Arguments> refElemArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 2),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1)
    );
  }

  public static Stream<Arguments> propArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 2),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1)
    );
  }

  public static Stream<Arguments> IdShortCollisionArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 1),
        Arguments.of(AAS2UMLPModes.HYBRID, 4),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 3)
    );
  }

  public static Stream<Arguments> SemanticIdCollisionArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 3),
        Arguments.of(AAS2UMLPModes.HYBRID, 1),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1)
    );
  }

  public static Stream<Arguments> ComplexCollisionArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 2),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 2)
    );
  }

  public static Stream<Arguments> ComplexCollisionMissingSemIdArgs() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 3),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 2)
    );
  }

  public static Stream<Arguments> ListCollision() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 1, List.of(1,1)),
        Arguments.of(AAS2UMLPModes.HYBRID, 1, List.of(1,1)),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1, List.of(1,1))
    );
  }

  public static Stream<Arguments> NestedElementCollisionSML() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 1, List.of(1,2)),
        Arguments.of(AAS2UMLPModes.HYBRID, 2, List.of(1,1)),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 2, List.of(1,1))
    );
  }

  public static Stream<Arguments> CollectionCollision() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 1, List.of(1,1)),
        Arguments.of(AAS2UMLPModes.HYBRID, 1, List.of(1,1)),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 1, List.of(1,1))
    );
  }

  public static Stream<Arguments> NestedElementCollisionSMC() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 1, List.of(1,2)),
        Arguments.of(AAS2UMLPModes.HYBRID, 2, List.of(1,1)),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 2, List.of(1,1))
    );
  }

  public static Stream<Arguments> ParentNoSemId() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 1, 1),
        Arguments.of(AAS2UMLPModes.HYBRID, 1, 1),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 0, 0)
    );
  }

  public static int countExtendingClasses(String extendedClass, ASTCDCompilationUnit cd) {
    int count = 0;
    for (ASTCDClass astClass : cd.getCDDefinition().getCDClassesList()) {
      for (ASTMCObjectType superClass : astClass.getSuperclassList()) {
        if (((ASTMCQualifiedType) superClass).getNameList().getFirst().equals(extendedClass)) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countAssociationsWithRightSide(String rightSide, ASTCDCompilationUnit cd) {
    AssociationFinder finder = new AssociationFinder(rightSide);
    CD4CodeTraverser t = CD4CodeMill.inheritanceTraverser();
    t.add4CDAssociation(finder);
    cd.accept(t);

    int count = 0;
    for (ASTCDAssociation association : finder.getAssociations()) {
      if (association.getRightQualifiedName().getBaseName().equals(rightSide)) {
        count++;
      }
    }

    return count;
  }

  public static int countAssociationsWithLeftSide(String leftSide, ASTCDCompilationUnit cd) {
    AssociationFinder finder = new AssociationFinder(leftSide);
    CD4CodeTraverser t = CD4CodeMill.inheritanceTraverser();
    t.add4CDAssociation(finder);
    cd.accept(t);

    int count = 0;
    for (ASTCDAssociation association : finder.getAssociations()) {
      if (association.getLeftQualifiedName().getBaseName().equals(leftSide)) {
        count++;
      }
    }

    return count;
  }

  public static int countAssociationsWithRightSideRole(String rightSideRole, ASTCDCompilationUnit cd) {
    AssociationFinder finder = new AssociationFinder(rightSideRole);
    CD4CodeTraverser t = CD4CodeMill.inheritanceTraverser();
    t.add4CDAssociation(finder);
    cd.accept(t);

    int count = 0;
    for (ASTCDAssociation association : finder.getAssociations()) {
      if (association.getRight().getCDRole().getName().equals(rightSideRole)) {
        count++;
      }
    }

    return count;
  }

}
