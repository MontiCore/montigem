<#-- (c) https://github.com/MontiCore/monticore -->
<#--
  The build method is generated to allow object creation.
-->
${tc.signature("name", "managerName", "assocList", "isSingleton")}
${cd4c.method("public Optional<${name}> build()")}

if (!buildFinished) {
  buildFinished = true;
  if (!isBuildable()) {
    return Optional.empty();
  }

<#list assocList as assoc>
    <#if genService.isMultiValued(assoc)>
      this._${assoc.getName()}.build();
    <#else>
    {
      boolean needsBuild = this._${assoc.getName()}.getLoadingState() == LoadingState.LOADED
                            && !this._${assoc.getName()}.get().isBuildFinished();
      if (needsBuild) {
        boolean buildFailed = this._${assoc.getName()}.get().build().isEmpty();
        if(buildFailed){
          //let the build fail
          ${name}ErrorCollector.report${assoc.getName()?cap_first}Invalid(this);
          return Optional.empty();
        }
      }
    }
    </#if>
</#list>

    if(!INTERNAL_deser_mode) {
    <#if !isSingleton>
      ${managerName}.add${name}(this);
    </#if>

<#list assocList as assoc>
  <#if genService.isList(assoc)>
    <#if genService.isBidirectional(assoc)>
      <#assign otherSide=assoc.getSymbol().getOtherSide().getAssocSide()>
      <#assign thisType=assoc.getSymbol().getType().print()>
      for (${thisType} id: this._${assoc.getName()}) {
          <#if genService.isList(otherSide)>
            id.add${otherSide.getName()?cap_first}(this, NotificationScope.MINIMAL);
          <#else>
            id.set${otherSide.getName()?cap_first}(this, NotificationScope.MINIMAL);
          </#if>
      }
    </#if>
  <#else>
    <#if genService.isBidirectional(assoc)>
      <#assign otherSide=assoc.getSymbol().getOtherSide().getAssocSide()>
      if (this._${assoc.getName()}.getLoadingState() == LoadingState.LOADED) {
      <#if genService.isList(otherSide)>
        if (!this._${assoc.getName()}.get().get${otherSide.getName()?cap_first}List().contains(this)) {
          this._${assoc.getName()}.get().add${otherSide.getName()?cap_first}(this, NotificationScope.GLOBAL);
        }
      <#else>
        this._${assoc.getName()}.get().set${otherSide.getName()?cap_first}(this, NotificationScope.GLOBAL);
      </#if>
      }
      </#if>
    </#if>
</#list>
}
<#list assocList as assoc>
  <#if genService.isMap(assoc) && genService.getOtherSideOfMap(assoc).isPresent()>
    <#assign otherSide=genService.getOtherSideOfMap(assoc).get()>
    <#assign valueType=assoc.getSymbol().getType().print()>
    <#assign valueManager=genService.getManagerOfType(assoc)>
    <#assign ownerType=otherSide.getSymbol().getType().print()>
    <#assign ownerManager=genService.getManagerOfType(otherSide)>
// bidirectional qualified association: the other side of the values put before building refers to this owner now,
// like for a bidirectional list (a single owner: they leave their old owner's map; the server removes them with ALL,
// the create command does not reach the old owner's subscribers, like the GLOBAL set of a bidirectional association
// above). Not for a deserialized object: loaded ones are consistent, for one created elsewhere its create command does so
if (!INTERNAL_deser_mode) {
  for (long valueID : this._${assoc.getName()}.getValueIDs()) {
    <#if !genService.isList(otherSide)>
    ${tc.includeArgs("umlp.methods.gemmap.OtherSideMove", ["valueID", "this.getGemId()", assoc.getName(),
        otherSide.getName(), valueType, valueManager, ownerType, ownerManager,
        "CommandManager.getParticipantId() == 0 ? NotificationScope.ALL : NotificationScope.LOCAL", ""])}
    </#if>
    if (${valueManager}.isLoaded${valueType}(valueID)) {
      ${valueType} value = ${valueManager}.get${valueType}Sure(valueID);
      ${tc.includeArgs("umlp.methods.gemmap.OtherSideRefer", ["value", "this.getGemId()", otherSide.getName(),
          genService.isList(otherSide)])}
    }
  }
}
  </#if>
</#list>
${name?uncap_first}Observers.forEach(obs -> obs.notifyBuild(this)); //+ an initial notification
}
return Optional.of(this);

