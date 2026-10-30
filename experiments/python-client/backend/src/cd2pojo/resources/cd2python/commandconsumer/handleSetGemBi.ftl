${tc.signature("clazzL", "attrL", "clazzR", "attrR", "domain", "listL", "listR" )}
<#include "cd2python.PythonMacros.ftl"> <#--Load macros-->


<#macro exchange clazzL attrL clazzR attrR listL listR>
<#--  <@indent>-->
    if domain_manager.is_loaded_locally(${domain}.${clazzR}.${clazzR}, message["${attrL}GemId"]):
    <@indent>
      ${clazzR?uncap_first} = await domain_manager.get(${domain}.${clazzR}.${clazzR}, message["${attrL}GemId"])
      <#if listR>
        await ${clazzR?uncap_first}.${attrR}_add(message["${attrR}Index"], message["${attrR}GemId"], montigem_runtime.gembase.NotificationScope.LOCAL)
      <#else>
        await ${clazzR?uncap_first}.set${attrR?cap_first}(message["${attrR}GemId"], montigem_runtime.gembase.NotificationScope.LOCAL)
      </#if>
    </@indent>
    elif domain_manager.is_loaded_locally(${domain}.${clazzL}.${clazzL}, message["old${attrR?cap_first}GemId"]):
    <@indent>
      <#if listL>
        old_obj = await domain_manager.get(${domain}.${clazzL}.${clazzL}, message["old${attrR?cap_first}GemId"])
        index = "todo"
        await old_obj.${attrL}_rem(message["${attrR}Index"], message["${attrL}GemId"], montigem_runtime.gembase.NotificationScope.MINIMAL)
      <#else>
          # // Does not change other side
          # // todo: is that really the fact?
      </#if>
      pass
    </@indent>
    elif domain_manager.is_loaded_locally(${domain}.${clazzL}.${clazzL}, message["${attrR}GemId"]):
    <@indent>
      ${clazzL?uncap_first} = await domain_manager.get(${domain}.${clazzL}.${clazzL}, message["${attrR}GemId"])
      <#if listL>
        await ${clazzL?uncap_first}.${attrL}_add(message["${attrL}Index"], message["${attrL}GemId"], montigem_runtime.gembase.NotificationScope.MINIMAL)
      <#else>
        await ${clazzL?uncap_first}.set${attrL?cap_first}(message["${attrL}GemId"], montigem_runtime.gembase.NotificationScope.MINIMAL)
      </#if>
    </@indent>
    else:
    <@indent>
      import sys
      print(f"Received unnecessary update message? ", file=sys.stderr)
      #raise Exception("BIDirectional Set failed?")
    </@indent>
<#--  </@indent>-->

</#macro>

if message["old${attrL?cap_first}GemId"] != message["${attrL}GemId"]: # exchange ${attrL}
<@indent>
  <@exchange clazzR attrR clazzL attrL listR listL >
  </@exchange>

</@indent>

if message["old${attrR?cap_first}GemId"] != message["${attrR}GemId"]: # exchange ${attrR}

<@indent>
  <@exchange clazzL attrL clazzR attrR listL listR>
  </@exchange>
</@indent>
