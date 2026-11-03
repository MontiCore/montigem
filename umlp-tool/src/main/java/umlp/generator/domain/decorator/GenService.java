/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import com.google.common.base.Defaults;
import de.monticore.types3.TypeCheck3;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._ast.ASTCDCardinality;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.class2mc.adapters.JClass2OOTypeSymbolAdapter;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpressionBuilder;
import de.monticore.symbols.basicsymbols.BasicSymbolsMill;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types.check.SymTypeOfGenerics;
import de.monticore.types.check.TypeCheck;
import de.monticore.types.check.TypeRelations;
import de.monticore.types.mcbasictypes._ast.ASTMCQualifiedType;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types.mccollectiontypes._ast.ASTMCListType;
import de.monticore.types.mccollectiontypes._ast.ASTMCOptionalType;
import de.monticore.types.mccollectiontypes._ast.ASTMCSetType;
import de.monticore.types.mcsimplegenerictypes._ast.ASTMCBasicGenericType;
import de.monticore.types.mccollectiontypes._ast.ASTMCGenericType;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.umlmodifier._ast.ASTModifier;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.UMLPMill;
import umlp._prettyprint.UMLPFullPrettyPrinter;
import umlp.generator.util.TagLookUp;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.monticore.types.check.SymTypePrimitive.box;

public class GenService {

  public static final String UMLP_ID_NAME = "umlpId";
  public static final String DERIVED_IMPL_TEMPLATE = "implTemplate";

  public GenService() {
  }

  public static TypeRelations typeCheck = new TypeRelations();

  public static ASTExpression createInitialValue(SymTypeExpression type) {
    ASTNameExpressionBuilder exprBuilder = UMLPMill.nameExpressionBuilder();
    if (isOptional(type)) {
      exprBuilder.setName("Optional.empty()");
    } else if (isList(type)) {
      exprBuilder.setName("new LinkedList<>()");
    } else if (isSet(type)) {
      exprBuilder.setName("new HashSet<>()");
    } else if (typeCheck.isInt(type)) {
      exprBuilder.setName(String.valueOf(Defaults.defaultValue(int.class)));
    } else if (typeCheck.isDouble(type)) {
      exprBuilder.setName(String.valueOf(Defaults.defaultValue(double.class)));
    } else if (typeCheck.isChar(type)) {
      exprBuilder.setName("\'\\" + "u0000\'");
    } else if (typeCheck.isShort(type)) {
      exprBuilder.setName(String.valueOf(Defaults.defaultValue(short.class)));
    } else if (typeCheck.isFloat(type)) {
      exprBuilder.setName(String.valueOf(Defaults.defaultValue(float.class)) + "f");
    } else if (typeCheck.isLong(type)) {
      exprBuilder.setName(String.valueOf(Defaults.defaultValue(long.class)) + "L");
    } else if (typeCheck.isBoolean(type)) {
      exprBuilder.setName(String.valueOf(Defaults.defaultValue(boolean.class)));
    } else if (typeCheck.isByte(type)) {
      exprBuilder.setName(String.valueOf(Defaults.defaultValue(byte.class)));
    } else if ("String".equals(type.print())) {
      exprBuilder.setName("\"\" ");
    }
    return exprBuilder.build();
  }

  public static String calculateJSon(SymTypeExpression type) {
    String json = "";
    if (typeCheck.isBoolean(type)) {
      json = "getAsJsonBoolean().getValue()";
    } else if (typeCheck.isShort(type)) {
      json = "getAsJsonNumber().getNumberAsShort()";
    } else if (typeCheck.isByte(type)) {
      json = "getAsJsonNumber().getNumberAsByte()";
    } else if (typeCheck.isInt(type)) {
      json = "getAsJsonNumber().getNumberAsInteger()";
    } else if (typeCheck.isFloat(type)) {
      json = "getAsJsonNumber().getNumberAsFloat()";
    } else if (typeCheck.isLong(type)) {
      json = "getAsJsonNumber().getNumberAsLong()";
    } else if (typeCheck.isDouble(type)) {
      json = "getAsJsonNumber().getNumberAsDouble()";
    } else if (typeCheck.isString(type)) {
      json = "getAsJsonString().getValue()";
    } else if (isCDEnum(type)) {
      json = "getAsJsonString().getValue()";
    } else if (type.isGenericType() && (
        ((SymTypeOfGenerics) type).printTypeWithoutTypeArgument().equals("List")
            || ((SymTypeOfGenerics) type).printTypeWithoutTypeArgument().equals("java.util.List"))) {
      return calculateJSon(((SymTypeOfGenerics) type).getArgument(0));
    }
    // else make abstract or use template
    return json;
  }

  public static String calculateJsonInclGenerics(SymTypeExpression type, String parameterName) {
    if (isList(type)) {
      return calculateJsonForList(type, parameterName);
    }
    if (!isOptional(type)) {
      return calculateJSon(type);
    }
    return calculateJSon(((SymTypeOfGenerics) type).getArgument(0));
  }

  public static String calculateJsonForList(SymTypeExpression type, String parameterName) {
//    return "getAsJsonArray().getValues().stream().map(e -> e." + calculateJSon(((SymTypeOfGenerics) type).getArgument(0)) +
//        ").collect(java.util.stream.Collectors.toList())";
    return "get" + StringTransformations.capitalize(parameterName) + "(jsonObject)";
  }

  public static String printTypeFullName(SymTypeExpression type) {
    if (!type.isGenericType()) {
      return type.printFullName();
    }
    return ((SymTypeOfGenerics) type).getArgument(0).printFullName();
  }

  // TODO (MB): Bessere Lösung?
  public static SymTypeExpression getSymType(String type) {
    Optional<TypeSymbol> primitive = BasicSymbolsMill.globalScope().resolveType(type);
    if (primitive.isPresent()) {
      return SymTypeExpressionFactory.createPrimitive(primitive.get());
    }
    return SymTypeExpressionFactory.createObscureType();
  }

  public static Optional<String> getBuildJsonHelperMethod(SymTypeExpression type) {
    Optional<String> jsonHelperMethod = Optional.empty();
    if (type.isObjectType() && type.printFullName().equals("java.time.LocalDate")) {
      jsonHelperMethod = Optional.of("DateTimeFormatter.ISO_DATE_TIME.format");
    }
    return jsonHelperMethod;
  }

  public static Optional<String> getBuildJsonHelperMethodImport(SymTypeExpression type) {
    Optional<String> jsonHelperMethod = Optional.empty();
    if (type.isObjectType() && type.printFullName().equals("java.time.LocalDate")) {
      jsonHelperMethod = Optional.of("java.time.format.DateTimeFormatter");
    }
    return jsonHelperMethod;
  }

  public static Optional<String> getFromJsonHelperMethod(SymTypeExpression type) {
    Optional<String> jsonHelperMethod = Optional.empty();
    if (type.isObjectType() && type.printFullName().equals("java.time.LocalDate")) {
      jsonHelperMethod = Optional.of("LocalDate.parse");
    } else if (isOptional(type)) {
      jsonHelperMethod = Optional.of("Optional.ofNullable");
    }
    return jsonHelperMethod;
  }

  public static Optional<String> getFromJsonHelperMethodImport(SymTypeExpression type) {
    Optional<String> jsonHelperMethod = Optional.empty();
    if (type.isObjectType() && type.printFullName().equals("java.time.LocalDate")) {
      jsonHelperMethod = Optional.of("java.time.LocalDate");
    }
    return jsonHelperMethod;
  }

  public static ASTExpression createInitialValue(ASTMCType type) {
    SymTypeExpression result = TypeCheck3.symTypeFromAST(type);
    if (!result.isObscureType()) {
      return createInitialValue(result);
    }
    Log.error("");
    return null;
  }

  public static ASTMCType createType(ASTCDAttribute attr) {
    if (attr.getSymbol().getType().isGenericType()) {
      SymTypeOfGenerics genericType = (SymTypeOfGenerics) attr.getSymbol().getType();
      List<String> arguments = genericType.getArgumentList().stream().
          map(a -> box(a.print())).collect(Collectors.toList());
      return MCTypeFacade.getInstance().createBasicGenericTypeOf(genericType.getBaseName(), arguments.toArray(new String[0]));
    } else {
      return MCTypeFacade.getInstance().createQualifiedType(box(getType(attr.getSymbol().getType())));
    }
  }

  public static String createName(String attrName) {
    return "_" + attrName;
  }

  public static String getType(SymTypeExpression symType) {
    if (symType.isGenericType()) {
      String res = ((SymTypeOfGenerics) symType).getBaseName();
      return box(res);
    }
    if (isDomainType(symType)) {
      return symType.print();
    }
    if(isNotADomainType(symType) && !isJavaType(symType)) {
      return symType.print();
    }
    return box(symType.getTypeInfo().getFullName());
  }

  public static boolean isVisible(ASTCDAttribute attr) {
    return !attr.getModifier().isFinal()
        && !attr.getModifier().isStatic();
  }

  public static boolean isVisibleNotDerived(ASTCDAttribute attr) {
    return !attr.getModifier().isDerived()
        && !attr.getModifier().isFinal()
        && !attr.getModifier().isStatic();
  }

  public static boolean isDerived(ASTCDAttribute attr) {
    return attr.getModifier().isDerived();
  }

  public static boolean isGem(ASTCDAttribute attr) {
    if (attr.getMCType() instanceof ASTMCBasicGenericType) {
      List<String> nameList = ((ASTMCBasicGenericType) attr.getMCType()).getNameList();
      return "Gem".equals(nameList.get(nameList.size() - 1)) || "GemList".equals(nameList.get(nameList.size() - 1));
    }
    return false;
  }

  public static String getGetterName(String name, SymTypeExpression type) {
    return (typeCheck.isBoolean(type) ? "is" : "get")
        + StringTransformations.capitalize(name);
  }

  /**
   * whether the attribute holds a GemMap, i.e. a GemValueMap or a GemEntryMap
   */
  public static boolean isGemMap(ASTCDAttribute attr) {
    if (attr.getMCType() instanceof ASTMCBasicGenericType) {
      List<String> nameList = ((ASTMCBasicGenericType) attr.getMCType()).getNameList();
      String name = nameList.get(nameList.size() - 1);
      return "GemValueMap".equals(name) || "GemEntryMap".equals(name);
    }
    return false;
  }

  public static boolean isGemEntryMap(ASTCDAttribute attr) {
    if (attr.getMCType() instanceof ASTMCBasicGenericType) {
      List<String> nameList = ((ASTMCBasicGenericType) attr.getMCType()).getNameList();
      return "GemEntryMap".equals(nameList.get(nameList.size() - 1));
    }
    return false;
  }

  /**
   * @param attr an attribute holding a GemMap, see {@link #isGemMap}
   * @return the type its keys are stored and synchronized as: the key type of a GemValueMap,
   * the gemId of the key object (Long) for a GemEntryMap
   */
  public static String getGemMapStoredKeyType(ASTCDAttribute attr) {
    if (isGemEntryMap(attr)) {
      return "Long";
    }
    return new CdUtilsPrinter().printType(((ASTMCGenericType) attr.getMCType()).getMCTypeArgument(0).getMCTypeOpt().get());
  }

  public static boolean isGemList(ASTCDAttribute attr) {
    if (attr.getMCType() instanceof ASTMCBasicGenericType) {
      List<String> nameList = ((ASTMCBasicGenericType) attr.getMCType()).getNameList();
      return "GemList".equals(nameList.get(nameList.size() - 1));
    }
    return false;
  }

  public static boolean isList(ASTCDAttribute attr) {
    ASTMCType mcType = attr.getMCType();
    return isList(mcType);
  }

  public static boolean isList(ASTMCType mcType) {
    if (mcType instanceof ASTMCListType) {
      return true;
    }
    if (mcType instanceof ASTMCBasicGenericType) {
      List<String> nameList = ((ASTMCBasicGenericType) mcType).getNameList();
      return "List".equals(nameList.get(nameList.size() - 1));
    }
    if (mcType instanceof ASTMCQualifiedType) {
      List<String> nameList = ((ASTMCQualifiedType) mcType).getNameList();
      return "List".equals(nameList.get(nameList.size() - 1));
    }
    return false;
  }

  public static boolean isSet(ASTCDAttribute attr) {
    ASTMCType mcType = attr.getMCType();
    return isSet(mcType);
  }

  public static boolean isSet(ASTMCType mcType) {
    if (mcType instanceof ASTMCSetType) {
      return true;
    }
    if (mcType instanceof ASTMCBasicGenericType) {
      List<String> nameList = ((ASTMCBasicGenericType) mcType).getNameList();
      return "Set".equals(nameList.get(nameList.size() - 1));
    }
    if (mcType instanceof ASTMCQualifiedType) {
      List<String> nameList = ((ASTMCQualifiedType) mcType).getNameList();
      return "Set".equals(nameList.get(nameList.size() - 1));
    }
    return false;
  }

  public static boolean isOptional(ASTCDAttribute attr) {
    ASTMCType mcType = attr.getMCType();
    return isOptional(mcType);
  }

  public static boolean isOptional(ASTMCType mcType) {
    if (mcType instanceof ASTMCOptionalType) {
      return true;
    }

    if (mcType instanceof ASTMCBasicGenericType) {
      List<String> nameList = ((ASTMCBasicGenericType) mcType).getNameList();
      return "Optional".equals(nameList.get(nameList.size() - 1));
    }
    return false;
  }

  public static boolean isBoolean(ASTMCType mcType) {
    return mcType.printType().equals("java.lang.Boolean")
        || mcType.printType().equals("boolean");
  }

  public static boolean isVisible(ASTCDAssocSide attr) {
    return !attr.getModifier().isFinal()
        && !attr.getModifier().isStatic();
  }

  public static boolean isVisibleNotDerived(ASTCDAssocSide attr) {
    return !GenService.isSymbolDerived(attr)
        && !attr.getModifier().isFinal()
        && !attr.getModifier().isStatic();
  }

  public static boolean isDerived(ASTCDAssocSide attr) {
    return attr.getModifier().isDerived();
  }

  public static boolean isList(ASTCDAssocSide assoc) {
    return assoc.isPresentCDCardinality()
        && (assoc.getCDCardinality().isMult() || assoc.getCDCardinality().isAtLeastOne())
        && !isMap(assoc);
  }

  /**
   * The target side of an association qualified by an attribute of the target class
   * ({@code Owner [[attr]] -> Target}) is a GemMap of the qualified side, keyed by attr.
   * Type qualifiers ({@code [Type]}) are not supported yet and ignored.
   *
   * @param assoc the target side, as returned by e.g. CDSymbolTables.getAssociations(owner)
   */
  public static boolean isMap(ASTCDAssocSide assoc) {
    return getQualifierAttributeName(assoc).isPresent();
  }

  /**
   * @param assoc the target side of an association
   * @return the name of the target attribute qualifying the association, if any
   */
  public static Optional<String> getQualifierAttributeName(ASTCDAssocSide assoc) {
    if (!assoc.isPresentSymbol() || !assoc.getSymbol().getOtherSide().isPresentAssocSide()) {
      return Optional.empty();
    }
    // the qualifier stands on the qualified (opposite) side, but names an attribute of the target
    ASTCDAssocSide qualifiedSide = assoc.getSymbol().getOtherSide().getAssocSide();
    if (qualifiedSide.isPresentCDQualifier() && qualifiedSide.getCDQualifier().isPresentByAttributeName()) {
      return Optional.of(qualifiedSide.getCDQualifier().getByAttributeName());
    }
    return Optional.empty();
  }

  /**
   * whether the association side holds several target ids, i.e. is a GemList or a GemMap
   */
  public static boolean isMultiValued(ASTCDAssocSide assoc) {
    return isList(assoc) || isMap(assoc);
  }

  public static boolean isList(ASTCDCardinality cardinality){
    return cardinality.isMult() || cardinality.isAtLeastOne();
  }

  public static boolean isAStLeastOne(ASTCDAssocSide assoc) {
    return assoc.isPresentCDCardinality() && assoc.getCDCardinality().isAtLeastOne() && !isMap(assoc);
  }

  public static boolean isOptional(ASTCDAssocSide assoc) {
    return assoc.isPresentCDCardinality() && assoc.getCDCardinality().isOpt() && !isMap(assoc);
  }

  public static boolean isBidirectional(ASTCDAssocSide assoc) {
    return assoc.getSymbol().isIsDefinitiveNavigable() && assoc.getSymbol().getOtherSide().isIsDefinitiveNavigable();
  }

  public static boolean isSymbolDerived(ASTCDAssocSide side) {
    return side.getSymbol().getAssoc().getAssociation().getAstNode().getModifier().isDerived();
  }

  public static boolean isSymbolLocal(ASTCDAssocSide side) {
    return side.getSymbol().getAssoc().getAssociation().getAstNode().getModifier().isLocal();
  }
  public static boolean hasImplTemplate(ASTCDAssocSide side){
    ASTCDAssociation assoc = side.getSymbol().getAssoc().getAssociation().getAstNode();
    ASTModifier m = assoc.getModifier();
    return m.isPresentStereotype() && m.getStereotype().contains(DERIVED_IMPL_TEMPLATE);
  }

  public static String getInnerType(ASTMCType mcType) {
    UMLPFullPrettyPrinter prettyPrinter = new UMLPFullPrettyPrinter();
    if (!isList(mcType) && !isOptional(mcType)) {
      Log.error("Not a list or optional type: " + prettyPrinter.prettyprint(mcType));
      return null;
    }

    if (mcType instanceof ASTMCBasicGenericType) {
      ASTMCBasicGenericType genType = (ASTMCBasicGenericType) mcType;
      return prettyPrinter.prettyprint(genType.getMCTypeArgument(0).getMCTypeOpt().get());
    }

    if (mcType instanceof ASTMCListType) {
      ASTMCListType lt = (ASTMCListType) mcType;
      if (!lt.getMCTypeArgumentList().isEmpty()) {
        return prettyPrinter.prettyprint(lt.getMCTypeArgumentList().get(0).getMCTypeOpt().get());
      }
    }

    if (mcType instanceof ASTMCOptionalType) {
      ASTMCOptionalType ot = (ASTMCOptionalType) mcType;
      if (!ot.getMCTypeArgumentList().isEmpty()) {
        return prettyPrinter.prettyprint(ot.getMCTypeArgumentList().get(0).getMCTypeOpt().get());
      }
    }

    return "java.lang.Object";
  }

  public static Optional<ASTMCType> getInnerTypeOfGenerics(ASTMCType mcType) {
    if (!isList(mcType) && !isOptional(mcType)) {
      return Optional.empty();
    }

    if (mcType instanceof ASTMCBasicGenericType) {
      ASTMCBasicGenericType genType = (ASTMCBasicGenericType) mcType;
      return Optional.of(genType.getMCTypeArgument(0).getMCTypeOpt().get());
    }

    if (mcType instanceof ASTMCListType) {
      ASTMCListType lt = (ASTMCListType) mcType;
      if (!lt.getMCTypeArgumentList().isEmpty()) {
        return Optional.of(lt.getMCTypeArgumentList().get(0).getMCTypeOpt().get());
      }
    }

    if (mcType instanceof ASTMCOptionalType) {
      ASTMCOptionalType ot = (ASTMCOptionalType) mcType;
      if (!ot.getMCTypeArgumentList().isEmpty()) {
        return Optional.of(ot.getMCTypeArgumentList().get(0).getMCTypeOpt().get());
      }
    }

    return Optional.empty();
  }

  /***************************************************************************/
  /* TODO: Nach TypeCheck verschieben?                                       */

  /***************************************************************************/

  public static boolean isOptional(SymTypeExpression type) {
    if (type instanceof SymTypeOfGenerics) {
      return ((SymTypeOfGenerics) type).getBaseName().equals("Optional");
    }
    return false;
  }

  public static boolean isList(SymTypeExpression type) {
    if (type instanceof SymTypeOfGenerics) {
      return ((SymTypeOfGenerics) type).getBaseName().equals("List");
    }
    return false;
  }

  public static boolean isSet(SymTypeExpression type) {
    if (type instanceof SymTypeOfGenerics) {
      return ((SymTypeOfGenerics) type).getBaseName().equals("Set");
    }
    return false;
  }

  public static boolean isCollection(SymTypeExpression type) {
    if (type instanceof SymTypeOfGenerics) {
      return type.getTypeInfo().getInterfaceList().stream().filter(
          i -> i.getTypeInfo().getName().equals("Collection")).findAny().isPresent();
    }
    return false;
  }

  // Note: Includes all domain types, not just gem identifiable types! (such as enums)
  public static boolean isDomainType(SymTypeExpression type) {
    // TODO: Bessere Lösung?
    if(!type.hasTypeInfo()) {
      Log.error("0xABC01: "+type.getClass().getName()+ " "+type.printFullName()
          + " has no type symbol.");
    }
    return isDomainType(type.getTypeInfo());
  }

  public static boolean isDomainType(TypeSymbol typeSymbol) {
    return typeSymbol instanceof CDTypeSymbol && !TagLookUp.isPojo(typeSymbol);
  }

  public static boolean isCDEnum(SymTypeExpression type) {
    return isDomainType(type) && ((CDTypeSymbol) type.getTypeInfo()).isIsEnum();
  }

  public static boolean isInnerCDEnum(SymTypeExpression type) {
    if (type.isGenericType()) {
      return isCDEnum(((SymTypeOfGenerics) type).getArgument(0));
    }
    return false;
  }

  public static boolean isCDOrInnerCDEnum(SymTypeExpression type) {
    return isCDEnum(type) || isInnerCDEnum(type);
  }

  public static boolean isPojoCDType(SymTypeExpression type) {
    if(!type.hasTypeInfo()) {
      Log.error("0xABC02: "+type.getClass().getName()+ " "+type.printFullName()
          + " has no type symbol.");
    }
    // TODO: Bessere Lösung?
    return isPojoCDType(type.getTypeInfo());
  }

  public static boolean isPojoCDType(TypeSymbol type) {
    // TODO: Bessere Lösung?
    return TagLookUp.isPojo(type);
  }

  public static boolean isNotADomainType(SymTypeExpression type) {
    if(!type.hasTypeInfo()) {
      Log.error("0xABC02: "+type.getClass().getName()+ " "+type.printFullName()
          + " has no type symbol.");
    }
    // TODO: Bessere Lösung?
    return isNotADomainType(type.getTypeInfo());
  }

  public static boolean isNotADomainType(TypeSymbol type) {
    // TODO: Bessere Lösung?
    return
        // Check for Java Types
        (type instanceof JClass2OOTypeSymbolAdapter)
            // check for Pojo types
            || TagLookUp.isPojo(type);
  }

  public static boolean isJavaType(SymTypeExpression type) {
    if(!type.hasTypeInfo()) {
      Log.error("0xABC03: "+type.getClass().getName()+ " "+type.printFullName()
          + " has no type symbol.");
    }
    // TODO: Bessere Lösung?
    return isJavaType(type.getTypeInfo());
  }

  public static boolean isJavaType(TypeSymbol type) {
    // TODO: Bessere Lösung?
    return (type instanceof JClass2OOTypeSymbolAdapter);
  }

  public static String getCorrectJavaFQN(String importString) {
    if(!importString.contains(".")) {
      return importString;
    }
    String[] splitString = importString.split("\\.");
    // Assume at least one . in import String
    for (int i = 0; i < splitString.length-1; i++) {
      splitString[i] = splitString[i].toLowerCase();
    }
    return String.join(".", splitString);
  }

  public static String getNativeName(String name) {
    if (name.startsWith("_")) {
      return name.substring(1);
    }
    return name;
  }

  public static ASTMCType createType(SymTypeExpression type) {
    if (type.isGenericType()) {
      SymTypeOfGenerics genericType = (SymTypeOfGenerics) type;
      List<String> arguments = genericType.getArgumentList().stream().
          map(a -> box(a.print())).collect(Collectors.toList());
      return MCTypeFacade.getInstance().createBasicGenericTypeOf(genericType.getBaseName(), arguments.toArray(new String[0]));
    } else {
      return MCTypeFacade.getInstance().createQualifiedType(getType(type));
    }
  }

  public static String getDBType(SymTypeExpression type) {
    if (type.isGenericType()) {
      return getDBType(((SymTypeOfGenerics) type).getArgument(0));
    }
    if (TypeCheck.isShort(type)) {
      return "Short";
    } else if (TypeCheck.isInt(type)) {
      return "Integer";
    } else if(TypeCheck.isByte(type)) {
      return "Byte";
    } else if (TypeCheck.isLong(type)) {
      return "Long";
    } else if (TypeCheck.isFloat(type)) {
      return "Float";
    } else if (TypeCheck.isDouble(type)) {
      return "Double";
    } else if (TypeCheck.isBoolean(type)) {
      return "Boolean";
    } else if (TypeCheck.isString(type)) {
      return "String";
    } else if (isCDEnum(type)) {
      return "java.lang.Enum";
    }
    return type.printFullName();
  }

  public static String getDBType(SymTypeExpression type, String neo4j) {
    if (neo4j != null && !neo4j.isEmpty()) {
      if (TypeCheck.isShort(type)) {
        return "Long";
      } else if (TypeCheck.isInt(type)) {
        return "Long";
      } else if(TypeCheck.isByte(type)) {
        return "Long";
      } else if (TypeCheck.isLong(type)) {
        return "Long";
      }
    }
    if (type.isGenericType()) {
      return getDBType(((SymTypeOfGenerics) type).getArgument(0),neo4j);
    }
    if (TypeCheck.isShort(type)) {
      return "Short";
    } else if (TypeCheck.isInt(type)) {
      return "Integer";
    } else if(TypeCheck.isByte(type)) {
      return "Byte";
    } else if (TypeCheck.isLong(type)) {
      return "Long";
    } else if (TypeCheck.isFloat(type)) {
      return "Float";
    } else if (TypeCheck.isDouble(type)) {
      return "Double";
    } else if (TypeCheck.isBoolean(type)) {
      return "Boolean";
    } else if (TypeCheck.isString(type)) {
      return "String";
    } else if (isCDEnum(type)) {
      return "java.lang.Enum";
    }
    return type.printFullName();
  }

  public static String getTypeConversionCall(SymTypeExpression type) {
    return "";
  }

  public static String getTypeConversionCall(SymTypeExpression type, String neo4j) {
    if (TypeCheck.isInt(type)) {
      return ".intValue()";
    } else if(TypeCheck.isByte(type)) {
      return ".byteValue()";
    } else if(TypeCheck.isShort(type)) {
      return ".shortValue()";
    }
    if (type.isGenericType()) {
      return getTypeConversionCall(((SymTypeOfGenerics) type).getArgument(0),neo4j);
    }
    return "";
  }

  public static boolean containsMethod(ASTCDType type, ASTCDMethodSignature method) {
    return type.getCDMethodList().stream().anyMatch(m -> m.deepEquals(method));
  }

  public static boolean hasStereotype(ASTModifier modifier, String value) {
    if (modifier.isPresentStereotype()) {
      return modifier.getStereotype().contains(value);
    }
    return false;
  }

  public static boolean isListOfGemIdentifiable(SymTypeExpression type) {
    return GenService.isList(type) && GenService.isDomainType(((SymTypeOfGenerics) type).getArgument(0))
        && !isInnerCDEnum(type);
  }
}
