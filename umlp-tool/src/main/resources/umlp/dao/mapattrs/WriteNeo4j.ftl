<#-- (c) https://github.com/MontiCore/monticore -->
<#-- writes the whole map to the node properties <Attr>_keys and <Attr>_values; Instant as OffsetDateTime in UTC, as
     the Neo4j driver has no Instant -->
${tc.signature("typeName", "name", "keyType", "valueType", "valueIsEnum", "keyIsInstant", "valueIsInstant")}
<#assign attrName=name?cap_first>
${cd4c.method("protected void write${attrName}(long gemId, Map<${keyType}, ${valueType}> map)")}
if (write${attrName}Statement == null) {
  write${attrName}Statement = "MATCH(n:${typeName} {gemId: $p1}) "
      + "SET n.${attrName}_keys = $p2, n.${attrName}_values = $p3";
}
List<Object> keys = new ArrayList<>();
List<Object> values = new ArrayList<>();
for (Map.Entry<${keyType}, ${valueType}> entry : map.entrySet()) {
  keys.add(entry.getKey()<#if keyIsInstant>.atOffset(java.time.ZoneOffset.UTC)</#if>);
  values.add(entry.getValue()<#if valueIsEnum>.name()<#elseif valueIsInstant>.atOffset(java.time.ZoneOffset.UTC)</#if>);
}
DatabaseAccessManager4Neo4j.executeWrite(write${attrName}Statement,
    Values.parameters("p1", gemId, "p2", keys, "p3", values));
