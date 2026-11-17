<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "attributes")}
<#macro constructorParams attributes>
    <#list attributes as attr>
        ${attr.getMCType().printType()} ${attr.name}<#if !attr?is_last>,</#if>
    </#list>
</#macro>
<#assign params><@constructorParams attributes/></#assign>
<#macro constructorParamNames attributes>
    <#list attributes as attr>
        ${attr.name}<#if !attr?is_last>, </#if>
    </#list>
</#macro>
<#assign paramNames><@constructorParamNames attributes/></#assign>

${cd4c.constructor("public ${name}(${params})")}

<#list attributes as attr>
  <#if genService.isList(attr)>
  this.set${attr.name?remove_beginning("_")?cap_first}List(${attr.name});
  <#else>
  this.set${attr.name?remove_beginning("_")?cap_first}(${attr.name});
  </#if>
</#list>
