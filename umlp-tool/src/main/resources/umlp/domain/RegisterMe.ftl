<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("className", "managerName", "isAbstract", "isSingleton", "isPojo")}
<#if isAbstract>
${cd4c.method("public abstract ${className} _registerMe()")}
<#else>
${cd4c.method("public ${className} _registerMe()")}

<#if isSingleton || isPojo>
<#-- Singletons are not cached; nothing to register. Cast for the TOP mechanism. -->
<#-- Pojos are not cached; nothing to register. Cast for the TOP mechanism. -->
return (${className}) this;
<#else>
<#-- Register with the manager owning the concrete type (monticore#5062). Cast for TOP. -->
return ${managerName}.register${className}((${className}) this);
</#if>
</#if>
