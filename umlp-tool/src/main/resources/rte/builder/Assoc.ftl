${tc.signature("name", "type", "builder", "isList")}
${cd4c.method("public ${builder} ${name}(${type} ${name})")}
<#if isList>
this.set${name?cap_first}List(${name});
<#else>
this.set${name?cap_first}(${name});
</#if>
return this.realBuilder;
