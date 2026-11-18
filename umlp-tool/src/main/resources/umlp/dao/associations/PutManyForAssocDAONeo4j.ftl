<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("association", "ownerName", "keyType")}
${cd4c.method("public void putMany${association.getName()?cap_first}(long ${ownerName}GemId, Map<${keyType}, Long> ids)")}
  ensurePut${association.getName()?cap_first}Created();
  for (Map.Entry<${keyType}, Long> entry : ids.entrySet()) {
    DatabaseAccessManager4Neo4j.executeWrite(put${association.getName()?cap_first},
      Values.parameters("owner", ${ownerName}GemId, "value", entry.getValue(), "key", entry.getKey()));
  }
