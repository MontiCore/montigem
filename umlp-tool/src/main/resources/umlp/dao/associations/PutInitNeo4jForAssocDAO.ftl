<#-- (c) https://github.com/MontiCore/monticore -->
<#-- type qualified association: the relationship from the owner to the value (named after the value role) carries the
     key as property; MERGE keeps one relationship per pair and updates its key. otherSideRole: for a bidirectional
     association the role of the value side, whose relationship from the value to the owner is stored as well, as for
     other bidirectional associations (the value side reads it, see GenService#isOtherSideOfMap); empty otherwise -->
${tc.signature("assocName", "ownerType", "valueType", "valueRole", "keyProperty", "otherSideRole")}
${cd4c.method("public void ensurePut${assocName?cap_first}Created()")}

    put${assocName?cap_first} = "MATCH (o:${ownerType} {gemId: $owner}) <#t>
    <#lt> MATCH (v:${valueType} {gemId: $value}) <#t>
    <#lt> MERGE (o)-[rel:${valueRole}]->(v) <#t>
    <#lt> SET rel.${keyProperty} = $key<#if otherSideRole?has_content> MERGE (v)-[:${otherSideRole}]->(o)</#if>";
