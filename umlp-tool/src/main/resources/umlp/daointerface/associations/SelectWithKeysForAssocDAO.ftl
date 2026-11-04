<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the values of an owner of a type qualified association by their keys -->
${tc.signature("oneSideName", "otherSide", "keyType")}
<#assign methodName = otherSide?cap_first+"IdsWithKeysFor"+oneSideName?cap_first>
${cd4c.method("public abstract Map<${keyType}, Long> select${methodName}(long ${oneSideName}GemId)")}
