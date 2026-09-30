${tc.signature("className")}
${cd4c.method("public String serialize(${className} obj)")}
return new PojoGraphContext().serializeObject(obj, this);
