<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("deSerTypes")}
${cd4c.method("protected Object deserSingleValue(JsonElement val, String tp)")}

// Prefer the runtime type discriminator written by the sender over the declared
// type, so subclass instances from extending CDs are materialized with their
// concrete type in the manager of that type (monticore#5062).
String requestedTp = tp;
if (val.isJsonObject() && val.getAsJsonObject().hasStringMember("@type")) {
  requestedTp = val.getAsJsonObject().getStringMember("@type");
}
if (!canDeser(requestedTp) && !BaseClassObjectMapper.canDeSer(requestedTp)) {
  // This DeSer does not know the (runtime) type:
  // return null so another CommandResultDeSer gets a chance to handle it.
  return null;
}

Object res = null;
if(BaseClassObjectMapper.canDeSer(requestedTp)){
  JsonElement value = val;
  res = BaseClassObjectMapper.deserialize(value.toString(), requestedTp).orElse(null);
}else {
  switch (requestedTp) {
<#list deSerTypes as fqn, simpleName>
    case "${fqn}": {
      res = new ${simpleName}DeSer().fromJson(val).orElse(null);
      break;
    }
</#list>
    default: {
      Log.error("Can not deserialize value of type " + requestedTp);
    }
  }
}
return res;
