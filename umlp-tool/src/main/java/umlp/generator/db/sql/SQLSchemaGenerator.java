/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.db.sql;

import com.google.common.base.CaseFormat;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDBasisNode;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.generating.GeneratorEngine;
import de.monticore.generating.GeneratorSetup;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.symboltable.ISymbol;
import de.monticore.types.check.SymTypeOfGenerics;
import de.se_rwth.commons.logging.Log;
import umlp._prettyprint.UMLPFullPrettyPrinter;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.UMLPSymbolTables;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: cardinality constraints?
public class SQLSchemaGenerator {
  public static final int MAX_IDENTIFIER_BYTES = 63;

  // identifiers that collide after truncation and thus get a hash, filled by initIdentifiers.
  // static since the DAO decorators use the static convert methods, reset per generator (one per CD)
  protected static final Set<String> hashedIdentifiers = new HashSet<>();
  protected static final Set<String> hashedColumns = new HashSet<>();

  protected GeneratorSetup generatorSetup;
  protected GeneratorEngine generatorEngine;
  protected Map<String, String> fqnClassNameToType;
  protected Map<String, String> identifiers = new HashMap<>();
  protected Map<String, String> identifierOrigins = new HashMap<>();

  public SQLSchemaGenerator(GeneratorSetup generatorSetup) {
    this.generatorSetup = generatorSetup;
    this.generatorEngine = new GeneratorEngine(generatorSetup);
    initClassNameToType();
    hashedIdentifiers.clear();
    hashedColumns.clear();
  }

  public static String convertAttributeName(String name, AttributeType type) {
    return type.format(resolveIdentifier(attributeName(name), hashedColumns));
  }

  public static String convertClassName(String name, AttributeType type) {
    return type.format(resolveIdentifier(className(name), hashedIdentifiers));
  }

  // not truncated since the DAO templates append "_id" themselves
  public static String convertAssociationRollName(String name) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
  }

  public static String convertAssocName(String name) {
    return resolveIdentifier(assocName(name), hashedIdentifiers);
  }

  public static String convertListAttributeTableName(String className, String attributeName) {
    return resolveIdentifier(listAttributeTableName(className, attributeName), hashedIdentifiers);
  }

  // PostgreSQL silently truncates identifiers to 63 bytes, we truncate the same way
  public static String truncateIdentifier(String identifier) {
    return truncate(identifier, MAX_IDENTIFIER_BYTES);
  }

  // non-colliding identifiers must stay exactly as PostgreSQL truncates them, deployed databases already use these names
  protected static String resolveIdentifier(String identifier, Set<String> hashed) {
    if (!hashed.contains(identifier)) {
      return truncateIdentifier(identifier);
    }
    String hash = String.format("_%08x", identifier.hashCode());
    return truncate(identifier, MAX_IDENTIFIER_BYTES - hash.length()) + hash;
  }

  private static String truncate(String identifier, int maxBytes) {
    byte[] bytes = identifier.getBytes(StandardCharsets.UTF_8);
    if (bytes.length <= maxBytes) {
      return identifier;
    }
    int end = maxBytes;
    // do not cut a multibyte character
    while ((bytes[end] & 0xC0) == 0x80) {
      end--;
    }
    return new String(bytes, 0, end, StandardCharsets.UTF_8);
  }

  private static String attributeName(String name) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
  }

  private static String className(String name) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
  }

  private static String assocName(String name) {
    String res = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    res = res.replaceAll("\\s", "_");
    res = res.replaceAll("_+", "_");
    res = res.replaceAll("_$", "");
    return "assoc_" + res;
  }

  private static String listAttributeTableName(String className, String attributeName) {
    return className(className) + "$" + attributeName(attributeName);
  }

  protected void initClassNameToType() {
    fqnClassNameToType = new LinkedHashMap<>();
    fqnClassNameToType.put("java.lang.Enum", "VARCHAR");
    fqnClassNameToType.put("Enum", "VARCHAR");
    fqnClassNameToType.put("java.lang.String", "VARCHAR");
    fqnClassNameToType.put("String", "VARCHAR");
    fqnClassNameToType.put("java.lang.Byte", "SMALLINT");
    fqnClassNameToType.put("Byte", "SMALLINT");
    fqnClassNameToType.put("byte", "SMALLINT");
    fqnClassNameToType.put("java.lang.Short", "SMALLINT");
    fqnClassNameToType.put("Short", "SMALLINT");
    fqnClassNameToType.put("short", "SMALLINT");
    fqnClassNameToType.put("java.lang.Integer", "INTEGER");
    fqnClassNameToType.put("Integer", "INTEGER");
    fqnClassNameToType.put("int", "INTEGER");
    fqnClassNameToType.put("java.lang.Long", "BIGINT");
    fqnClassNameToType.put("Long", "BIGINT");
    fqnClassNameToType.put("long", "BIGINT");
    fqnClassNameToType.put("java.lang.Float", "REAL");
    fqnClassNameToType.put("Float", "REAL");
    fqnClassNameToType.put("float", "REAL");
    fqnClassNameToType.put("java.lang.Double", "DOUBLE PRECISION");
    fqnClassNameToType.put("Double", "DOUBLE PRECISION");
    fqnClassNameToType.put("double", "DOUBLE PRECISION");
    fqnClassNameToType.put("java.lang.Boolean", "BOOLEAN");
    fqnClassNameToType.put("Boolean", "BOOLEAN");
    fqnClassNameToType.put("boolean", "BOOLEAN");
    fqnClassNameToType.put("java.lang.Char", "CHAR");
    fqnClassNameToType.put("Char", "CHAR");
    fqnClassNameToType.put("char", "CHAR");

    fqnClassNameToType.put("java.time.LocalDate", "DATE");
    fqnClassNameToType.put("java.time.LocalDateTime", "TIMESTAMP");
    fqnClassNameToType.put("java.time.LocalTime", "TIME(6) WITHOUT TIME ZONE");
  }

  public void addClassToSQLTypeMapping(String fqnClassName, String sqlType) {
    fqnClassNameToType.put(fqnClassName, sqlType);
  }

  public void generate(ASTCDCompilationUnit cdAST) {
    String templateName = "umlp.sql.Schema";
    generatorEngine.generate(templateName, Paths.get("resources", cdAST.getCDDefinition().getName() + ".sql"), cdAST, generateContent(cdAST));
  }

  // dry run that only detects colliding identifiers. has to run before the DAO decorators use the convert methods
  // and after all addClassToSQLTypeMapping calls (see UMLP2Java.ftl)
  public void initIdentifiers(ASTCDCompilationUnit cd) {
    generateStatements(cd, new IndentPrinter());
  }

  protected String generateContent(ASTCDCompilationUnit cd) {
    // otherwise the first of two colliding identifiers would be printed without hash
    initIdentifiers(cd);
    IndentPrinter p = new IndentPrinter();
    generateStatements(cd, p);
    return p.getContent();
  }

  protected void generateStatements(ASTCDCompilationUnit cd, IndentPrinter p) {
    identifiers.clear();
    identifierOrigins.clear();

    generateSQLSchemasForAllExtendedCDs(cd, p);

    generateTypeSchemas(cd.getCDDefinition(), p);
    for (ASTCDAssociation association : cd.getCDDefinition().getCDAssociationsList()) {
      if (!association.getModifier().isDerived()) {
        generateAssociationSchema(cd.getCDDefinition(), association, p);
      }
    }
  }

  protected void generateSQLSchemasForAllExtendedCDs(ASTCDCompilationUnit cd, IndentPrinter p) {
    UMLPSymbolTables.getExtendedCDs(cd).forEach(extendedCD -> {
      ASTCDDefinition extendedDefinition = (ASTCDDefinition) extendedCD.getAstNode();
      generateTypeSchemas(extendedDefinition, p);
      for (ASTCDAssociation association : extendedDefinition.getCDAssociationsList()) {
        if (!association.getModifier().isDerived()) {
          generateAssociationSchema(extendedDefinition, association, p);
        }
      }
    });
  }

  protected void generateTypeSchemas(ASTCDDefinition cd, IndentPrinter p) {
    for (ASTCDClass node : cd.getCDClassesList()) {
      generateClassSchema(cd, node, p);
    }
    // List attributes are saved in a table named after the defining type
    // thus we also need to take interfaces into account here
    for (ASTCDInterface node : cd.getCDInterfacesList()) {
      generateListAttributeTables(cd, node, p);
    }
  }

  public void generateClassSchema(ASTCDDefinition cd, ASTCDClass node, IndentPrinter p) {
    p.print("CREATE TABLE ");
    String tableName = className(node.getName());
    String origin = "class " + cd.getName() + "." + node.getName();
    p.print(AttributeType.QUOTED.format(checkIdentifier("", tableName, origin)));
    p.println("(");
    p.indent();

    List<ASTCDAttribute> relevantAttributes = CDSymbolTables.getAttributesInHierarchy(node)
        .stream()
        .filter(a -> !a.getModifier().isStatic())
        .filter(a -> !a.getModifier().isDerived())
        .collect(Collectors.toList());

    // printed by generateIdField
    checkIdentifier("", "pk_" + tableName, origin);
    generateIdField(tableName, p);
    for (ASTCDAttribute attribute : relevantAttributes) {
      Optional<? extends ISymbol> typeSym = attribute.getMCType().getDefiningSymbol();
      if (!GenService.isList(attribute)
          && !GenService.isOptional(attribute)) {
        p.print(",");
        generateSimpleField(cd, node, attribute, p);
      }
      if(GenService.isOptional(attribute)) {
        p.print(",");
        generateOptionalField(cd, node, attribute, p);
      }
    }

    p.unindent();
    p.println(");");

    generateListAttributeTables(cd, node, p);
  }

  private void generateListAttributeTables(ASTCDDefinition cd, ASTCDType node, IndentPrinter p) {
    // Only create List Attribute Tables once for inheritance chain
    List<ASTCDAttribute> relevantListAttributes = node.getCDAttributeList()
        .stream()
        .filter(a -> !a.getModifier().isStatic())
        .filter(a -> !a.getModifier().isDerived())
        .filter(GenService::isList)
        .collect(Collectors.toList());


    for (ASTCDAttribute attribute : relevantListAttributes) {
      generateListFieldTable(cd, node, attribute, p);
      if (attribute.getMCType().getDefiningSymbol().isEmpty()) {
        generateListFieldTable(cd, node, attribute, p);
      }
    }
  }

  protected void generateListFieldTable(ASTCDDefinition cd, ASTCDType typeNode, ASTCDAttribute attribute, IndentPrinter p) {
    p.print("CREATE TABLE ");
    String tableName = listAttributeTableName(typeNode.getName(), attribute.getName());
    String origin = "attribute " + cd.getName() + "." + typeNode.getName() + "." + attribute.getName();
    p.print(checkIdentifier("", tableName, origin));
    p.println("(");
    p.indent();

    String r1 = "id";
    String r2 = "_value";
    String r3 = "index";

    p.print(r1);
    p.println(" BIGINT NOT NULL,");

    p.print(r2);
    p.print(" ");
    String type = GenService.isInnerCDEnum(attribute.getSymbol().getType()) ?
        "java.lang.Enum" : ((SymTypeOfGenerics) attribute.getSymbol().getType()).getArgument(0).printFullName();
    if (!fqnClassNameToType.containsKey(type)) {
      Log.error("0xdf10c Can not convert type to SQL: " + type + " used in " + cd.getName() + "." + typeNode.getName() + "." + attribute.getName());
    }

    p.print(fqnClassNameToType.get(type));
    p.println(" NOT NULL,");

    p.print(r3);
    p.println(" BIGINT NOT NULL,");

    p.println("CONSTRAINT " + checkIdentifier("", "pk_" + tableName, origin) + " PRIMARY KEY (" + r1 + "," + r3 + ")");

    p.unindent();
    p.println(");");
  }

  protected void generateIdField(String tableName, IndentPrinter p) {
    p.println("id BIGINT NOT NULL,");
    p.println("CONSTRAINT " + resolveIdentifier("pk_" + tableName, hashedIdentifiers) + " PRIMARY KEY (id)");
  }

  protected void generateOptionalField(ASTCDDefinition cd, ASTCDClass classNode, ASTCDAttribute attribute, IndentPrinter p) {
    String origin = "attribute " + cd.getName() + "." + classNode.getName() + "." + attribute.getName();
    p.print(AttributeType.QUOTED.format(checkIdentifier(className(classNode.getName()), attributeName(attribute.getName()), origin)));
    p.print(" ");
    Optional<? extends ISymbol> typeSymbolOpt = attribute.getMCType().getDefiningSymbol();
    if (typeSymbolOpt.isEmpty()) {
      Log.error("0xdf10d Can not find symbol for type of attribute " + cd.getName() + "." + classNode.getName() + "." + attribute.getName());
      return;
    }

    String fullName;
    if(GenService.isCDEnum(attribute.getSymbol().getType()) || GenService.isInnerCDEnum(attribute.getSymbol().getType())) {
      fullName = "java.lang.Enum";
    } else if(GenService.isOptional(attribute)) {
      fullName = ((SymTypeOfGenerics) attribute.getSymbol().getType()).getArgument(0).printFullName();
    } else {
      fullName = typeSymbolOpt.get().getFullName();
    }
    if (!fqnClassNameToType.containsKey(fullName)) {
      Log.error("0xdf10e Can not transform type to SQL: " + fullName + " used in " + cd.getName() + "." + classNode.getName() + "." + attribute.getName());
    }
    p.print(fqnClassNameToType.get(fullName));
  }

  protected void generateSimpleField(ASTCDDefinition cd, ASTCDClass classNode, ASTCDAttribute attribute, IndentPrinter p) {
    generateOptionalField(cd, classNode, attribute, p);

    p.println(" NOT NULL");
  }

  protected void generateAssociationSchema(ASTCDDefinition cd, ASTCDAssociation association, IndentPrinter p) {
    Optional<CDTypeSymbol> lSymbolOpt = cd.getEnclosingScope().resolveCDType(association.getLeft().getMCQualifiedType().getMCQualifiedName().getQName());
    Optional<CDTypeSymbol> rSymbolOpt = cd.getEnclosingScope().resolveCDType(association.getRight().getMCQualifiedType().getMCQualifiedName().getQName());
    if (lSymbolOpt.isEmpty()) {
      UMLPFullPrettyPrinter prettyPrinter = new UMLPFullPrettyPrinter();
      Log.error("0xdf10a Can not find type " +
          prettyPrinter.prettyprint(association.getLeft().getMCQualifiedType())
          + " for association "
          + prettyPrinter.prettyprint((ASTCDBasisNode) association)
      );
      return;
    }

    if (rSymbolOpt.isEmpty()) {
      UMLPFullPrettyPrinter prettyPrinter = new UMLPFullPrettyPrinter();
      Log.error("0xdf10b Can not find type " +
          prettyPrinter.prettyprint(association.getRight().getMCQualifiedType())
          + " for association "
          + prettyPrinter.prettyprint((ASTCDBasisNode) association)
      );
      return;
    }

    if (lSymbolOpt.get().isIsDerived()) {
      UMLPFullPrettyPrinter prettyPrinter = new UMLPFullPrettyPrinter();
      Log.info("Skipping generation of association " + prettyPrinter.prettyprint((ASTCDBasisNode) association) + " since the left class is derived", "SQLSchemaGenerator");
      return;
    }


    if (rSymbolOpt.get().isIsDerived()) {
      UMLPFullPrettyPrinter prettyPrinter = new UMLPFullPrettyPrinter();
      Log.info("Skipping generation of association" + prettyPrinter.prettyprint((ASTCDBasisNode) association) + " since the right class is derived", "SQLSchemaGenerator");
      return;
    }


    p.print("CREATE TABLE ");
    String tableName = assocName(association.getName());
    String origin = "association " + cd.getName() + "." + association.getName();
    p.print(checkIdentifier("", tableName, origin));
    p.println("(");
    p.indent();

    // TODO: use roll names: Person -> Person
    String r1 = convertAssociationRollName(association.getLeft().getName());
    String r2 = convertAssociationRollName(association.getRight().getName());
    // only checked, printed untruncated like in the DAO templates
    checkIdentifier(tableName, r1 + "_id", origin);
    checkIdentifier(tableName, r2 + "_id", origin);

    p.print(r1 + "_id");
    p.println(" BIGINT NOT NULL,");
    // TODO: different foreign key for each non-abstract, non-derived subclass
    // TODO: Convert fqn?

    //p.println("CONSTRAINT fk_" + r1 + "_" + tableName + " FOREIGN KEY (" + r1 + "_id) REFERENCES " + convertClassName(lSymbolOpt.get().getName(), AttributeType.QUOTED) + ",");

    p.print(r2 + "_id");
    p.println(" BIGINT NOT NULL,");

    //p.println("CONSTRAINT fk_" + r2 + "_" + tableName + " FOREIGN KEY (" + r2 + "_id) REFERENCES " + convertClassName(rSymbolOpt.get().getName(), AttributeType.QUOTED) + ",");

    p.println("CONSTRAINT " + checkIdentifier("", "pk_" + tableName, origin) + " PRIMARY KEY (" + r1 + "_id, " + r2 + "_id)");

    p.unindent();
    p.println(");");
  }

  // every identifier of the schema has to pass through here, otherwise its collisions go unnoticed.
  // namespace "" holds tables and constraints, a table name holds the columns of that table
  protected String checkIdentifier(String namespace, String identifier, String origin) {
    Set<String> hashed = namespace.isEmpty() ? hashedIdentifiers : hashedColumns;
    String truncated = truncateIdentifier(identifier);
    String key = namespace + "." + truncated;
    String previous = identifiers.putIfAbsent(key, identifier);
    if (previous == null) {
      identifierOrigins.put(key, origin);
    } else if (!previous.equals(identifier) && !hashed.contains(identifier)) {
      hashed.add(previous);
      hashed.add(identifier);
      Log.warn("0xdf10f The SQL identifiers " + previous + " (" + identifierOrigins.get(key) + ") and "
          + identifier + " (" + origin + ") are both truncated to " + truncated + " by PostgreSQL's limit of "
          + MAX_IDENTIFIER_BYTES + " bytes, so a hash is appended to them. Consider shortening one of the names, "
          + "e.g. by an explicit association name in the CD.");
    }
    return resolveIdentifier(identifier, hashed);
  }

  public enum AttributeType {
    PLAIN(""),
    QUOTED("\""),
    ESCAPED_QUOTED("\\\"");

    private final String modifier;

    AttributeType(String modifier) {
      this.modifier = modifier;
    }


    String format(String s) {
      return modifier + s + modifier;
    }
  }

}
