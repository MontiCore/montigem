<#-- (c) https://github.com/MontiCore/monticore -->
<#-- ownerIsLeft: whether the owner (the qualified side) is the left side of the association -->
${tc.signature("association", "keyType", "ownerIsLeft")}
${cd4c.method("public void put${association.getName()?cap_first}(long ${association.getLeft().getName()}GemId, long ${association.getRight().getName()}GemId, ${keyType} key)")}
  ensurePut${association.getName()?cap_first}Created();
  DatabaseAccessManager4Neo4j.executeWrite(put${association.getName()?cap_first},
    Values.parameters(
<#if ownerIsLeft>
      "owner", ${association.getLeft().getName()}GemId,
      "value", ${association.getRight().getName()}GemId,
<#else>
      "owner", ${association.getRight().getName()}GemId,
      "value", ${association.getLeft().getName()}GemId,
</#if>
      "key", key
    )
  );
