<#-- (c) https://github.com/MontiCore/monticore -->
<#-- observedTypes: the types of values with a key observer, keyObserverPrefix: the name prefix of their classes -->
${tc.signature("type", "observedTypes", "keyObserverPrefix")}
${cd4c.constructor("public ${type}(long ownerGemID)")}
super(null);
this.ownerGemID = ownerGemID;
<#list observedTypes as observedType>
this.keyObserver${observedType} = new ${keyObserverPrefix}${observedType}KeyObserver(this);
</#list>
