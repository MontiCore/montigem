import enum
import importlib
import inspect
import typing
from typing import Any
from montigem_runtime.commandmanager import CommandManager, Command, uncap
from pydantic import BaseModel, PrivateAttr
from collections.abc import Iterator, AsyncIterator
import asyncio


class NotificationScope(enum.Enum):
  MINIMAL = 0
  LOCAL = 3
  GLOBAL = 4
  ALL = 5

  def contains(self, other: 'NotificationScope'):
    return other.value <= self.value


GEM_ID = typing.Annotated[int, "is_gem_id"]


class GemBase(BaseModel):
  gemId: int
  TYPE: typing.ClassVar[str] = "ERROR_TYPE"
  PACKAGE: typing.ClassVar[str] = "ERROR_PCK"

  def serialize(self) -> str:
    return self.model_dump_json(exclude_none=True)


def create_gem_deserializer(type: Any):
  return lambda value: deserialize_gem(value, type)


def deserialize_gem(value: Any, type=None) -> 'Gem':
  if isinstance(value, Gem):
    if value._target is None:
      value._target = type
    return value
  # if isinstance(value, GemBase):
  #   value = value.gemId
  return Gem[type](value, target=type)


def empty_gem(type: str) -> 'Gem':
  """Default value of a Gem field: unset, but aware of its target type"""
  return Gem[type](target=type)


def serialize_gem(value: 'Gem') -> int:
  return value.gemId


def create_gem_list_deserializer(type: Any):
  return lambda value: deserialize_gem_list(value, type)


def deserialize_gem_list(value: Any, type=None) -> 'GemList':
  if isinstance(value, GemList):
    return value
  elif isinstance(value, list):  # todo: is origin?
    r = GemList[type](target=type)
    r.indexes = value
    return r
  elif value is None:
    return GemList[type]()
  else:
    raise 'Unknown type'


def serialize_gem_list(value: 'GemList') -> typing.List[int]:
  return value.indexes


DataT = typing.TypeVar('DataT', bound=GemBase)


def resolve_gem_class(target: str) -> typing.Type[GemBase]:
  """Resolves a qualified type name such as 'MyDomain.B' or 'MyDomain.B.B' to its GemBase class"""
  try:
    obj = importlib.import_module(target)
  except ImportError:
    module_name, _, cls_name = target.rpartition('.')
    obj = getattr(importlib.import_module(module_name), cls_name)
  if inspect.ismodule(obj):
    # generated modules contain a class of the same name (MyDomain.B -> MyDomain.B.B)
    obj = getattr(obj, target.rpartition('.')[2])
  return obj


class Gem(BaseModel, typing.Generic[DataT]):
  """
  Lazy reference to a domain object.

    b = await my_a.assocB              # loads the referenced object
    v = await my_a.assocB.fieldOfB     # loads it and reads an attribute
    await my_a.directB.setFieldOfB(42) # loads it and calls an async method
  """
  gemId: int = -1
  _target: typing.Optional[str] = PrivateAttr(default=None)

  def __init__(self, /, gemId=-1, target: typing.Optional[str] = None, **data: Any) -> None:
    super().__init__(**data)
    self.gemId = gemId
    self._target = target

  def set_gemid(self, gem_id):
    self.gemId = gem_id

  def target_class(self) -> typing.Type[DataT]:
    if self._target is None:
      raise TypeError("Gem has no target type, unable to resolve it")
    return resolve_gem_class(self._target)

  async def load(self) -> DataT:
    if self.gemId < 0:
      raise ValueError(f"Gem of {self._target} is not set")
    cls = self.target_class()
    return await DomainManager.for_package(cls.PACKAGE).get(cls, self.gemId)

  def __await__(self) -> typing.Generator[Any, None, DataT]:
    return self.load().__await__()

  def __getattr__(self, name: str) -> Any:
    # only called if regular lookup fails; keep pydantic internals & private attributes intact
    if name.startswith('_'):
      return super().__getattr__(name)

    async def call():
      obj = await self.load()
      return getattr(obj, name)

    return ChainableAsyncCall(call)


class GemList(BaseModel, typing.Generic[DataT]):
  indexes: typing.List[int] = []
  _target: typing.Optional[str] = PrivateAttr(default=None)

  def __init__(self, /, target: typing.Optional[str] = None, indexes = None, **data: Any) -> None:
    super().__init__(**data)
    self.indexes = [] if indexes is None else indexes
    self._target = target

  def add(self, index_in_list: int, gem_id: int):
    self.indexes.insert(index_in_list, gem_id)

  def get_gem_id(self, index_in_list: int) -> GEM_ID:
    return self.indexes[index_in_list]

  def get_index_of_gem_id(self, gem_id: GEM_ID) -> int:
    return self.indexes.index(gem_id)

  def pop(self, index_in_list: int) -> GEM_ID:
    return self.indexes.pop(index_in_list)

  def __len__(self) -> int:
    return len(self.indexes)

  def gem_ids(self) -> Iterator[GEM_ID]:
    return map(self.get_gem_id, self.indexes)

  def target_class(self) -> typing.Type[DataT]:
    if self._target is None:
      raise TypeError("GemList has no target type, unable to use implicit shortcuts such as the async iterator")
    return resolve_gem_class(self._target)

  def __aiter__(self):
    return AsyncGemListIterator(self.indexes, self.target_class())

class AsyncGemListIterator:
  """
  Iterator for the async list comprehension
  async for b_elem in a.assocList:
    # b_elem is now the domain object
  """
  def __init__(self, indexes, target_class):
    self._iterator = iter(indexes)
    self.target_class = target_class

  async def __anext__(self):
    try:
      gem_id = next(self._iterator)
    except StopIteration:
      raise StopAsyncIteration

    return await DomainManager.for_package(self.target_class.PACKAGE).get(self.target_class, gem_id)
    return await self.api.get_object(index)

class GlobalGemList(GemList):
  _cache: typing.MutableMapping[int, DataT] = {}

  def is_loaded(self, gem_id: int) -> bool:
    return gem_id in self._cache.keys()

  def set(self, index_in_list: int, obj: DataT):
    assert index_in_list == obj.gemId
    self.indexes.insert(obj.gemId, obj.gemId)
    self._cache[obj.gemId] = obj

  def get(self, gem_id: int) -> DataT:
    return self._cache[gem_id]


class GemProtocolException(Exception):
  def __init__(self, message, command: typing.Optional[Command], resp: Any):
    self.message = message
    super().__init__(self.message)


class DomainManager:
  _instances: typing.ClassVar[
    typing.MutableMapping[str, 'DomainManager']] = {}  # technically support multiple managers
  global_state: typing.MutableMapping[typing.Type[GemBase], GlobalGemList]
  domain_name: str

  def __init__(self, domain_name: str, command_manager: CommandManager):
    self.domain_name = domain_name
    self.command_manager = command_manager
    self.global_state = {}
    DomainManager._instances[domain_name.lower()] = self

  G = typing.TypeVar("G", bound=GemBase)

  @classmethod
  def for_package(cls, domain: str) -> 'DomainManager':
    """The manager responsible for a domain"""
    try:
      return cls._instances[domain.lower()]
    except KeyError:
      raise LookupError(f"No DomainManager registered for domain {domain!r}") from None

  def get_global_list(self, domaintype: typing.Type[G]) -> GlobalGemList[G]:
    """
    Only contains the locally known elements, see list_filtered to retrieve all
    :param domaintype: the type
    :return: the local GemList of all elements of the given type.
    """
    if domaintype not in self.global_state:
      self.global_state[domaintype] = GlobalGemList(str(domaintype))
    return self.global_state[domaintype]

  async def list_filtered(self, domaintype: typing.Type[G]):
    return await self.command_manager.send(
      Command(commandType=f"{domaintype.PACKAGE}.commands.{domaintype.TYPE}_get_filtered"))

  def is_loaded_locally(self, domaintype: typing.Type[G], gem_id: GEM_ID) -> bool:
    return self.get_global_list(domaintype).is_loaded(gem_id)

  async def get(self, domaintype: typing.Type[G], gem_id: int) -> G:
    # todo: future for multiple calls?
    glist = self.get_global_list(domaintype)
    if glist.is_loaded(gem_id):
      return glist.get(gem_id)

    command = Command(commandType=f"{domaintype.PACKAGE}.commands.{domaintype.TYPE}_get",
                      gemId=gem_id)
    resp = await self.command_manager.send(command)
    if not resp['successful']:
      raise GemProtocolException(f"Get of {domaintype}#{gem_id} not successful", command=command,
                                 resp=resp)
    r = domaintype.model_validate(resp["value"])
    glist.set(gem_id, r)
    return r

  async def get_free_gem_id(self):
    return (await self.command_manager.send(
      Command(commandType=f"{self.domain_name}.commands.GetFreeGemIdRequest")))["value"]

  async def find_all(self, domaintype: typing.Type[G]) -> typing.List[G]:
    r = await self.list_filtered(domaintype)
    if not r["successful"]:
      raise GemProtocolException(f"Find all of {domaintype} not successful", command=None, resp=r)
    results = await asyncio.gather(*(self.get(domaintype, gem_id) for gem_id in r['list']))
    return list(results)

  async def create(self, obj: G) -> G:
    cmd = Command(commandType=f"{obj.PACKAGE}.commands.{obj.TYPE}_create",
                  **{uncap(obj.TYPE): obj})
    r = await self.command_manager.send(cmd)
    if not r["successful"]:
      raise GemProtocolException(f"Create not successful", command=cmd, resp=r)
    return await self.get(type(obj), obj.gemId)

  async def get_list(self, domaintype: typing.Type[G], gem_id_iterator: Iterator[GEM_ID]) -> \
  AsyncIterator[G]:
    """

    [obj.name async for obj in my_manager.get_list(MyDomain.Type, parent.list.gem_ids()]

    :param domaintype: domain class, similar to #get
    :param gem_id_iterator: the iterator
    :return:
    """
    for gem_id in gem_id_iterator:
      yield await self.get(domaintype, gem_id)


class ChainableAsyncCall:
  """
  A lazy, chainable async attribute access / call, used as a helper to proxy gems.
  Nothing is executed until the chain is awaited:
    await gem.attr.other
    await gem.attr.setOther(42)
  """

  def __init__(self, factory):
    # factory: zero-arg async function producing the value of this chain step
    self._factory = factory

  def __await__(self):
    return self._factory().__await__()

  def __getattr__(self, name):
    # attribute access only: resolve (and await, e.g. a Gem's lazy attribute), but never call
    async def chained():
      attr = getattr(await self, name)
      return await attr if inspect.isawaitable(attr) else attr

    return ChainableAsyncCall(chained)

  def __call__(self, *args, **kwargs):
    # call the resolved callable and await its result if it is a coroutine (async method)
    async def chained():
      result = (await self)(*args, **kwargs)
      return await result if inspect.isawaitable(result) else result

    return ChainableAsyncCall(chained)
