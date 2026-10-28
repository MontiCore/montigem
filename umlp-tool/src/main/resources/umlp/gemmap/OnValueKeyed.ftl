<#-- (c) https://github.com/MontiCore/monticore -->
<#-- observedTypes: the target and its subtypes, most specific first;
     a value is observed with the observer of its most specific type, as subclasses notify their own observers only -->
${tc.signature("targetName", "observedTypes")}
${cd4c.method("protected void onValueKeyed(${targetName} value)")}
// rekey the value when its qualifying attribute changes
<#list observedTypes as type>
<#if type?is_first>if<#else>} else if</#if> (value instanceof ${type}) {
  if (!((${type}) value).containsObserver(keyObserver${type})) {
    ((${type}) value).addObserver(keyObserver${type});
  }
</#list>
}
