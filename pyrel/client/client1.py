import json
import asyncio
import websockets

class PokerClient:
    def __init__(self, base_url, name, token):
        self.base_url = base_url
        self.name = name
        self.token = token
        self.session = None
        self.handlers = {}

    async def authenticate(self):
        uri = f"ws://{self.base_url}/game?name={self.name}&token={self.token}"
        self.session = await websockets.connect(uri)
        print("Connected and authenticated")

        response = await self.session.recv()
        if response != "OK":
            print("Failed to authenticate")
            self.session = None
            return False
        return True

    async def send_message(self, message):
        if self.session:
            await self.session.send(message)
            print(f"Sent: {message}")

    async def handle_message(self, message):
        try:
            data = json.loads(message)
            msg_type = data.get("type")
            handler = self.handlers.get(msg_type)
            if handler:
                await handler(data)
            else:
                print(f"No handler for message type: {msg_type}")
        except json.JSONDecodeError:
            print("Error decoding JSON")

    async def listen(self):
        async for message in self.session:
            print(f"Received: {message}")
            await self.handle_message(message)

    def register_handler(self, type):
        def decorator(func):
            self.handlers[type] = func
            return func
        return decorator

    async def run(self):
        if await self.authenticate():
            await self.listen()


async def main():
    client = PokerClient(base_url='localhost:7777', name="TESTUSER1", token="rlnRwPviMpOW")

    @client.register_handler(type="playerAction")
    async def player_action_handler(data):
        action = data.get("action")
        amount = data.get("amount")
        action_messages = {
            "BET": f"Bet with amount {amount}",
            "CALL": "Call",
            "RAISE": f"Raise with amount {amount}",
            "FOLD": "Fold",
            "CHECK": "Check",
            "ALL_IN": "All-in"
        }
        print(f"Player action: {action_messages.get(action, 'Unknown action')}")

    @client.register_handler(type="gameInfo")
    async def game_info_handler(data):
        winner = data.get("winner")
        board_cards = data.get("boardCards")
        player_states = data.get("playerStates")
        bank = data.get("bank")
        side_pods = data.get("sidePods")
        print(f"Game Info:\nWinner: {winner}\nBoard Cards: {board_cards}\nPlayer States: {player_states}\nBank: {bank}\nSide Pods: {side_pods}")

    await client.run()


if __name__ == "__main__":
    asyncio.run(main())