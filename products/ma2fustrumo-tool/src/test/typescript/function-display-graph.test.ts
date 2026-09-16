import '@angular/compiler';
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {BaseClassObjectMapper, JsonPrinter, PojoGraphContext} from '@umlp/commonj2ts';
import {Function4DisplayBuilder} from '../../../../../umlp-rte/frontend/functiondisplay/build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/Function4DisplayBuilder';
import {Function4DisplayDeSer} from '../../../../../umlp-rte/frontend/functiondisplay/build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/Function4DisplayDeSer';
import {Port4DisplayBuilder} from '../../../../../umlp-rte/frontend/functiondisplay/build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/Port4DisplayBuilder';
import {Channel4DisplayBuilder} from '../../../../../umlp-rte/frontend/functiondisplay/build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/Channel4DisplayBuilder';
import {FunctionStructure4FenixDeSer} from '../../../../../umlp-rte/frontend/functiondisplay/build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/FunctionStructure4FenixDeSer';

test('generated browser model preserves reverse links and shared identities through JSON', async () => {
  BaseClassObjectMapper.addDeSers(await (await FunctionStructure4FenixDeSer.getInstance()).getDeSerMap());
  const root = (await new Function4DisplayBuilder().__Function4DisplayBuilderConstructor()).name('root');
  const child = (await new Function4DisplayBuilder().__Function4DisplayBuilderConstructor()).name('child');
  await child.owner(root);
  assert.equal(await root.getSubFunction(0), child);
  const channel = await (await new Channel4DisplayBuilder().__Channel4DisplayBuilderConstructor()).build();
  const input = (await new Port4DisplayBuilder().__Port4DisplayBuilderConstructor()).name('in');
  const output = (await new Port4DisplayBuilder().__Port4DisplayBuilderConstructor()).name('out');
  await input.channel4Display(channel);
  await output.channel4Display(channel);
  await root.addIn(input);
  await child.addOut(output);
  assert.equal(await channel.sizePort4Display(), 2);
  const type = 'mc.fenix.functiondisplay.functionstructure4fenix.Function4Display';
  const json = await BaseClassObjectMapper.serialize(await root.build(), type);
  const wire = JSON.parse(json);
  assert.equal(typeof wire.in[0], 'object');
  assert.match(json, /"\$ref"/);
  const copy: any = await BaseClassObjectMapper.deserialize(json, type);
  assert.ok(copy);
  const copiedChild = await copy.getSubFunction(0);
  assert.equal(copiedChild.getOwner(), copy);
  assert.equal((await copy.getIn(0)).getOwnerIn(), copy);
  assert.equal((await copy.getIn(0)).getChannel4Display(), (await copiedChild.getOut(0)).getChannel4Display());
  const [a, b]: any[] = await Promise.all([
    BaseClassObjectMapper.deserialize(json, type), BaseClassObjectMapper.deserialize(json, type)
  ]);
  assert.notEqual(a, b);
  assert.equal((await a.getSubFunction(0)).getOwner(), a);
  assert.equal((await b.getSubFunction(0)).getOwner(), b);
  await child.owner(null);
  assert.equal(await root.sizeSubFunction(), 0);
});

test('JSON array members are JSON objects, not quoted JSON strings', () => {
  const printer = new JsonPrinter(true);
  printer.beginObject();
  printer.beginArray('values');
  printer.valueJson('{"$ref":"0"}');
  printer.endArray();
  printer.endObject();
  assert.deepEqual(JSON.parse(printer.toString()), {values: [{$ref: '0'}]});
});

test('failed reads do not retain graph identities', async () => {
  const context = new PojoGraphContext();
  const deser = await new Function4DisplayDeSer().__Function4DisplayDeSerConstructor();
  await assert.rejects(context.deserializeObject({$ref: 'missing'}, deser), /Invalid POJO reference/);
});
