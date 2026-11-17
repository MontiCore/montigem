<#-- (c) https://github.com/MontiCore/monticore -->
<#-- writes the whole map to the node properties <Attr>_keys and <Attr>_values, the keys and values converted like
     single attributes (dao/SetNeo4j) -->
${tc.signature("typeName", "name", "keyType", "valueType", "keyDbType", "valueDbType")}
<#assign attrName=name?cap_first>
${cd4c.method("protected void write${attrName}(long gemId, Map<${keyType}, ${valueType}> map)")}
if (write${attrName}Statement == null) {
  write${attrName}Statement = "MATCH(n:${typeName} {gemId: $p1}) "
      + "SET n.${attrName}_keys = $p2, n.${attrName}_values = $p3";
}
List<Object> keys = new ArrayList<>();
List<Object> values = new ArrayList<>();
for (Map.Entry<${keyType}, ${valueType}> entry : map.entrySet()) {
  org.neo4j.driver.Value entryValue = Values.parameters(
      ${includeArgs("umlp.dao.SetNeo4j", "", "key", keyDbType, "entry.getKey()", false, "_Neo4j")},
      ${includeArgs("umlp.dao.SetNeo4j", "", "value", valueDbType, "entry.getValue()", false, "_Neo4j")});
  keys.add(entryValue.get("key"));
  values.add(entryValue.get("value"));
}
DatabaseAccessManager4Neo4j.executeWrite(write${attrName}Statement,
    Values.parameters("p1", gemId, "p2", keys, "p3", values));
