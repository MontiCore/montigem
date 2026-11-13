<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyObservers: the key observers of the map roles that may hold objects of the class (GemMapKeyRole) -->
${tc.signature("cdName", "name", "keyObservers")}
${cd4c.method("protected void _doAddRequiredObserver${name}(${name} h)")}
  if(!h.containsObserver(the${name}DbObserver)){
    h.addObserver(the${name}DbObserver);
  }
  if(!h.containsObserver(the${name}CommandSendingObserver)){
    h.addObserver(the${name}CommandSendingObserver);
  }
<#list keyObservers as keyObserver>
  if(!h.containsObserver(${keyObserver})){
    h.addObserver(${keyObserver});
  }
</#list>
