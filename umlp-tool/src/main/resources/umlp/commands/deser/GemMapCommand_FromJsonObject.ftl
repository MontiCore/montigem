<#-- (c) https://github.com/MontiCore/monticore -->
<#-- commands of a GemMap (put, rekey), see Put_BuildJson; keyJson reads a key from a JsonElement -->
${tc.signature("command", "valueIdName", "keyFields", "keyJson")}
${cd4c.method("protected Optional<${command}> fromJsonObject(JsonObject jsonObject)")}
  return Optional.of(new ${command}(
    jsonObject.getMember("gemId").getAsJsonNumber().getNumberAsLong(),
    jsonObject.getMember("${valueIdName}").getAsJsonNumber().getNumberAsLong()<#list keyFields as k>,
    jsonObject.hasMember("${k}") ? jsonObject.getMember("${k}").${keyJson} : null</#list>
  ));
