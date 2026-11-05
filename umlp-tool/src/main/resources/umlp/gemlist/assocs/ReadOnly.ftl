<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a change through the list on the other side of a GemMap (the value side of a bidirectional type qualified
     association, see GenService#isOtherSideOfMap) is refused: there is no key to put the value under, the owners' maps
     change it. signature: the signature of the _internal_<op>AndSync method; name: the role of the other side;
     owner: its type -->
${tc.signature("signature", "name", "owner")}
${cd4c.method(signature)}
Log.error("0xA7330 The ${name} of the object with id " + getOwnerGemID() + " cannot be changed: it is the other side"
    + " of the maps of the ${owner} objects, change those instead");
