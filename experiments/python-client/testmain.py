import sys

sys.path.append('build/py_out')
sys.path.append('runtime-package/src')

from MyDomain import A
from MyDomain import B
from MyDomain import MyDomainManager

from montigem_runtime.commandmanager import CommandManager, ainput

command_manager: CommandManager = CommandManager()
my_domain_manager = MyDomainManager.MyDomainManager(command_manager)

import asyncio


async def get_me_a_b(index=0) -> B.B:
  available_B = await my_domain_manager.list_filtered(MyDomain.B.B)
  if len(available_B["list"]) <= index:
    new_b = await my_domain_manager.create(MyDomain.B.B(
      gemId=await my_domain_manager.get_free_gem_id(),
    ))
    return new_b
  return await my_domain_manager.get(MyDomain.B.B, available_B["list"][index])


async def inputhandler():
  """
  Handle the inputs send from the test
  """
  await asyncio.sleep(1)
  print("sleep done")

  if True:
    import textwrap
    import time
    global_vars = globals()
    local_vars = {}
    while True:
      x = await ainput(None)
      if x:
        x = x.replace("###", "\n")
        indented_code = textwrap.indent(x + '\nreturn locals()', "    ")
        # Wrap in an async function
        wrapped = f"async def __user_func__():\n{indented_code}\n"
        print(wrapped, file=sys.stderr)
        # Prepare a namespace for execution
        try:
          exec(wrapped, global_vars, local_vars)

          # Await the generated async function
          new_locals = await local_vars["__user_func__"]()
          global_vars = global_vars | new_locals
        except:
          import traceback
          traceback.print_exc()
        print("---")
      else:
        await asyncio.sleep(1)
        print("no input=")
  else:
    x = 42
    print(x)
    print("hello")
    my_b = await get_me_a_b()
    print(my_b)
    my_b2 = await get_me_a_b(1)

    available_A = await my_domain_manager.list_filtered(MyDomain.A.A)
    print(available_A)
    if len(available_A["list"]) > 0:
      print("yay")
      my_a = await my_domain_manager.get(MyDomain.A.A, available_A["list"][0])
    else:
      my_a = await my_domain_manager.create(MyDomain.A.A(
        gemId=await my_domain_manager.get_free_gem_id(),
        b=my_b, directB=my_b
      ))
      print(my_a)

    x = await ainput("Test")
    print("x")
    print(x)
    print(type(my_b2))
    await my_a.setB(my_b2.gemId, montigem_runtime.gembase.NotificationScope.ALL)

    print("all done!")


import MyDomain.MyDomainCommandHandler

cmd_handler = MyDomain.MyDomainCommandHandler.MyDomainCommandHandler()


async def main():
  async def external_consumer():
    """Simulates an external event loop consuming messages from the incoming queue."""
    while True:
      message = await command_manager.queue_in.get()  # Wait for a message
      print(f"Received message: {message}", file=sys.stderr)
      if "commandType" in message:
        await cmd_handler.handleCommand(message, my_domain_manager)
      else:
        print("TODO")

  host = "localhost"
  port = "8081"
  if len(sys.argv) == 2:
    port = sys.argv[1]

  async with asyncio.TaskGroup() as tg:
    # register the tasks as part of a task group
    # the command-managers connection
    tg.create_task(command_manager.connect(host=f"ws://{host}:{port}/umlp/api/command"))
    # its forwarding of task to the command handler
    tg.create_task(command_manager.command_forwarding_consumer(cmd_handler, my_domain_manager))
    # and finally our own logic
    tg.create_task(inputhandler())


if __name__ == "__main__":
  asyncio.run(main())
