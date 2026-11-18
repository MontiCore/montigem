/*
 * Facade for '@umlp/functiondisplay' used only by the real-tsc compile check
 * (see util.RealTscCheck).
 *
 * The real package's root index.ts exports the Angular FunctionDisplay
 * component, so this facade exports only the generated <<pojoCD>> classes.
 *
 * '../functiondisplay-src' is created at test-run time as a symlink to
 * umlp-rte/frontend/functiondisplay/build/gui2java2ts/mc/fenix/functiondisplay.
 */
export { Channel4Display } from "../functiondisplay-src/functionstructure4fenix/Channel4Display";
export { Channel4DisplayBuilder } from "../functiondisplay-src/functionstructure4fenix/Channel4DisplayBuilder";
export { Channel4DisplayDeSer } from "../functiondisplay-src/functionstructure4fenix/Channel4DisplayDeSer";
export { Function4Display } from "../functiondisplay-src/functionstructure4fenix/Function4Display";
export { Function4DisplayBuilder } from "../functiondisplay-src/functionstructure4fenix/Function4DisplayBuilder";
export { Function4DisplayDeSer } from "../functiondisplay-src/functionstructure4fenix/Function4DisplayDeSer";
export { FunctionStructure4FenixDeSer } from "../functiondisplay-src/functionstructure4fenix/FunctionStructure4FenixDeSer";
export { Port4Display } from "../functiondisplay-src/functionstructure4fenix/Port4Display";
export { Port4DisplayBuilder } from "../functiondisplay-src/functionstructure4fenix/Port4DisplayBuilder";
export { Port4DisplayDeSer } from "../functiondisplay-src/functionstructure4fenix/Port4DisplayDeSer";
