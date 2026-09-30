<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("cdName", "className", "simpleAttributes")}
${cd4c.method("public Optional<${className}> deserialize(JsonObject json)")}
return (Optional<${className}>) new PojoGraphContext().deserializeObject(json, this);
