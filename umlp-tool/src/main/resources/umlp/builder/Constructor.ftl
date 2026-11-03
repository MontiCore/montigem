<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "assocToManagerMap", "initialAttributes", "assocToGemMapClass")}
${cd4c.constructor("public ${name}Builder(long gemId)")}

this.realBuilder = (${name}Builder) this;
this.gemId = gemId;
<#list assocToManagerMap as assoc, manager>
<#assign assocName=assoc.getSymbol().getOtherSide().getType().print()+"_"+assoc.getName()?cap_first>
    this._${assoc.getName()} = ${manager}.create${assocName}Builder(gemId, new ArrayList<>());
</#list>
<#list assocToGemMapClass as assoc, gemMapClass>
    this._${assoc.getName()} = new ${gemMapClass}(gemId);
</#list>
<#list initialAttributes as i>
  this.${i[0]}(${i[1]});
</#list>
