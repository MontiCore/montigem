<#-- (c) https://github.com/MontiCore/monticore -->
<#-- commands of a GemMap (put, rekey), see GemMapCommand_BuildJson; keyJson reads a key from a JsonElement;
     hasOldValue/hasOldOwner: the command names the replaced value / the owner the value left (-1 if absent) -->
${tc.signature("command", "valueIdName", "keyFields", "keyJson", "hasOldValue", "hasOldOwner")}
${cd4c.method("protected Optional<${command}> fromJsonObject(JsonObject jsonObject)")}
  return Optional.of(new ${command}(
    jsonObject.getMember("gemId").getAsJsonNumber().getNumberAsLong(),
    jsonObject.getMember("${valueIdName}").getAsJsonNumber().getNumberAsLong()<#list keyFields as k>,
    jsonObject.hasMember("${k}") ? jsonObject.getMember("${k}").${keyJson} : null</#list><#if hasOldValue>,
    jsonObject.hasMember("oldValueGemId") ? jsonObject.getMember("oldValueGemId").getAsJsonNumber().getNumberAsLong() : -1</#if><#if hasOldOwner>,
    jsonObject.hasMember("oldOwnerGemId") ? jsonObject.getMember("oldOwnerGemId").getAsJsonNumber().getNumberAsLong() : -1</#if>
  ));
