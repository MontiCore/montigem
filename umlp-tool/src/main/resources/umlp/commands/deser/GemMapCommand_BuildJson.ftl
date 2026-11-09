<#-- (c) https://github.com/MontiCore/monticore -->
<#-- commands of a GemMap (put, rekey): owner gemId, value gemId and the key fields; hasOldValue/hasOldOwner: the
     replaced value / the owner the value left, if any -->
${tc.signature("command", "valueIdName", "keyFields", "hasOldValue", "hasOldOwner")}
${cd4c.method("public void buildJson(${command} command, JsonPrinter jsonPrinter)")}
  super.buildJson(command, jsonPrinter);
  jsonPrinter.member("gemId", command.getGemId());
  jsonPrinter.member("${valueIdName}", command.get${valueIdName?cap_first}());
<#list keyFields as k>
  if (command.get${k?cap_first}() != null) {
    jsonPrinter.member("${k}", command.get${k?cap_first}());
  }
</#list>
<#if hasOldValue>
  if (command.getOldValueGemId() != -1) {
    jsonPrinter.member("oldValueGemId", command.getOldValueGemId());
  }
</#if>
<#if hasOldOwner>
  if (command.getOldOwnerGemId() != -1) {
    jsonPrinter.member("oldOwnerGemId", command.getOldOwnerGemId());
  }
</#if>
