<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("oneSideName", "otherSideName", "oneSideType", "keyProperty")}
<#assign methodName = otherSideName?cap_first+"IdsWithKeysFor"+oneSideName?cap_first>
${cd4c.method("public void ensureSelect${methodName}Created()")}

    select${methodName} = "MATCH (n:${oneSideType} {gemId: $p1}) <#t>
    <#lt>-[rel:${otherSideName}]->(m) RETURN m.gemId, rel.${keyProperty}";
