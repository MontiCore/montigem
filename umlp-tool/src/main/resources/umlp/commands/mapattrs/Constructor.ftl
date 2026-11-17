<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the command of an entry of a map attribute: put (key and value) or remove (key only) -->
${tc.signature("classname", "packageName", "keyType", "valueType", "hasValue")}
<#assign valueParam = hasValue?then(", " + valueType + " value", "")>
${cd4c.constructor("public ${classname}(long gemId, ${keyType} key${valueParam})")}
super("${packageName}.${classname}");
this.gemId = gemId;
this.key = key;
<#if hasValue>
this.value = value;
</#if>
