import json
import typing
import sys
from asyncio import gather, create_task, Future, sleep
import traceback
from pydantic import BaseModel, ConfigDict, Field

from websockets.asyncio.client import connect
import asyncio


def uncap(s):
  return s[:1].lower() + s[1:]


async def ainput(string: str) -> str:
  """async input alternative"""
  import sys
  if string:
    await asyncio.get_event_loop().run_in_executor(None, lambda s=string: sys.stdout.write(s + ' '))
  return await asyncio.get_event_loop().run_in_executor(
    None, sys.stdin.readline)


class CommandManager:
  instance: 'CommandManager' = None
  do_debug: bool = False

  def __init__(self):
    self.queue_in = asyncio.Queue()
    self.queue_out = asyncio.Queue()
    self.listeners = []
    self.participantId = None
    self.commandId = 1

    self.wait_for_command_map: {int: Future} = {}
    CommandManager.instance = self

  async def connect(self, host="ws://localhost:8081/umlp/api/command"):
    print("called connect" + host)

    async def receive_messages(websocket):
      print("receive_messages started")
      """Receives messages from WebSocket and puts them into the incoming queue."""
      async for message in websocket:
        if self.do_debug:
          print("receive " + str(message), file=sys.stderr)
        msg = json.loads(message)
        # complete future
        if  ("statusResult" in msg
               and "simpleResult" in msg
               and "listResult" in msg
               and "validationResult" in msg
           ):
          # command is a reply
          f = self.wait_for_command_map[msg["commandId"]]
          del self.wait_for_command_map[msg["commandId"]]
          f.set_result(msg)
        else:
          # new command
          await self.queue_in.put(msg)

    async def send_messages(websocket):
      """Gets messages from the outgoing queue and sends them through the WebSocket."""
      while True:
        message = await self.queue_out.get()  # Waits for a message to send
        if self.do_debug:
          print("---> send " + str(message), file=sys.stderr)
        await websocket.send(message)

    async with connect(host) as websocket:
      message = await websocket.recv()
      self.participantId = int(message)
      print(f'Connected at participant %s' % self.participantId)
      # Start send and receive tasks concurrently
      receive_task = asyncio.create_task(receive_messages(websocket))
      send_task = asyncio.create_task(send_messages(websocket))

      # Wait for both tasks (they run forever unless an exception occurs)
      # await asyncio.gather(receive_task, send_task)
      # return [receive_task, send_task]
      await asyncio.gather(receive_task, send_task)

  async def send(self, msg: 'Command'):
    while self.participantId is None:
      await sleep(0.1) # not yet connected -> spin until the participant id is set
    used_cmd_id = self.commandId
    msg.commandId = used_cmd_id
    msg.participantId = self.participantId
    self.commandId = self.commandId + 1
    f = asyncio.get_running_loop().create_future()
    self.wait_for_command_map[used_cmd_id] = f
    await self.queue_out.put(msg.model_dump_json(exclude_none=True))
    return await f

  async def command_forwarding_consumer(self, cmd_handler, manager):
    while True:
      message = await self.queue_in.get()  # Wait for a message
      if self.do_debug:
        print(f"command manager > Received message: {message}", file=sys.stderr)
      if "commandType" in message:
        try:
          await cmd_handler.handleCommand(message, manager)
        except BaseException as e:
          print("command manager > Received and failed to handle message", str(message), e)
          traceback.print_exc()
      else:
        raise Exception(f"command manager > Unexpected message, missing commandType")

class Command(BaseModel):
  model_config = ConfigDict(extra="allow")  # Pydantic v2
  commandId: typing.Optional[int] = Field(default=None)
  participantId: typing.Optional[int] = Field(default=None)
  commandType: str
