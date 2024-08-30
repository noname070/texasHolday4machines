import asyncio
import websockets
import json

class PokerClient:
    def __init__(self, name, token, url="ws://localhost:7777/game"):
        self.url = url
        self.name = name
        self.token = token

    async def connect(self):
        async with websockets.connect(self.url) as websocket:
            await self.auth(websocket)
            await self.handle_communication(websocket)

    async def auth(self, websocket):
        await websocket.send(json.dumps( {
            "name": self.name,
            "token": self.token
        }))
        
        if (await websocket.recv() == "OK"):
            print(f"Authed as {self.name}:{self.token}")

    async def handle_communication(self, websocket):
        while True:
            try:
                self.handle_server_message(await websocket.recv())
                await self.prompt_for_action(websocket)
            except websockets.exceptions.ConnectionClosed as e:
                print(f"Connection closed: {e}")
                break

    def handle_server_message(self, message):
        print(f"Server: {message}")

    async def prompt_for_action(self, websocket):
        action = input("Enter action (BET, CALL, RAISE, FOLD, CHECK, ALL_IN): ").strip().upper()
        if action in ["BET", "RAISE"]:
            amount = int(input("Enter amount: ").strip())
            action_payload = {
                "action": action,
                "token": self.token,
                "amount": amount
            }
        else:
            action_payload = {
                "action": action,
                "token": self.token
            }
        
        await websocket.send(json.dumps(action_payload))

# Пример использования
if __name__ == "__main__":
    client = PokerClient("BIMBIMBAMBAM", "odawoj")
    asyncio.get_event_loop().run_until_complete(client.connect())