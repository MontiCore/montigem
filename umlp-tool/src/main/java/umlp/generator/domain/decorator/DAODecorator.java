package umlp.generator.domain.decorator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDClassBuilder;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.db.sql.SQLSchemaGenerator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.*;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.monticore.cd.facade.CDModifier.PRIVATE;
import static de.monticore.cd.facade.CDModifier.PROTECTED;
import static de.monticore.cd.facade.CDModifier.PUBLIC;

public class DAODecorator extends DAOInterfaceDecorator {

  public DAODecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  protected ASTCDType createDAOTypeForDomainClass(ASTCDClass domainClass) {
    ASTCDClassBuilder builder = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(getDAOClassName(domainClass))
        .addAllCDMembers(createAttributesForDomainClassDAOs(domainClass));

    builder.setCDInterfaceUsage(getInterfaceUsageFacade()
        .createCDInterfaceUsage(domainClass.getName() + "DAO" + DAOInterfaceDecorator.INTERFACE_SUFFIX));

    ASTCDClass clazz = builder.build();
    // Add @jsweet.lang.Erased annotation
    glex.replaceTemplate("cd2java.Annotations", clazz,
        new StringHookPoint("@jsweet.lang.Erased"));
    return clazz;
  }

  protected String getDAOClassName(ASTCDClass domainClass) {
    return domainClass.getName() + "DAO";
  }

  /**
   * Creates DaoClass from an Association
   *
   * @param assoc
   * @return
   */
  @Override
  protected ASTCDClass createDAOTypeForAssoc(ASTCDAssociation assoc) {
    return UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(getDAOSpecificNameForAssoc(assoc.getName()))
        .addAllCDMembers(createAttributesForAssociationDAOs(assoc))
        .setCDInterfaceUsage(getInterfaceUsageFacade()
        .createCDInterfaceUsage("Assoc" + StringTransformations.capitalize(assoc.getName())
           + "DAO" + DAOInterfaceDecorator.INTERFACE_SUFFIX)).build();
  }

  protected String getDAOSpecificNameForAssoc(String assocName) {
    return "Assoc" + StringTransformations.capitalize(assocName) + "DAO";
  }

  protected List<ASTCDAttribute> createAttributesForDomainClassDAOs(ASTCDClass domainClass) {
    List<ASTCDAttribute> attributesList = Lists.newLinkedList();

    // Select / Insert Queries
    attributesList.add(createQueryAttrForMethod(domainClass.getName(), "select"));
    attributesList.add(createQueryAttrForMethod(domainClass.getName(), "selectAllIds"));
    attributesList.add(createQueryAttrForMethod(domainClass.getName(), "selectFiltered", "List"));
    attributesList.add(createQueryAttrForMethod(domainClass.getName(), "insert"));
    attributesList.add(createQueryAttrForMethod(domainClass.getName(), "delete"));

    // Set/Update Queries
    for (ASTCDAttribute attr : CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived()).toList()) {
      if (GenService.isList(attr)) {
        ASTCDAttribute removePrepStatement = createQueryAttr(getStatementAttributeName(attr, "remove"));
        attributesList.add(removePrepStatement);
        ASTCDAttribute updatePrepStatement = createQueryAttr(getStatementAttributeName(attr, "update"));
        attributesList.add(updatePrepStatement);
        ASTCDAttribute selectPrepStatement = createQueryAttr( getStatementAttributeName(attr, "select"));
        attributesList.add(selectPrepStatement);
      } else {
        ASTCDAttribute attribute = createQueryAttr(getStatementAttributeName(attr, "update"));
        attributesList.add(attribute);
      }
    }
    return attributesList;
  }

  protected List<ASTCDAttribute> createAttributesForAssociationDAOs(ASTCDAssociation association) {
    List<ASTCDAttribute> attributesList = Lists.newLinkedList();

    // Add / Remove / Update PreparedStatements
    attributesList.add(createQueryAttrForMethod("add", association));
    attributesList.add(createQueryAttrForMethod("remove", association));

    if (association.getCDAssocDir().isDefinitiveNavigableRight()) {
      attributesList.add(createQueryForMethodOfSide("removeAllEntriesRelatedTo", association.getLeft()));
      attributesList.add(createQueryForMethodOfSide("update", association.getRight()));
    }
    if (association.getCDAssocDir().isDefinitiveNavigableLeft()) {
      attributesList.add(createQueryForMethodOfSide("removeAllEntriesRelatedTo", association.getRight()));
      attributesList.add(createQueryForMethodOfSide("update", association.getLeft()));
    }

    // Select for each side PreparedStatement
    String idNumerusLeft = getIDNumerusForAssociationSide(association.getLeft());
    String idNumerusRight = getIDNumerusForAssociationSide(association.getRight());

    attributesList.add(createQueryForSelect(association.getLeft().getName(), association.getRight().getName(), idNumerusLeft));
    attributesList.add(createQueryForSelect(association.getRight().getName(), association.getLeft().getName(), idNumerusRight));

    return attributesList;
  }

  // All Methods for Association DAOs

  @Override
  protected void removeAllMethodForAssocDAO(ASTCDType daoClass, ASTCDAssociation assoc,
                                            ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.RemoveAllInitForAssocDAO", start.getName(),
        SQLSchemaGenerator.convertAssociationRollName(start.getName()), SQLSchemaGenerator.convertAssocName(assoc.getName()));
    getCD4C().addMethod(daoClass, "umlp.dao.associations.RemoveAllForAssocDAO", start.getName());
  }

  @Override
  protected void selectMethodForAssocDAO(ASTCDType daoClass, ASTCDAssociation assoc, ASTCDAssocSide assocStart, ASTCDAssocSide assocEnd) {

    if(GenService.isMultiValued(assocEnd)) {
      selectInitMethodForListAssocDAO(daoClass, assoc.getName(), assocStart, assocEnd, "s");
      selectMethodForListAssocDAO(daoClass, assocStart, assocEnd);
    } else {
      selectInitMethodForListAssocDAO(daoClass, assoc.getName(), assocStart, assocEnd, "");
      selectMethodForGemAssocDAO(daoClass, assocStart, assocEnd);
    }
  }

  protected void selectInitMethodForListAssocDAO(ASTCDType daoClass, String assocName,
                                                 ASTCDAssocSide start, ASTCDAssocSide end, String numerus) {
    String startName = start.getName();
    String endName = end.getName();
    getCD4C().addMethod(daoClass, "umlp.dao.associations.SelectInitForAssocDAO", startName, endName,
        SQLSchemaGenerator.convertAssociationRollName(startName), SQLSchemaGenerator.convertAssociationRollName(endName),
        SQLSchemaGenerator.convertAssocName(assocName), numerus);
  }

  @Override
  protected void selectMethodForListAssocDAO(ASTCDType daoClass, ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.SelectForAssocDAO", start.getName(), end.getName());
  }

  @Override
  protected void selectMethodForGemAssocDAO(ASTCDType daoClass, ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.SelectForGemAssocDAO", start.getName(), end.getName());
  }

  @Override
  protected void addMethodsForAssociationDAO(ASTCDType daoClass, ASTCDAssociation assoc) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.AddInitForAssocDAO", assoc.getName(),
        SQLSchemaGenerator.convertAssociationRollName(assoc.getLeft().getName()),
        SQLSchemaGenerator.convertAssociationRollName(assoc.getRight().getName()),
        SQLSchemaGenerator.convertAssocName(assoc.getName()));
    getCD4C().addMethod(daoClass, "umlp.dao.associations.AddForAssocDAO", assoc);
    getCD4C().addMethod(daoClass, "umlp.dao.associations.AddManyForAssocDAO", assoc);
  }

  @Override
  protected void updateMethodForAssocDAO(ASTCDType daoClass, ASTCDAssociation assoc, ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.UpdateInitForAssocDAO", end.getName(),
        SQLSchemaGenerator.convertAssociationRollName(start.getName()),
        SQLSchemaGenerator.convertAssociationRollName(end.getName()),
        SQLSchemaGenerator.convertAssocName(assoc.getName()));
    getCD4C().addMethod(daoClass, "umlp.dao.associations.UpdateForAssocDAO", start.getName(), end.getName());
  }

  @Override
  protected void removeMethodForAssociationDAO(ASTCDType daoClass, ASTCDAssociation assoc) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.RemoveInitForAssocDAO", assoc.getName(),
        SQLSchemaGenerator.convertAssociationRollName(assoc.getLeft().getName()),
        SQLSchemaGenerator.convertAssociationRollName(assoc.getRight().getName()),
        SQLSchemaGenerator.convertAssocName(assoc.getName()));
    getCD4C().addMethod(daoClass, "umlp.dao.associations.RemoveForAssocDAO", assoc);
  }


  // All methods for domain class daos

  @Override
  protected void updateAndSelectListAttrMethods(ASTCDType doaClass, ASTCDClass domainClass) {
    UMLPSymbolTables.getAttributesInHierarchyWithDefiningType(domainClass).entrySet().stream()
        .filter(entry -> !entry.getKey().getModifier().isDerived() && GenService.isList(entry.getKey())).forEach(entry -> {
              ASTCDAttribute attr = entry.getKey();
              ASTCDType definingType = entry.getValue();
              GenService.createType(attr);
              String innerType = GenService.getInnerType(GenService.createType(attr));
              String tableName = SQLSchemaGenerator.convertListAttributeTableName(definingType.getName(), attr.getName());
              String dbType = GenService.getDBType(attr.getSymbol().getType());
              getCD4C().addMethod(doaClass, "umlp.dao.UpdateListAttr", tableName, attr, innerType, dbType);
              getCD4C().addMethod(doaClass, "umlp.dao.SelectListAttr", tableName, attr, innerType, dbType);
            }
        );
  }

  @Override
  protected void updateAttrMethod(ASTCDType doaClass, ASTCDClass domainClass) {
    getNormalAttributes(domainClass).forEach(attr ->
        getCD4C().addMethod(doaClass, "umlp.dao.UpdateAttr", SQLSchemaGenerator.convertClassName(domainClass.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED),
            attr.getName(),
            SQLSchemaGenerator.convertAttributeName(attr.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED), attr.getMCType().printType(),
            GenService.getDBType(attr.getSymbol().getType())));
  }

  @Override
  protected void updateOptionalAttrMethod(ASTCDType doaClass, ASTCDClass domainClass) {
    CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived() && GenService.isOptional(a)).forEach(attr ->
            getCD4C().addMethod(doaClass, "umlp.dao.UpdateOptionalAttr",
                SQLSchemaGenerator.convertClassName(domainClass.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED),
                attr.getName(),
                SQLSchemaGenerator.convertAttributeName(attr.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED),
                GenService.createType(attr).printType(), GenService.getDBType(attr.getSymbol().getType())));
  }

  @Override
  protected void deleteMethod(ASTCDType daoClass, ASTCDClass domainClass, Map<ASTCDAssocSide, ASTCDAssociation> assocSideToAssoc) {
    getCD4C().addMethod(daoClass, "umlp.dao.Delete", cdName, SQLSchemaGenerator.convertClassName(domainClass.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED),
        domainClass.getName(), assocSideToAssoc, getAssocToSourceDefinitionCD(assocSideToAssoc.values().stream()));
  }

  @Override
  protected void selectMethod(ASTCDType daoClass, ASTCDClass domainClass,
                              Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                              Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap) {
    List<ASTCDAttribute> listAttributes = getListAttributes(domainClass);
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);
    Map<ASTCDAttribute, String> optMap = Maps.newLinkedHashMap();
    getOptionalAttributes(domainClass).forEach(e -> optMap.put(e,
        SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));

    getCD4C().addMethod(daoClass, "umlp.dao.SelectInit",
        SQLSchemaGenerator.convertClassName(domainClass.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED), domainClass.getName(),
        attributes.stream().map(a -> SQLSchemaGenerator.convertAttributeName(a.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED)).collect(Collectors.toList()),
        getOptionalAttributes(domainClass).stream().map(a -> SQLSchemaGenerator.convertAttributeName(a.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED)).collect(Collectors.toList()));

    Map<ASTCDAttribute, String> attributeMap = Maps.newLinkedHashMap();
    attributes.forEach(e -> attributeMap.put(e,
        SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));
    getCD4C().addMethod(daoClass, "umlp.dao.Select", domainClass.getName(), cdName, attributeMap,
        optMap, listAttributes, listAssociationMap, gemAssociationMap);
  }

  @Override
  protected void selectAllIdsMethod(ASTCDType daoClass, ASTCDClass domainClass,
                                    Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                                    Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap){
    List<ASTCDAttribute> listAttributes = getListAttributes(domainClass);
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);
    Map<ASTCDAttribute, String> optMap = Maps.newLinkedHashMap();
    getOptionalAttributes(domainClass).forEach(e -> optMap.put(e,
        SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));

    getCD4C().addMethod(daoClass, "umlp.dao.SelectAllIdsInit",
        SQLSchemaGenerator.convertClassName(domainClass.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED), domainClass.getName(),
        attributes.stream().map(a -> SQLSchemaGenerator.convertAttributeName(a.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED)).collect(Collectors.toList()));

    Map<ASTCDAttribute, String> attributeMap = Maps.newLinkedHashMap();
    attributes.forEach(e -> attributeMap.put(e,
        SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));
    getCD4C().addMethod(daoClass, "umlp.dao.SelectAllIds", domainClass.getName(), cdName, attributeMap,
        optMap, listAttributes, listAssociationMap, gemAssociationMap);
  }

  @Override
  protected void selectFilteredMethod(ASTCDType daoClass, ASTCDClass domainClass) {
    getCD4C().addMethod(daoClass, "umlp.dao.SelectFilteredInit", domainClass.getName());
    getCD4C().addMethod(daoClass, "umlp.dao.SelectFiltered", domainClass.getName());
  }

  @Override
  protected void unpackMethod(ASTCDType daoClass, ASTCDClass domainClass,
                              Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                              Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap){
    List<ASTCDAttribute> listAttributes = getListAttributes(domainClass);
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);
    Map<ASTCDAttribute, String> optMap = Maps.newLinkedHashMap();
    getOptionalAttributes(domainClass).forEach(e -> optMap.put(e,
        SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));

    Map<String, String> assocToCorrectManagerMap =
        getAssocToSourceDefinitionCD(Stream.of(gemAssociationMap.values(), listAssociationMap.values()).flatMap(Collection::stream));

    Map<String, String> assocToCorrectDAOFactoryMap =
        getAssocToTargetDefinitionCD(Stream.of(gemAssociationMap.values(), listAssociationMap.values()).flatMap(Collection::stream));

    Map<ASTCDAttribute, String> attributeMap = Maps.newLinkedHashMap();
    attributes.forEach(e -> attributeMap.put(e,
        SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));
    getCD4C().addMethod(daoClass, "umlp.dao.Unpack", domainClass.getName(), cdName, attributeMap,
        optMap, listAttributes, listAssociationMap, gemAssociationMap, assocToCorrectManagerMap, assocToCorrectDAOFactoryMap,"");
  }

  @Override
  protected void insertMethods(ASTCDType daoClass, ASTCDClass domainClass,
                               Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                               Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap) {
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);
    List<ASTCDAttribute> listAttributes = getListAttributes(domainClass);
    List<ASTCDAttribute> optionalAttributes = getOptionalAttributes(domainClass);

    Map<String, String> assocToSourceDefinitionCD =
        getAssocToSourceDefinitionCD(Stream.of(gemAssociationMap.values(), listAssociationMap.values()).flatMap(Collection::stream));

    getCD4C().addMethod(daoClass, "umlp.dao.InsertInit", SQLSchemaGenerator.convertClassName(domainClass.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED)
        , domainClass.getName(), attributes.stream().map(a -> SQLSchemaGenerator.convertAttributeName(a.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED)).collect(Collectors.toList()),
        optionalAttributes.stream().map(a -> SQLSchemaGenerator.convertAttributeName(a.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED)).collect(Collectors.toList()));
    getCD4C().addMethod(daoClass, "umlp.dao.Insert", domainClass.getName(), cdName, attributes,
        optionalAttributes, listAttributes, listAssociationMap, gemAssociationMap, assocToSourceDefinitionCD, "");
    getCD4C().addMethod(daoClass, "umlp.dao.BulkInsert", domainClass.getName(), cdName, attributes,
        optionalAttributes , listAttributes, listAssociationMap, gemAssociationMap,"");
    /*getCD4C().addMethod(daoClass, "umlp.dao.AddManyAssociationsToBatch", domainClass.getName(), cdName, attributes,
        optionalAttributes , listAttributes, listAssociationMap, gemAssociationMap, assocToSourceDefinitionCD);
    getCD4C().addMethod(daoClass, "umlp.dao.ExecuteAssocBatches", domainClass.getName(), cdName, attributes,
        optionalAttributes , listAttributes, listAssociationMap, gemAssociationMap, assocToSourceDefinitionCD);*/
  }

  @Override
  protected void addDatabaseSpecificImports(ASTCDType clazz) {
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("DatabaseAccessManager"));

    // JDBC
    getCD4C().addImport(clazz, "java.sql.PreparedStatement");
    getCD4C().addImport(clazz, "java.sql.ResultSet");
    getCD4C().addImport(clazz, "java.sql.SQLException");
    getCD4C().addImport(clazz, "java.io.IOException");
    getCD4C().addImport(clazz, "java.sql.Statement");
    getCD4C().addImport(clazz,"java.sql.BatchUpdateException");
    getCD4C().addImport(clazz,"java.sql.Connection");
  }

  @Override
  protected void addImports(ASTCDType domainClass, ASTCDType clazz) {
    super.addImports(domainClass, clazz);
    Set<String> managersFromExtendedCDs = new LinkedHashSet<>();
    Set<String> daoFactoriesFromExtendedCDs = new LinkedHashSet<>();
    UMLPSymbolTables.getOppositeSideToAssociationMap((ASTCDClass) domainClass).values().stream()
        .filter(association -> ! UMLPSymbolTables.getDiagramFor(association).getCDDefinition().getName().equals(
            UMLPSymbolTables.getDiagramFor(domainClass).getCDDefinition().getName()))
        .forEach(association -> {
          managersFromExtendedCDs.add(
              Joiners.DOT.join(association.getSymbol().getPackageName().toLowerCase(),
                  getCorrectManager(association)));
          daoFactoriesFromExtendedCDs.add(
              Joiners.DOT.join(association.getSymbol().getPackageName().toLowerCase(),
                  UMLPSymbolTables.getDiagramFor(association).getCDDefinition().getName()+"DAOFactory"));
        });

    CDSymbolTables.getAssociations(domainClass).forEach(assocSide ->
        managersFromExtendedCDs.add(
            Joiners.DOT.join(assocSide.getSymbol().getType().getTypeInfo().getPackageName().toLowerCase(),
            UMLPSymbolTables.getDiagramFor(assocSide.getSymbol().getType().getTypeInfo().getAstNode()).getCDDefinition().getName()+ManagerDecorator.SUFFIX)));
    managersFromExtendedCDs.forEach(managerImport -> getCD4C().addImport(clazz, managerImport));
    daoFactoriesFromExtendedCDs.forEach(managerImport -> getCD4C().addImport(clazz, managerImport));
  }

  // Used to find out correct Manager
  protected Map<String,String> getAssocToTargetDefinitionCD(Stream<ASTCDAssociation> associations) {
    Map<String, String> assocToCorrectManagerMap = new LinkedHashMap<>();
    associations.forEach(assoc -> {
      // Assumes a CoCo guarantees there no type of CD is changed
      if(assoc.getCDAssocDir().isDefinitiveNavigableRight()) {
        assocToCorrectManagerMap.put(assoc.getName(),
            UMLPSymbolTables.getDiagramFor(assoc.getRight().getSymbol().getType().getTypeInfo().getAstNode()).getCDDefinition().getName());
      } else {
        assocToCorrectManagerMap.put(assoc.getName(),
            UMLPSymbolTables.getDiagramFor(assoc.getLeft().getSymbol().getType().getTypeInfo().getAstNode()).getCDDefinition().getName());
      }
    });
    return assocToCorrectManagerMap;
  }

  // Used to find out correct DAOFactory
  protected Map<String,String> getAssocToSourceDefinitionCD(Stream<ASTCDAssociation> associations) {
    Map<String, String> assocToCorrectManagerMap = new LinkedHashMap<>();
    associations.forEach(assoc -> {
      // Assumes a CoCo guarantees there no type of CD is changed
      if(assoc.getCDAssocDir().isDefinitiveNavigableRight()) {
        assocToCorrectManagerMap.put(assoc.getName(),
            UMLPSymbolTables.getDiagramFor(assoc.getLeft().getSymbol().getType().getTypeInfo().getAstNode()).getCDDefinition().getName());
      } else {
        assocToCorrectManagerMap.put(assoc.getName(),
            UMLPSymbolTables.getDiagramFor(assoc.getRight().getSymbol().getType().getTypeInfo().getAstNode()).getCDDefinition().getName());
      }
    });
    return assocToCorrectManagerMap;
  }

  /******* Helper Methods for Query Attribute generation *******/

  protected ASTCDAttribute createQueryAttrForMethod(String method, ASTCDAssociation association) {
    return createQueryAttr(method + StringTransformations.capitalize(association.getName()));
  }

  protected ASTCDAttribute createQueryForMethodOfSide(String method, ASTCDAssocSide assocSide) {
    return createQueryAttr(method + StringTransformations.capitalize(assocSide.getName()));
  }

  protected ASTCDAttribute createQueryForSelect(String requested, String given, String requestedNumerus) {
    return createQueryAttr(
        "select" + StringTransformations.capitalize(requested) + "Id" + requestedNumerus + "For" + StringTransformations.capitalize(given));
  }

  protected ASTCDAttribute createQueryAttrForMethod(String domainClassName, String methodName) {
    return createQueryAttrForMethod(domainClassName, methodName, "");
  }

  protected ASTCDAttribute createQueryAttrForMethod(String domainClassName, String methodName, String suffix) {
    return createQueryAttr(methodName + domainClassName + suffix);
  }

  protected ASTCDAttribute createQueryAttr(String attrName) {
    return getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createQualifiedType("String"), attrName);
  }

  protected String getIDNumerusForAssociationSide(ASTCDAssocSide side) {
    if (GenService.isMultiValued(side)) {
      return "s";
    }
    return "";
  }

  protected String getStatementAttributeName(ASTCDAttribute attr, String methodName) {
    return methodName + StringTransformations.capitalize(attr.getName()) + "Statement";
  }
}
