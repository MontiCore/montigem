<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyFields: the key fields of the command (put: key; rekey: oldKey, newKey), all of type keyType -->
${tc.signature("classname", "packageName", "valueIdName", "keyType", "keyFields")}
<#assign keyParams = "">
<#list keyFields as k><#assign keyParams = keyParams + ", " + keyType + " " + k></#list>
${cd4c.constructor("public ${classname}(long gemId, long ${valueIdName}${keyParams})")}
super("${packageName}.${classname}");
this.gemId = gemId;
this.${valueIdName} = ${valueIdName};
<#list keyFields as k>
this.${k} = ${k};
</#list>
