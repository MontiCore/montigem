<#-- (c) https://github.com/MontiCore/monticore -->
<#-- included code (tc.includeArgs) of _doDelete<Type> (server and mock manager): the deleted object's entries of its
     bidirectional GemMaps leave first, through its maps with ALL, whatever the scope of the deletion, like removals:
     the other side of the loaded values no longer refers to it, the database drops the rows and the remove commands
     reach the subscribers of the owner and of the values (the delete command reaches only the owner's).
     name: the deleted type; deletedOpt: an expression for the Optional of the deleted object;
     bidirectionalMaps: the roles of its bidirectional GemMaps -->
${tc.signature("name", "deletedOpt", "bidirectionalMaps")}
<#if bidirectionalMaps?has_content>
  {
    java.util.Optional<${name}> deleted = ${deletedOpt};
    if (deleted.isPresent()) {
  <#list bidirectionalMaps as map>
      for (long valueID : new java.util.ArrayList<>(deleted.get()._internal_get${map?cap_first}GemMap().getValueIDs())) {
        deleted.get().remove${map?cap_first}ByIdSure(valueID, NotificationScope.ALL);
      }
  </#list>
    }
  }
</#if>
