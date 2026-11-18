<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("className", "attributes", "assocs")}
${cd4c.method("public void buildJson(${className} obj, JsonPrinter jsonPrinter)")}
new PojoGraphContext().writeObject(obj, jsonPrinter, this);
