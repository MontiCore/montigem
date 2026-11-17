<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyDbType: the type dao/SetNeo4j stores the key as (QualifierKey#getDbType) -->
${tc.signature("association", "ownerName", "keyType", "keyDbType")}
${cd4c.method("public void putMany${association.getName()?cap_first}(long ${ownerName}GemId, Map<${keyType}, Long> ids)")}
  ensurePut${association.getName()?cap_first}Created();
  for (Map.Entry<${keyType}, Long> entry : ids.entrySet()) {
    DatabaseAccessManager4Neo4j.executeWrite(put${association.getName()?cap_first},
      Values.parameters("owner", ${ownerName}GemId, "value", entry.getValue(),
          ${includeArgs("umlp.dao.SetNeo4j", "", "key", keyDbType, "entry.getKey()", false, "_Neo4j")}));
  }
