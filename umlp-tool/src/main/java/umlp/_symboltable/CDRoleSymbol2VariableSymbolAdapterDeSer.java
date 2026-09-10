package umlp._symboltable;

import de.monticore.cdassociation._symboltable.CDRoleSymbolDeSer;
import de.monticore.symbols.basicsymbols._symboltable.IBasicSymbolsScope;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbolDeSer;
import de.monticore.symboltable.serialization.json.JsonObject;
import de.monticore.types.check.SymTypeExpressionDeSer;

public class CDRoleSymbol2VariableSymbolAdapterDeSer extends VariableSymbolDeSer {

  @Override
  public VariableSymbol deserialize(JsonObject symbolJson) {
    return deserialize((IBasicSymbolsScope) null, symbolJson);
  }

  @Override
  public VariableSymbol deserialize(IBasicSymbolsScope scope, JsonObject symbolJson) {
    CDRoleSymbolDeSer cdRoleSymbolDeSer = new CDRoleSymbolDeSer();
    var role = cdRoleSymbolDeSer.deserialize(symbolJson);
    // The role deserializer resolves types without the importing language's scope.
    role.setType(SymTypeExpressionDeSer.deserializeMember("type", symbolJson, scope));
    var adapter = new CDRoleSymbol2VariableSymbolAdapter(role);
    adapter.setEnclosingScope(scope);
    adapter.setFullName(role.getFullName());
    adapter.setPackageName(role.getPackageName());
    return adapter;
  }
}
