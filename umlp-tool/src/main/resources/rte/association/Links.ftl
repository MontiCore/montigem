<#-- Update this end before calling the opposite mutator: its guard terminates recursion. -->
<#macro attach assoc target>
  <#if genService.isBidirectional(assoc)>
    <#assign other = assoc.getSymbol().getOtherSide().getAssocSide()>
    <#assign otherName = other.getName()?cap_first>
    <#if genService.isList(other)>
      ${target}.add${otherName}(this);
    <#elseif genService.isOptional(other)>
      ${target}.set${otherName}(Optional.of(this));
    <#else>
      ${target}.set${otherName}(this);
    </#if>
  </#if>
</#macro>
<#macro detach assoc target>
  <#if genService.isBidirectional(assoc)>
    <#assign other = assoc.getSymbol().getOtherSide().getAssocSide()>
    <#assign otherName = other.getName()?cap_first>
    <#if genService.isList(other)>
      ${target}.remove${otherName}(this);
    <#elseif genService.isOptional(other)>
      if (${target}.get${otherName}().orElse(null) == this) {
        ${target}.set${otherName}(Optional.empty());
      }
    <#else>
      if (${target}.get${otherName}() == this) {
        ${target}.set${otherName}(null);
      }
    </#if>
  </#if>
</#macro>
