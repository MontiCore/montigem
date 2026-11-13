<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "hasSuperClass")}
${cd4c.method("public void removeObserver(${name}Observer o)")}

Log.debug("Removing observer "+o.getClass().getName() + " on ${name} with GemId "+this.getGemId(), this.getClass().getName());
<#if hasSuperClass>
  // addObserver registers with the superclass, too
  super.removeObserver(o);
</#if>
${name?uncap_first}Observers.remove(o);
