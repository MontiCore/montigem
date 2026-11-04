<#-- (c) https://github.com/MontiCore/monticore -->
<#-- removes the relationships of a type qualified association whose key is the object with keyGemId (deleted key
     object) -->
${tc.signature("association", "ownerType", "valueRole", "keyProperty")}
${cd4c.method("public void removeAllWithKey${association.getName()?cap_first}(long keyGemId)")}
  DatabaseAccessManager4Neo4j.executeWrite(
    "MATCH (:${ownerType})-[rel:${valueRole}]->() WHERE rel.${keyProperty} = $p1 DELETE rel",
    Values.parameters("p1", keyGemId));
