<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyObservers: the key observers of the map roles that may hold objects of the class (GemMapKeyRole) -->
${tc.signature("name", "keyObservers")}
${cd4c.method("protected ${name} _doRegister${name}(${name} obj)")}

if (${name?uncap_first}s.containsKey(obj.getGemId())) {
  return ${name?uncap_first}s.get(obj.getGemId());
}
if (!(obj).containsObserver(the${name}CommandObserver)) {
  (obj).addObserver(the${name}CommandObserver);
}
<#list keyObservers as keyObserver>
if (!(obj).containsObserver(${keyObserver})) {
  (obj).addObserver(${keyObserver});
}
</#list>
${name?uncap_first}s.put(obj.getGemId(), obj);
return obj;
