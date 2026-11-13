${tc.signature("name", "type")}
${cd4c.method("protected ${type} deserialize${name?cap_first}WithContext(JsonObject json, PojoGraphContext context)")}
return deserialize${name?cap_first}(json);
