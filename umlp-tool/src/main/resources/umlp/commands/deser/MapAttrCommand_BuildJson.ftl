<#-- (c) https://github.com/MontiCore/monticore -->
<#-- owner gemId, key and, for a put, the value; null is left out -->
${tc.signature("command", "hasValue", "valueGetter", "valueIsEnum")}
${cd4c.method("public void buildJson(${command} command, JsonPrinter jsonPrinter)")}
  super.buildJson(command, jsonPrinter);
  jsonPrinter.member("gemId", command.getGemId());
  if (command.getKey() != null) {
    jsonPrinter.member("key", command.getKey());
  }
<#if hasValue>
  if (command.${valueGetter}() != null) {
    jsonPrinter.member("value", command.${valueGetter}()<#if valueIsEnum>.name()</#if>);
  }
</#if>
