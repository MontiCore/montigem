/*
 * Facade for '@umlp/commonj2ts' used only by the real-tsc compile check in
 * exectests.TestRealTscCompileAndRun.
 *
 * We deliberately do not import the real package's root index.ts: its
 * barrel re-exports util/UmlpToAsyncPipe.ts, which pulls in @angular/core
 * and rxjs. None of the collection classes we test need Angular, so this
 * facade imports them directly by relative path instead, bypassing that
 * barrel entirely.
 *
 * '../commonj2ts-src' is created at test-run time (see
 * TestRealTscCompileAndRun) as a symlink to the real
 * umlp-rte/frontend/commonj2ts/src/main/gui/mc/fenix/commonj2ts directory.
 */
export { AsyncSet } from "../commonj2ts-src/javautil/AsyncSet";
export { AsyncList } from "../commonj2ts-src/javautil/AsyncList";
export { AsyncStream, Collectors } from "../commonj2ts-src/javautil/AsyncStream";
export { StringHelper } from "../commonj2ts-src/javautil/StringHelper";
export { Arrays } from "../commonj2ts-src/javautil/Arrays";
export { GemListUtil } from "../commonj2ts-src/util/GemListUtil";
export { GemList } from "../commonj2ts-src/common/GemList";
export { IUMLPList } from "../commonj2ts-src/common/IUMLPList";
export { LocalDateTimeHelper } from "../commonj2ts-src/types/LocalDateTimeHelper";
