<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the effected objects of a GemMap put command: gemIds always, optionalGemIds (fields that are -1 if there is no
     such object, e.g. no replaced value) only if set. A -1 would subscribe the receivers to -1 (CommandManager), so
     every later command with a -1 would reach them. ids is a Set: JSweet does not transpile add on a Collection typed
     variable (known bug) -->
${tc.signature("gemIds", "optionalGemIds")}
${cd4c.method("public Collection<Long> getEffectedGemIds()")}

  java.util.Set<Long> ids = Sets.newHashSet(<#list gemIds as gemId>${gemId}<#if gemId?has_next>, </#if></#list>);
<#list optionalGemIds as gemId>
  if (${gemId} != -1) {
    ids.add(${gemId});
  }
</#list>
  return ids;
