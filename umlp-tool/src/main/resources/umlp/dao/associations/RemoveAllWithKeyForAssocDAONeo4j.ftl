<#-- (c) https://github.com/MontiCore/monticore -->
<#-- removes the relationships of a type qualified association whose key is the object with keyGemId (deleted key
     object); mirrorRole: for a bidirectional association the role of the value side, whose relationship from the value
     to the owner goes as well (see PutInitNeo4jForAssocDAO), empty otherwise -->
${tc.signature("association", "ownerType", "valueRole", "keyProperty", "mirrorRole")}
${cd4c.method("public void removeAllWithKey${association.getName()?cap_first}(long keyGemId)")}
  DatabaseAccessManager4Neo4j.executeWrite(
<#if mirrorRole?has_content>
    "MATCH (o:${ownerType})-[rel:${valueRole}]->(v) WHERE rel.${keyProperty} = $p1 "
        + "OPTIONAL MATCH (v)-[back:${mirrorRole}]->(o) DELETE rel, back",
<#else>
    "MATCH (:${ownerType})-[rel:${valueRole}]->() WHERE rel.${keyProperty} = $p1 DELETE rel",
</#if>
    Values.parameters("p1", keyGemId));
