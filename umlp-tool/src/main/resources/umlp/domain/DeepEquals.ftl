<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("className", "gemAssocs", "listAssocs", "mapAssocs")}
${cd4c.method("public boolean TESTING_deepEquals(Object o)")}
if(this == o){
  return true;
}

if (o instanceof ${className?cap_first}) {
${className?cap_first} other = (${className?cap_first}) o;

return this.equals(other) &&
  <#list gemAssocs as gemAssoc>
  get${gemAssoc.getName()?cap_first}Gem().isPresent() == other.get${gemAssoc.getName()?cap_first}Gem().isPresent() &&
  (!get${gemAssoc.getName()?cap_first}Gem().isPresent() || get${gemAssoc.getName()?cap_first}().TESTING_deepEquals(other.get${gemAssoc.getName()?cap_first}())) &&
  </#list>
  <#list listAssocs as gemListAssoc>
  _internal_get${gemListAssoc.getName()?cap_first}GemList().TESTING_deepEquals(other. _internal_get${gemListAssoc.getName()?cap_first}GemList()) &&
  </#list>
  <#list mapAssocs as gemMapAssoc>
  _internal_get${gemMapAssoc.getName()?cap_first}GemMap().getIDMap().equals(other._internal_get${gemMapAssoc.getName()?cap_first}GemMap().getIDMap()) &&
  </#list>
  true;
}
return false;
