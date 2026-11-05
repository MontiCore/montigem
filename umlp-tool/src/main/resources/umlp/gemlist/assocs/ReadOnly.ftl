<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a change through the list of a GemMap's mirror (the value side of a bidirectional qualified association, see
     GenService#isMapMirror) is refused: the mirror is read-only, the owner's map changes it.
     signature: the signature of the _internal_<op>AndSync method; name: the role of the mirror; owner: its type -->
${tc.signature("signature", "name", "owner")}
${cd4c.method(signature)}
Log.error("0xA7330 The ${name} of the object with id " + getOwnerGemID() + " cannot be changed: it mirrors the maps"
    + " of the ${owner} objects, change those instead");
