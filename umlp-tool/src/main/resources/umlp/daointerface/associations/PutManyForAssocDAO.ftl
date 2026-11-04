<#-- (c) https://github.com/MontiCore/monticore -->
<#-- stores the values of an owner of a type qualified association with their keys -->
${tc.signature("association", "ownerName", "keyType")}
${cd4c.method("public abstract void putMany${association.getName()?cap_first}(long ${ownerName}GemId, Map<${keyType}, Long> ids)")}
