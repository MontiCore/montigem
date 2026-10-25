${tc.signature("deSerTypes")}
${cd4c.method("protected void serSingleValue(Object o, JsonPrinter printer, String tp)")}

switch (tp){
// Java type system restrictions force us to use casts here
<#list deSerTypes as fqn, simpleName>
  case "${fqn}": {
    // Dispatch via the object's own DeSer so subclass instances (possibly from an
    // extending CD unknown to this switch) are serialized with their concrete type
    // and all their fields (monticore#5062). Raw type: DeSers of subclasses do not
    // extend the DeSers of their superclasses, so no covariant typing is possible.
    JsonDeSer runtimeDeSer = ((${simpleName}) o).getDeSer();
    if (runtimeDeSer != null) {
      runtimeDeSer.buildJson(o, printer);
    } else {
      new ${simpleName}DeSer().buildJson((${simpleName}) o, printer);
    }
  break;
  }
</#list>
  default:{
    Log.error("Can not serialize value of type " + tp);
  }
}