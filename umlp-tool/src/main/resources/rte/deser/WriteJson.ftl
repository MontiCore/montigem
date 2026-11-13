${tc.signature("className", "fields")}
${cd4c.method("public void writeJson(${className} obj, JsonPrinter printer, PojoGraphContext context)")}
<#list fields as field>
serialize${field?cap_first}WithContext(obj, printer, context);
</#list>
