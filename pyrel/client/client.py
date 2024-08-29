import websockets
import asyncio

async def handle_message(message):
    if message.startswith("REQUEST_ACTION"):
        parts = message.split()
        action = input(f"Your action (CALL, RAISE, FOLD): ")
        await websocket.send(action)
    elif message.startswith("REQUEST_RAISE_AMOUNT"):
        raise_amount = input("Enter raise amount: ")
        await websocket.send(raise_amount)
    else:
        print("Received:", message)

async def client():
    async with websockets.connect("ws://server_address:port") as websocket:
        async for message in websocket:
            await handle_message(message)

asyncio.run(client())