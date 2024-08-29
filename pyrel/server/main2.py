from pocker_gamelogic import *
from itertools import cycle
from functools import cmp_to_key


class Game:
    def __init__(self, *players: Player, start_capital:int = 10000, sb_value: int = 15, sb_upgrade: int = 10) -> None:
        self.players = players
        for p in self.players:
            p.capital = start_capital
            
        assert 3 < len(self.players) < 6, "ненормальное количество"
            
        self.sb = sb_value
        self.round_counter = 0
        self.sb_upgrade = sb_upgrade
        self.dealers = cycle(range(len(players)))

    def next_round(self):
        self.round_counter += 1
        
        self.deck = Deck()
        self.board = []
        
        current_dealer: Player = self.players[next(self.dealers)]
        sb: Player = self.players[(self.players.index(current_dealer)+1) % len(self.players)]
        bb: Player = self.players[(self.players.index(current_dealer)+2) % len(self.players)]
        
        # pre-flop
        self.bets = {
            current_dealer : 0,
            sb : self.sb * (self.round_counter // self.sb_upgrade + 1),
            bb : self.sb * (self.round_counter // self.sb_upgrade + 1) * 2
        }
        
        for p in self.players:
            p.receive_card(self.deck.get_card())
            p.receive_card(self.deck.get_card())
            # дать игроку статус ожидания хода
    
    def wait_all_check(self):
        max_bet = max(self.bets.values())
        while True:
            for player in self.players:
                if self.bets[player] < max_bet:
                    player_action = player.make_decision(max_bet - self.bets[player])
                    # тут как то надо связать сервер и спросить у игрока его действие.
                    if player_action == Actions.FOLD:
                        del self.bets[player] # 
                    else:
                        self.bets[player] += player_action
                    
            if all(bet == max_bet for bet in self.bets.values()):
                break
    
    def next_state(self, n: int):
        self.board += [self.deck.get_card() for _ in range(n)]
        self.wait_all_check()
    
    def final(self):
        remaining_players = [player for player in self.players if player in self.bets]
        
        if len(remaining_players) == 1:
            return remaining_players[0]
        
        winner = max(remaining_players, key=lambda player: player.get_max_combo(self.board))
        
        total_bet = sum(self.bets.values())
        winner.capital += total_bet

        return winner
    
    def calculate_success(self): # TODO
        probabilities = {}
        for player in self.players:
            cards = player.get_max_combo(self.board)[1]
            probabilities[player] = get_probability(cards, self.board)
        return probabilities

def main():
    printcard = lambda x: f"[{x.rank.name}{x.suit.name[0]}]"

    players = [
        Player("BIBA"),
        Player("BOBA"),
        Player("YOBA"),
        Player("POOPA"),
        Player("LOOPA")
    ]
    
    game = Game(*players)
    game.next_round()

    game.next_state(3) 
    game.next_state(1) 
    game.next_state(1) 
    
    print("Final winner:", game.final().name)

if __name__ == "__main__":
    main()