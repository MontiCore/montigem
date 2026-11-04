<#-- (c) https://github.com/MontiCore/monticore -->
<#-- type qualified association: the relationship from the owner to the value (named after the value role) carries the
     key as property; MERGE keeps one relationship per pair and updates its key -->
${tc.signature("assocName", "ownerType", "valueType", "valueRole", "keyProperty")}
${cd4c.method("public void ensurePut${assocName?cap_first}Created()")}

    put${assocName?cap_first} = "MATCH (o:${ownerType} {gemId: $owner}) <#t>
    <#lt> MATCH (v:${valueType} {gemId: $value}) <#t>
    <#lt> MERGE (o)-[rel:${valueRole}]->(v) <#t>
    <#lt> SET rel.${keyProperty} = $key";
