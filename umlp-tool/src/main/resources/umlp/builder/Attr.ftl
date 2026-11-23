<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "type", "builder", "isList", "isMap", "keyType", "valueType")}
${cd4c.method("public ${builder} ${name}(${type} ${name})")}

<#if isList>
this._${name} = new ArrayList(${name});
<#elseif isMap>
<#-- entry by entry, with the checks of ${name}Put (null, unsafe long and sub-microsecond keys) -->
this._${name} = new LinkedHashMap<>();
for (Map.Entry<${keyType}, ${valueType}> entry : ${name}.entrySet()) {
  ${name}Put(entry.getKey(), entry.getValue());
}
<#else>
this._${name} = ${name};
</#if>
return this.realBuilder;
