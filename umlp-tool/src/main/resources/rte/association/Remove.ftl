${tc.signature("assoc")}
<#import "rte.association.Links" as links>
<#assign name=assoc.getName() type=genService.getCorrectJavaFQN(assoc.getSymbol().getType().printFullName())>
${cd4c.method("public boolean remove${name?cap_first}(${type} element)")}
if (!this._${name}.remove(element)) {
  return false;
}
<@links.detach assoc "element"/>
return true;
