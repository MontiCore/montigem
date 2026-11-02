package aas;

import aas.utils.AssociationFinder;
import aas.utils.ClassFinder;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEntity;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultFile;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultMultiLanguageProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReferenceElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementList;
import aas.decorator.AAS2UMLPDecorator;

import static aas.decorator.utils.QualifierTypes.C_ONE_QUALIFIER;
import static aas.decorator.utils.QualifierTypes.C_ONE_TO_MANY_QUALIFIER;
import static aas.decorator.utils.QualifierTypes.C_ZERO_TO_MANY_QUALIFIER;
import static aas.decorator.utils.QualifierTypes.C_ZERO_TO_ONE_QUALIFIER;
import static aas.helpers.AAS2UMLPDecoratorTestHelper.countAssociationsWithLeftSide;
import static aas.helpers.AAS2UMLPDecoratorTestHelper.countAssociationsWithRightSide;
import static aas.helpers.AAS2UMLPDecoratorTestHelper.countAssociationsWithRightSideRole;
import static aas.helpers.AAS2UMLPDecoratorTestHelper.countExtendingClasses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class AAS2UMLPDecoratorTest extends AbstractTest {

  private GlobalExtensionManagement glex;
  private ASTCDCompilationUnit cd;

  @Override
  public void _setup() {
    glex = new GlobalExtensionManagement();
    Map<String, String> nameTransformations = new HashMap<>();
    glex.defineGlobalVar(AAS2UMLPGenerator.NAMETRANSFORMATIONS, nameTransformations);
    Map<String, String> semanticIdToClass = new HashMap<>();
    glex.defineGlobalVar(AAS2UMLPGenerator.SEMANTICIDTOCLASS, semanticIdToClass);

    ASTCDDefinition cdDef = CD4CodeMill.cDDefinitionBuilder()
        .setName("test")
        .setModifier(CDModifier.PUBLIC.build())
        .build();

    cd = CD4CodeMill.cDCompilationUnitBuilder()
        .setCDDefinition(cdDef)
        .build();
  }

  @ParameterizedTest
  @EnumSource(AAS2UMLPModes.class)
  void testSubmodel(AAS2UMLPModes mode) {

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder().idShort("sub1").id("sub1Id").build();
    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder().idShort("sub2").id("sub2Id").build();

    applyAAS2UMLPDecorator(mode,List.of(submodel1, submodel2));

    assertEquals(2, countExtendingClasses("Submodel", cd));
  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#smcArgs")
  void testSubmodelElementCollection(AAS2UMLPModes mode, int expectedCount) {

    DefaultSubmodelElementCollection smcWithSemId = new DefaultSubmodelElementCollection.Builder()
        .idShort("smcWithSemId")
        .semanticId(semanticId("semId"))
        .build();

    DefaultSubmodelElementCollection smcWithoutSemId = new DefaultSubmodelElementCollection.Builder()
        .idShort("smcWithoutSemId")
        .build();

    DefaultSubmodel submodel = new DefaultSubmodel.Builder().idShort("sub1")
        .submodelElements(List.of(smcWithoutSemId, smcWithSemId)).id("sub1id").build();

    applyAAS2UMLPDecorator(mode,submodel);

    assertEquals(expectedCount, countExtendingClasses("SubmodelElementCollection", cd));
  }



  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#smlArgs")
  void testSubmodelElementList(AAS2UMLPModes mode, int expectedCount) {

    DefaultProperty prop = new DefaultProperty.Builder()
        .valueType(DataTypeDefXsd.INT)
        .build();

    DefaultSubmodelElementList listWithSemanticId =
        new DefaultSubmodelElementList.Builder()
            .idShort("ListWithSemanticId")
            .value(prop)
            .semanticId(semanticId("https://example.com/semantic/ListWithSemanticId"))
            .build();

    DefaultSubmodelElementList listWithoutSemanticId =
        new DefaultSubmodelElementList.Builder()
            .idShort("ListWithoutSemanticId")
            .build();

    DefaultSubmodelElementList nestedList =
        new DefaultSubmodelElementList.Builder()
            .idShort("NestedList")
            .semanticId(semanticId("https://example.com/semantic/NestedList"))
            .build();

    DefaultSubmodelElementList outerList =
        new DefaultSubmodelElementList.Builder()
            .idShort("OuterList")
            .semanticId(semanticId("https://example.com/semantic/OuterList"))
            .value(nestedList)
            .build();

    DefaultSubmodel submodel =
        new DefaultSubmodel.Builder()
            .idShort("ExampleSubmodel")
            .id("https://example.com/submodels/example/1")
            .submodelElements(listWithSemanticId)
            .submodelElements(listWithoutSemanticId)
            .submodelElements(outerList)
            .id("sub1id")
            .build();

    applyAAS2UMLPDecorator(mode,submodel);

    assertEquals(expectedCount, countExtendingClasses("SubmodelElementList", cd));

  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#mlpArgs")
  void testMultiLanguageProperty(AAS2UMLPModes mode, int expectedCount) {

    DefaultMultiLanguageProperty mlpWithSemId = new DefaultMultiLanguageProperty.Builder()
        .idShort("mlpWithSemId")
        .semanticId(semanticId("semId"))
        .build();

    DefaultMultiLanguageProperty mlpWithoutSemId = new DefaultMultiLanguageProperty.Builder()
        .idShort("mlpWithoutSemId")
        .build();

    DefaultSubmodel submodel = new DefaultSubmodel.Builder().idShort("sub1")
        .submodelElements(List.of(mlpWithoutSemId, mlpWithSemId)).id("sub1id").build();

    applyAAS2UMLPDecorator(mode,submodel);

    assertEquals(expectedCount, countAssociationsWithRightSide("MultiLanguageProperty", cd));

  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#fileArgs")
  void testFile(AAS2UMLPModes mode, int expectedCount) {

    DefaultFile fileWithSemId = new DefaultFile.Builder()
        .idShort("fileWithSemId")
        .semanticId(semanticId("semId"))
        .build();

    DefaultFile fileWithoutSemId = new DefaultFile.Builder()
        .idShort("fileWithoutSemId")
        .build();

    DefaultSubmodel submodel = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(fileWithoutSemId, fileWithSemId))
        .build();

    applyAAS2UMLPDecorator(mode,submodel);

    assertEquals(expectedCount,countAssociationsWithRightSide("File", cd));
  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#entityArgs")
  void testEntity(AAS2UMLPModes mode, int expectedCount) {

    DefaultEntity entityWithSemId = new DefaultEntity.Builder()
        .idShort("entWithSemId")
        .semanticId(semanticId("semId"))
        .build();

    DefaultEntity entityWithoutSemId = new DefaultEntity.Builder()
        .idShort("entWithoutSemId")
        .build();

    DefaultSubmodel submodel = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(entityWithoutSemId, entityWithSemId))
        .build();

    applyAAS2UMLPDecorator(mode,submodel);

    assertEquals(expectedCount,countAssociationsWithRightSide("AASEntity", cd));
  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#refElemArgs")
  void testReferenceElement(AAS2UMLPModes mode, int expectedCount) {

    DefaultReferenceElement referenceElementWithSemId = new DefaultReferenceElement.Builder()
        .idShort("refEWithSemId")
        .semanticId(semanticId("semId"))
        .build();

    DefaultReferenceElement referenceElementWithoutSemId = new DefaultReferenceElement.Builder()
        .idShort("refEWithoutSemId")
        .build();

    DefaultSubmodel submodel = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(referenceElementWithoutSemId,referenceElementWithSemId))
        .build();

    applyAAS2UMLPDecorator(mode,submodel);

    assertEquals(expectedCount,countAssociationsWithRightSide("ReferenceElement", cd));
  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#propArgs")
  void testProperty(AAS2UMLPModes mode, int expectedCount) {

    DefaultProperty propWithSemId = new DefaultProperty.Builder()
        .idShort("propWithSemId")
        .valueType(DataTypeDefXsd.STRING)
        .semanticId(semanticId("semId"))
        .build();

    DefaultProperty propWithoutSemId = new DefaultProperty.Builder()
        .idShort("propWithoutSemId")
        .valueType(DataTypeDefXsd.INT)
        .build();

    DefaultSubmodel submodel = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(propWithoutSemId,propWithSemId))
        .build();

    applyAAS2UMLPDecorator(mode,submodel);

    assertEquals(expectedCount,countExtendingClasses("Property", cd));

    CD4CodeTraverser t = CD4CodeMill.inheritanceTraverser();
    ClassFinder finder = new ClassFinder("PropWithSemId");
    t.add4CDBasis(finder);
    cd.accept(t);

    assertNotNull(finder.getFoundClass());

    ASTCDClass foundClass = ((ASTCDClass) finder.getFoundClass());
    assertEquals("Optional<String>", foundClass.getCDAttributeList().getFirst().getMCType().printType());
  }

  @ParameterizedTest
  @EnumSource(AAS2UMLPModes.class)
  void testUniqueElements(AAS2UMLPModes mode) {
    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultProperty prop3 = new DefaultProperty.Builder()
        .idShort("prop3")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId3"))
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(prop1, prop2, prop3))
        .build();
    applyAAS2UMLPDecorator(mode, submodel1);

    assertEquals(3,countExtendingClasses("Property", cd), "Incorrect amount of classes extending Property");

  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#IdShortCollisionArgs")
  void testIdShortCollision(AAS2UMLPModes mode, int expectedCountClasses) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .build();

    DefaultProperty prop3 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultProperty prop4 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId3"))
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(prop1, prop2, prop3))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub3id")
        .submodelElements(List.of(prop4))
        .build();

    applyAAS2UMLPDecorator(mode, List.of(submodel1, submodel2));

    assertEquals(expectedCountClasses,countExtendingClasses("Property", cd), "Incorrect amount of classes extending Property");

  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#SemanticIdCollisionArgs")
  void testSemanticIdCollision(AAS2UMLPModes mode, int expectedCountClasses) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId"))
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId"))
        .build();

    DefaultProperty prop3 = new DefaultProperty.Builder()
        .idShort("prop3")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId"))
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(prop3))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub3id")
        .submodelElements(List.of(prop1, prop2))
        .build();

    applyAAS2UMLPDecorator(mode, List.of(submodel1, submodel2));

    assertEquals(expectedCountClasses,countExtendingClasses("Property", cd), "Incorrect amount of classes extending Property");

  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#ComplexCollisionArgs")
  void testComplexCollision(AAS2UMLPModes mode, int expectedCountClasses) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultProperty prop3 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(prop1, prop2, prop3))
        .build();

    applyAAS2UMLPDecorator(mode, submodel1);

    assertEquals(expectedCountClasses,countExtendingClasses("Property", cd), "Incorrect amount of classes extending Property");

  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#ComplexCollisionMissingSemIdArgs")
  void testComplexCollisionMissingSemId(AAS2UMLPModes mode, int expectedCountClasses) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultProperty prop3 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultProperty prop4 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(prop1, prop2, prop3, prop4))
        .build();

    applyAAS2UMLPDecorator(mode, submodel1);

    assertEquals(expectedCountClasses,countExtendingClasses("Property", cd), "Incorrect amount of classes extending Property");

  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#ListCollision")
  void testListCollision(AAS2UMLPModes mode, int expectedCountClasses, List<Integer> expectedCountAssociations) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementList sml1 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId1"))
        .value(prop1)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementList sml2 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId1"))
        .value(prop2)
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(sml1))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub2id")
        .submodelElements(List.of(sml2))
        .build();

    applyAAS2UMLPDecorator(mode, List.of(submodel1, submodel2));
    System.out.println(CD4CodeMill.prettyPrint(cd,true));
    assertEquals(expectedCountClasses,countExtendingClasses("SubmodelElementList", cd), "Incorrect amount of classes extending SubmodelElementList");
    assertEquals(expectedCountAssociations.get(0), countAssociationsWithRightSide("Prop1",cd ));
    assertEquals(expectedCountAssociations.get(1), countAssociationsWithLeftSide("Sml1",cd ));
  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#NestedElementCollisionSML")
  void testNestedElementCollisionSML(AAS2UMLPModes mode, int expectedCountClasses, List<Integer> expectedCountAssociations) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementList sml1 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId1"))
        .value(prop1)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultSubmodelElementList sml2 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId2"))
        .value(prop2)
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(sml1))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub2id")
        .submodelElements(List.of(sml2))
        .build();

    applyAAS2UMLPDecorator(mode, List.of(submodel1, submodel2));

    assertEquals(expectedCountClasses,countExtendingClasses("SubmodelElementList", cd), "Incorrect amount of classes extending SubmodelElementList");
    assertEquals(expectedCountAssociations.get(0), countAssociationsWithRightSide("Prop2", cd));
    assertEquals(expectedCountAssociations.get(1), countAssociationsWithLeftSide("Sml1", cd));
  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#CollectionCollision")
  void testCollectionCollision(AAS2UMLPModes mode, int expectedCountClasses, List<Integer> expectedCountAssociations) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementCollection smc1 = new DefaultSubmodelElementCollection.Builder()
        .idShort("smc1")
        .semanticId(semanticId("smcId1"))
        .value(prop1)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementCollection smc2 = new DefaultSubmodelElementCollection.Builder()
        .idShort("smc1")
        .semanticId(semanticId("smcId1"))
        .value(prop2)
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(smc1))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub2id")
        .submodelElements(List.of(smc2))
        .build();

    applyAAS2UMLPDecorator(mode, List.of(submodel1, submodel2));
    assertEquals(expectedCountClasses,countExtendingClasses("SubmodelElementCollection", cd), "Incorrect amount of classes extending SubmodelElementCollection");
    assertEquals(expectedCountAssociations.get(0), countAssociationsWithRightSide("Prop1",cd ));
    assertEquals(expectedCountAssociations.get(1), countAssociationsWithLeftSide("Smc1",cd ));
  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#NestedElementCollisionSMC")
  void testNestedElementCollisionSMC(AAS2UMLPModes mode, int expectedCountClasses, List<Integer> expectedCountAssociations) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementCollection smc1 = new DefaultSubmodelElementCollection.Builder()
        .idShort("smc1")
        .semanticId(semanticId("smcId1"))
        .value(prop1)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultSubmodelElementCollection smc2 = new DefaultSubmodelElementCollection.Builder()
        .idShort("smc1")
        .semanticId(semanticId("smcId2"))
        .value(prop2)
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(smc1))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub2id")
        .submodelElements(List.of(smc2))
        .build();

    applyAAS2UMLPDecorator(mode, List.of(submodel1, submodel2));

    assertEquals(expectedCountClasses,countExtendingClasses("SubmodelElementCollection", cd), "Incorrect amount of classes extending SubmodelElementCollection");
    assertEquals(expectedCountAssociations.get(0), countAssociationsWithRightSide("Prop2", cd));
    assertEquals(expectedCountAssociations.get(1), countAssociationsWithLeftSide("Smc1", cd));
  }

  @ParameterizedTest
  @EnumSource(AAS2UMLPModes.class)
  void testNestedNonComplexElementCollision(AAS2UMLPModes mode) {
    DefaultFile file1  = new DefaultFile.Builder()
        .idShort("arbitrary")
        .semanticId(semanticId("semId"))
        .build();

    DefaultFile file2  = new DefaultFile.Builder()
        .idShort("arbitrary")
        .semanticId(semanticId("semId"))
        .build();

    DefaultSubmodelElementCollection smc = new DefaultSubmodelElementCollection.Builder()
        .idShort("smc1")
        .semanticId(semanticId("smcSemId"))
        .value(file2)
        .build();

    DefaultSubmodel submodel = new DefaultSubmodel.Builder()
        .idShort("sub")
        .id("subid")
        .submodelElements(List.of(smc,file1))
        .build();

    applyAAS2UMLPDecorator(mode, submodel);

    System.out.println(CD4CodeMill.prettyPrint(cd, true));

    assertEquals(2, countAssociationsWithRightSideRole("arbitrary", cd));
  }

  @ParameterizedTest
  @MethodSource("aas.helpers.AAS2UMLPDecoratorTestHelper#ParentNoSemId")
  void testParentNoSemId(AAS2UMLPModes mode, int expectedCountCollection, int expectedCountList) {

    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementCollection smc = new DefaultSubmodelElementCollection.Builder()
        .idShort("smc1")
        .value(prop1)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultSubmodelElementList sml = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .value(prop2)
        .build();

    DefaultSubmodel submodel = new DefaultSubmodel.Builder()
        .idShort("sub")
        .id("subid")
        .submodelElements(List.of(smc,sml))
        .build();

    applyAAS2UMLPDecorator(mode, submodel);

    System.out.println(CD4CodeMill.prettyPrint(cd,true));

    assertEquals(expectedCountCollection,countExtendingClasses("SubmodelElementCollection", cd), "Incorrect amount of classes extending SubmodelElementCollection");
    assertEquals(expectedCountList,countExtendingClasses("SubmodelElementList", cd), "Incorrect amount of classes extending SubmodelElementList");

  }

  @ParameterizedTest
  @EnumSource(AAS2UMLPModes.class)
  void testCardinalities(AAS2UMLPModes mode) {
    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .qualifiers(C_ZERO_TO_ONE_QUALIFIER)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .qualifiers(C_ONE_QUALIFIER)
        .build();

    DefaultProperty prop3 = new DefaultProperty.Builder()
        .idShort("prop3")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId3"))
        .qualifiers(C_ONE_TO_MANY_QUALIFIER)
        .build();

    DefaultProperty prop4 = new DefaultProperty.Builder()
        .idShort("prop4")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId4"))
        .qualifiers(C_ZERO_TO_MANY_QUALIFIER)
        .build();

    DefaultProperty prop5 = new DefaultProperty.Builder()
        .idShort("prop5")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId5"))
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(prop1, prop2, prop3, prop4, prop5))
        .build();

    applyAAS2UMLPDecorator(mode, submodel1);

    CD4CodeTraverser t = CD4CodeMill.inheritanceTraverser();
    AssociationFinder finder = new AssociationFinder("Sub1");
    t.add4CDAssociation(finder);
    cd.accept(t);

    assertEquals(5, finder.getAssociations().size());
    assertTrue(finder.getAssociations().get(0).getRight().getCDCardinality().isOpt());
    assertTrue(finder.getAssociations().get(1).getRight().getCDCardinality().isOne());
    assertTrue(finder.getAssociations().get(2).getRight().getCDCardinality().isAtLeastOne());
    assertTrue(finder.getAssociations().get(3).getRight().getCDCardinality().isMult());
    assertTrue(finder.getAssociations().get(4).getRight().getCDCardinality().isMult());
  }

  @Test
  void testYAML_IDSHORTONLY() {
    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementList sml1 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId1"))
        .value(prop1)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultSubmodelElementList sml2 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId2"))
        .value(prop2)
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(sml1))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub2id")
        .submodelElements(List.of(sml2))
        .build();

    applyAAS2UMLPDecorator(AAS2UMLPModes.IDSHORT_ONLY, List.of(submodel1, submodel2));

    assertTrue(glex.hasGlobalVar(AAS2UMLPGenerator.NAMETRANSFORMATIONS));

    Map<String, String> map = (HashMap<String, String>) glex.getGlobalVar(AAS2UMLPGenerator.NAMETRANSFORMATIONS);

    assertFalse(map.isEmpty());

    assertEquals(6, map.size());
  }

  @Test
  void testYAML_HYBRID() {
    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementList sml1 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId1"))
        .value(prop1)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultSubmodelElementList sml2 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId2"))
        .value(prop2)
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(sml1))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub2id")
        .submodelElements(List.of(sml2))
        .build();

    applyAAS2UMLPDecorator(AAS2UMLPModes.HYBRID, List.of(submodel1, submodel2));

    assertTrue(glex.hasGlobalVar(AAS2UMLPGenerator.NAMETRANSFORMATIONS));

    Map<String, String> map = (HashMap<String, String>) glex.getGlobalVar(AAS2UMLPGenerator.NAMETRANSFORMATIONS);

    assertFalse(map.isEmpty());

    assertEquals(6, map.size());

    assertTrue(glex.hasGlobalVar(AAS2UMLPGenerator.SEMANTICIDTOCLASS));

    Map<String, String> map2 = (HashMap<String, String>) glex.getGlobalVar(AAS2UMLPGenerator.SEMANTICIDTOCLASS);

    assertFalse(map2.isEmpty());

    assertEquals(4, map2.size());
  }

  @Test
  void testYAML_SEMANTICIDONLY() {
    DefaultProperty prop1 = new DefaultProperty.Builder()
        .idShort("prop1")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId1"))
        .build();

    DefaultSubmodelElementList sml1 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId1"))
        .value(prop1)
        .build();

    DefaultProperty prop2 = new DefaultProperty.Builder()
        .idShort("prop2")
        .valueType(DataTypeDefXsd.INT)
        .semanticId(semanticId("semId2"))
        .build();

    DefaultSubmodelElementList sml2 = new DefaultSubmodelElementList.Builder()
        .idShort("sml1")
        .semanticId(semanticId("smlId2"))
        .value(prop2)
        .build();

    DefaultSubmodel submodel1 = new DefaultSubmodel.Builder()
        .idShort("sub1")
        .id("sub1id")
        .submodelElements(List.of(sml1))
        .build();

    DefaultSubmodel submodel2 = new DefaultSubmodel.Builder()
        .idShort("sub2")
        .id("sub2id")
        .submodelElements(List.of(sml2))
        .build();

    applyAAS2UMLPDecorator(AAS2UMLPModes.SEMANTICID_ONLY, List.of(submodel1, submodel2));

    assertTrue(glex.hasGlobalVar(AAS2UMLPGenerator.SEMANTICIDTOCLASS));

    Map<String, String> map2 = (HashMap<String, String>) glex.getGlobalVar(AAS2UMLPGenerator.SEMANTICIDTOCLASS);

    assertFalse(map2.isEmpty());

    assertEquals(4, map2.size());
  }

  private void applyAAS2UMLPDecorator(AAS2UMLPModes mode, DefaultSubmodel submodel) {
    applyAAS2UMLPDecorator(mode, List.of(submodel));
  }

  private void applyAAS2UMLPDecorator(AAS2UMLPModes mode, List<Submodel> submodels) {
    AAS2UMLPDecorator decorator = new AAS2UMLPDecorator(glex, mode);
    decorator.decorate("test", submodels, List.of(), cd);
  }



}
