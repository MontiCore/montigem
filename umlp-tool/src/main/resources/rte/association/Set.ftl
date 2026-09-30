${tc.signature("assoc")}
<#import "rte.association.Links" as links>
<#assign name=assoc.getName() type=genService.getCorrectJavaFQN(assoc.getSymbol().getType().printFullName()) optional=genService.isOptional(assoc)>
<#assign parameterType=optional?then("Optional<" + type + ">", type)>
${cd4c.method("public void set${name?cap_first}(${parameterType} value)")}
<#if optional>
Objects.requireNonNull(value);
${type} previous = this._${name}.orElse(null);
${type} next = value.orElse(null);
<#else>
${type} previous = this._${name};
${type} next = value;
</#if>
if (previous == next) {
  return;
}
this._${name} = value;
if (previous != null) {
  <@links.detach assoc "previous"/>
}
if (next != null) {
  <@links.attach assoc "next"/>
}
