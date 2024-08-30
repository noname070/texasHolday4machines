import requests
import json
import websockets
import asyncio

class PokerClient:
    def __init__(self, server, name, token):
        self.server = server
        self.token = None
        self.session = None

    def register(self, username):
        response = requests.post(f'{self.server}/', json={'name': username})
        data = response.json()
        self.token = data['token']
        print(f"Registered as {username}, Token: {self.token}")

    def join_game(self, game_id):
        response = requests.post(f'{self.base_url}/join', json={'token': self.token, 'game_id': game_id})
        data = response.json()
        print(f"Joined game {game_id}. Players: {data['players']}")

    def start_game(self):
        response = requests.post(f'{self.base_url}/start', json={'token': self.token})
        if response.status_code == 200:
            print("Game started!")
        else:
            print(f"Error starting game: {response.text}")

    def get_state(self):
        response = requests.get(f'{self.base_url}/state', params={'token': self.token})
        data = response.json()
        print(f"Current game state: {json.dumps(data, indent=2)}")

    async def listen_for_updates(self):
        async with websockets.connect(f'{self.base_url.replace("http", "ws")}/ws') as websocket:
            await websocket.send(json.dumps({'token': self.token}))
            while True:
                update = await websocket.recv()
                print(f"Update received: {update}")

    def make_action(self, action, amount=None):
        payload = {'token': self.token, 'action': action}
        if amount is not None:
            payload['amount'] = amount
        response = requests.post(f'{self.base_url}/action', json=payload)
        if response.status_code == 200:
            print(f"Action {action} executed successfully.")
        else:
            print(f"Error executing action: {response.text}")


# Пример использования
if __name__ == "__main__":
    client = PokerClient(base_url='http://localhost:8000')

    # Зарегистрироваться как игрок
    client.register("Player1")

    # Присоединиться к игре
    client.join_game(game_id=1)

    # Начать игру
    client.start_game()

    # Запросить текущее состояние игры
    client.get_state()

    # Сделать действие (например, сделать ставку)
    client.make_action('BET', amount=100)

    # Асинхронно слушать обновления с сервера
    asyncio.get_event_loop().run_until_complete(client.listen_for_updates())