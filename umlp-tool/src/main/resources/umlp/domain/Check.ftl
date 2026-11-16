<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("domain", "name", "reportError", "isAssocList")}
${cd4c.method("protected boolean check${name?cap_first}()")}

boolean res = false;
res=true;

<#if reportError>
  <#assign valueAccessor = isAssocList?then("_internal_get${name?cap_first}GemList()","_${name}")>
if(${valueAccessor} == null){
  res = false;
  ${domain}ErrorCollector.report${name?cap_first}Invalid((${domain}) this);
}
</#if>

return res;
