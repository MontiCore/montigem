<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "className")}
if(idx < 0){
  throw new IllegalStateException("idx (=" + idx + ") must be 0 or higher");
}
if(idx > ${name}.size()){
  throw new IllegalStateException("idx (=" + idx + ") must be at most owns.size (=" + ${name}.size() + ")");
}

long oldValue;
if(this.${name}.size() > idx){
  oldValue = this.${name}.getID(idx);
}else{
  oldValue = -1;
}

this.${name}.set(idx, o);
${className?uncap_first}Observers.forEach(obs -> obs.maybeNotifySet${name?cap_first}((${className}) this, idx, oldValue, o.getGemId(), scope));
