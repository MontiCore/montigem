<#-- (c) https://github.com/MontiCore/monticore -->
<#-- owner gemId, key and, for a put, the value; null is left out -->
${tc.signature("command", "keyUseDeSer", "keyType", "hasValue", "valueGetter", "valueIsEnum", "useDeSer", "valueType",
  "objectMapperName")}
${cd4c.method("public void buildJson(${command} command, JsonPrinter jsonPrinter)")}
  super.buildJson(command, jsonPrinter);
  jsonPrinter.member("gemId", command.getGemId());
  if (command.getKey() != null) {
  <#if keyUseDeSer>
    jsonPrinter.memberJson("key", ${objectMapperName}.serialize(command.getKey(), "${keyType}").orElseThrow());
  <#else>
    jsonPrinter.member("key", command.getKey());
  </#if>
  }
<#if hasValue>
  if (command.${valueGetter}() != null) {
  <#if useDeSer>
    jsonPrinter.memberJson("value", ${objectMapperName}.serialize(command.${valueGetter}(), "${valueType}").orElseThrow());
  <#else>
    jsonPrinter.member("value", command.${valueGetter}()<#if valueIsEnum>.name()</#if>);
  </#if>
  }
</#if>
