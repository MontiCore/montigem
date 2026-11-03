<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyObservers: the key observers of the map roles that may hold objects of the class (GemMapKeyRole) -->
${tc.signature("name", "managerName", "keyObservers")}
${cd4c.method("protected void _doAddWithScope${name}(${name} o, NotificationScope scope)")}
  if(scope.contains(NotificationScope.GLOBAL)){
    StatusResult res = CommandManager.executeCommand(new ${name}_create(o)).asStatusResult();
  }
  // TODO: Error handling
  Log.trace("Adding known ${name?uncap_first} " + o.getGemId(), LogUtil.getLoggerName());
  known${name}s.put(o.getGemId(), o);
  if (this.loaded${name}List.isPresent()) {
    this.loaded${name}List.get().add(o);
  }
  if (!o.containsObserver(the${name}CommandObserver)) {
    o.addObserver(the${name}CommandObserver);
  }
<#list keyObservers as keyObserver>
  if (!o.containsObserver(${keyObserver})) {
    o.addObserver(${keyObserver});
  }
</#list>
  ${managerName?uncap_first}Observers.forEach(obs -> obs.maybeNotify${name}Added(o, scope));
