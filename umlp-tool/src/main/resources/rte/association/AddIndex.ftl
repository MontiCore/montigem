${tc.signature("assoc")}
<#import "rte.association.Links" as links>
<#assign name=assoc.getName() type=genService.getCorrectJavaFQN(assoc.getSymbol().getType().printFullName())>
${cd4c.method("public void add${name?cap_first}(int index, ${type} element)")}
Objects.requireNonNull(element);
if (index < 0 || index > this._${name}.size()) {
  throw new IndexOutOfBoundsException();
}
if (this._${name}.contains(element)) {
  return;
}
this._${name}.add(index, element);
<@links.attach assoc "element"/>
