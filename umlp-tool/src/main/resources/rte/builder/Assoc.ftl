${tc.signature("name", "type", "builder", "isList")}
${cd4c.method("public ${builder} ${name}(${type} ${name})")}
this.set${name?cap_first}${isList?then("List", "")}(${name});
return this.realBuilder;
