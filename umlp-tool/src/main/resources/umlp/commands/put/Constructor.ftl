<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyFields: the key fields of the command (put: key; rekey: oldKey, newKey), all of type keyType;
     hasOldOwner: the command names the owner the value left (bidirectional association with a single owner) -->
${tc.signature("classname", "packageName", "valueIdName", "keyType", "keyFields", "hasOldOwner")}
<#assign keyParams = "">
<#list keyFields as k><#assign keyParams = keyParams + ", " + keyType + " " + k></#list>
<#if hasOldOwner><#assign keyParams = keyParams + ", long oldOwnerGemId"></#if>
${cd4c.constructor("public ${classname}(long gemId, long ${valueIdName}${keyParams})")}
super("${packageName}.${classname}");
this.gemId = gemId;
this.${valueIdName} = ${valueIdName};
<#list keyFields as k>
this.${k} = ${k};
</#list>
<#if hasOldOwner>
this.oldOwnerGemId = oldOwnerGemId;
</#if>
