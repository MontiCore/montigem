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
// like for a bidirectional list (a single owner: they leave their old owner's map, see _internal_put${assoc.getName()?cap_first}Id).
// Not for a deserialized object: loaded ones are consistent, for one created elsewhere its create command does so
if (!INTERNAL_deser_mode) {
  boolean server = CommandManager.getParticipantId() == 0;
  for (long valueID : this._${assoc.getName()}.getValueIDs()) {
    if (!server && !${valueManager}.isLoaded${valueType}(valueID)) {
      continue;
    }
    Optional<${valueType}> value = ${valueManager}.${valueType?uncap_first}Opt(valueID);
    if (value.isEmpty()) {
      continue;
    }
    <#if genService.isList(otherSide)>
    if (value.get()._internal_get${otherSide.getName()?cap_first}GemList().indexOfId(this.getGemId()) == -1) {
      value.get()._internal_add${otherSide.getName()?cap_first}(value.get()._internal_get${otherSide.getName()?cap_first}GemList().getIDs().size(),
          Collections.singletonList(this.getGemId()), NotificationScope.MINIMAL);
    }
    <#else>
    long oldOwnerID = value.get().get${otherSide.getName()?cap_first}Gem().getGemId();
    if (oldOwnerID != -1 && oldOwnerID != this.getGemId()
        && (server || ${ownerManager}.isLoaded${ownerType}(oldOwnerID))) {
      Optional<${ownerType}> oldOwner = ${ownerManager}.${ownerType?uncap_first}Opt(oldOwnerID);
      if (oldOwner.isPresent()) {
        oldOwner.get()._internal_remove${assoc.getName()?cap_first}Id(valueID, server ? NotificationScope.ALL : NotificationScope.LOCAL);
      }
    }
    value.get()._internal_set${otherSide.getName()?cap_first}(this.getGemId(), NotificationScope.MINIMAL);
    </#if>
  }
}
  </#if>
</#list>
${name?uncap_first}Observers.forEach(obs -> obs.notifyBuild(this)); //+ an initial notification
}
return Optional.of(this);

