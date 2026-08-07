package mc.fenix.functiondisplay.functionstructure4fenix;

import de.monticore.symboltable.serialization.JsonPrinter;
import de.monticore.symboltable.serialization.json.JsonObject;
import java.util.function.Consumer;

@umlp.jsweet.extension.annotation.CD2Java
public class FunctionActionDeSer extends FunctionActionDeSerTOP {

    @Override
    protected void serializeFunctionClick(FunctionAction obj, JsonPrinter printer) {
        // Consumer cannot be serialized
    }

    @Override
    protected Consumer<Function4Display> deserializeFunctionClick(JsonObject json) {
        // Consumer cannot be deserialized
        return (f) -> {};
    }

}