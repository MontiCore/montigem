${tc.signature("assoc")}
<#assign name=assoc.getName() type=genService.getCorrectJavaFQN(assoc.getSymbol().getType().printFullName())>
${cd4c.method("public void set${name?cap_first}List(List<${type}> elements)")}
List<${type}> replacement = new ArrayList<>(Objects.requireNonNull(elements));
for (${type} element : replacement) {
  Objects.requireNonNull(element);
}
for (${type} element : new ArrayList<>(this._${name})) {
  remove${name?cap_first}(element);
}
for (${type} element : replacement) {
  add${name?cap_first}(element);
}
