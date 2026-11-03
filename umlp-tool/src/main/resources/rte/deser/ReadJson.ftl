${tc.signature("className", "fields")}
${cd4c.method("public void readJson(${className} obj, JsonObject json, PojoGraphContext context)")}
${className}Builder builder = (${className}Builder) obj;
<#list fields as field>
builder.${field}(deserialize${field?cap_first}WithContext(json, context));
</#list>
