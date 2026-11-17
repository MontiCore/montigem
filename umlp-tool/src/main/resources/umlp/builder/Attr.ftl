<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "type", "builder", "isList", "isMap")}
${cd4c.method("public ${builder} ${name}(${type} ${name})")}

<#if isList>
this._${name} = new ArrayList(${name});
<#elseif isMap>
this._${name} = new LinkedHashMap<>(${name});
<#else>
this._${name} = ${name};
</#if>
return this.realBuilder;
