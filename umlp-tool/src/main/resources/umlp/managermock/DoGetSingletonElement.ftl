<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyObservers: the key observers of the map roles that may hold objects of the class (GemMapKeyRole) -->
${tc.signature("name", "keyObservers")}
${cd4c.method("protected ${name} _doGet${name}()")}
if (${name?uncap_first}Instance.isEmpty()) {
  // Construct a new instance of this singleton
  // We do not respect the dummyProvider, as only one singleton is allowed
  ${name}Builder b = ${name?uncap_first}BuilderFromId(getFreeGemId());
  ${name?uncap_first}Instance = b.build();
  if (${name?uncap_first}Instance.isEmpty()) {
    throw new RuntimeException("Unable to create a new instance for ${name} - override its builder");
  }
  if (!${name?uncap_first}Instance.get().containsObserver(the${name}CommandObserver)) {
    ${name?uncap_first}Instance.get().addObserver(the${name}CommandObserver);
  }
<#list keyObservers as keyObserver>
  if (!${name?uncap_first}Instance.get().containsObserver(${keyObserver})) {
    ${name?uncap_first}Instance.get().addObserver(${keyObserver});
  }
</#list>
};
return ${name?uncap_first}Instance.get();
