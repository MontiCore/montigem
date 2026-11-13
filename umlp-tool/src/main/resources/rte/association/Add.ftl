${tc.signature("assoc")}
<#assign name=assoc.getName() type=genService.getCorrectJavaFQN(assoc.getSymbol().getType().printFullName())>
${cd4c.method("public boolean add${name?cap_first}(${type} element)")}
Objects.requireNonNull(element);
if (this._${name}.contains(element)) {
  return false;
}
add${name?cap_first}(this._${name}.size(), element);
return true;
