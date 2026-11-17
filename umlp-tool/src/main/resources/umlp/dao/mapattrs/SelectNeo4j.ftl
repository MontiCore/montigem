<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the entries from the node properties <Attr>_keys and <Attr>_values -->
${tc.signature("typeName", "name", "keyType", "valueType", "keySymType", "valueSymType")}
<#assign attrName=name?cap_first>
${cd4c.method("public Map<${keyType}, ${valueType}> select${attrName}(long gemId)")}
if (select${attrName}Statement == null) {
  select${attrName}Statement = "MATCH(n:${typeName} {gemId: $p1}) "
      + "UNWIND range(0, size(coalesce(n.${attrName}_keys, [])) - 1) AS i "
      + "RETURN n.${attrName}_keys[i], n.${attrName}_values[i]";
}
try (Session session = DatabaseAccessManager4Neo4j.getSession()) {
  Result rs = session.run(select${attrName}Statement, Values.parameters("p1", gemId));
  Map<${keyType}, ${valueType}> res = new LinkedHashMap<>();
  while (rs.hasNext()) {
    org.neo4j.driver.Record rd = rs.next();
    res.put(${includeArgs("umlp.dao.mapattrs.GetValue", "rd", keySymType, 0, "_Neo4j")},
        ${includeArgs("umlp.dao.mapattrs.GetValue", "rd", valueSymType, 1, "_Neo4j")});
  }
  return res;
}
