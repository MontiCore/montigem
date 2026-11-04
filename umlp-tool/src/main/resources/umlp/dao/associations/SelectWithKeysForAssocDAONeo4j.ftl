<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyExpression: converts the key property of the record in variable record to the key type -->
${tc.signature("oneSideName", "otherSide", "keyType", "keyExpression")}
<#assign methodName = otherSide?cap_first+"IdsWithKeysFor"+oneSideName?cap_first>
${cd4c.method("public Map<${keyType}, Long> select${methodName}(long ${oneSideName}GemId)")}
ensureSelect${methodName?cap_first}Created();

try(Session session = DatabaseAccessManager4Neo4j.getSession()) {
  Result rs = session.run(select${methodName}, Values.parameters("p1", ${oneSideName}GemId));
  Map<${keyType}, Long> res = new java.util.LinkedHashMap<>();

  while(rs.hasNext()){
    org.neo4j.driver.Record record = rs.next();
    res.put(${keyExpression}, record.get(0).asLong());
  }
  return res;
}
