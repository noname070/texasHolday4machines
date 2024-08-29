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
        self.dealers = cycle(range(players))

    def next_round(self):
        self.round_counter += 1
        
        self.deck = Deck()
        self.board = []
        
        current_dealer: Player = self.dealers.__next__()
        sb: Player = self.players[self.players.index(current_dealer)+1%len(self.players)-1]
        bb: Player = self.players[self.players.index(current_dealer)+2%len(self.players)-1]
        
        # pre-flop
        self.bets = {
            self.players[current_dealer] : 0,
            self.players[sb] : self.sb_value * (self.round_counter // self.sb_upgrade + 1),
            self.players[bb] : self.sb_value * (self.round_counter // self.sb_upgrade + 1) * 2
        }
    
    def wait_all_check(self): pass # ждем когда все игроки уравновесят ставки и bets у всех одинаковый.
    
    def next_state(self, n: int): pass # выкидываем на стол n карт и ждем уравновешивания ставок
    
    def final(self): pass # посчитать кто выиграл из оставшихся игроков. если комбинации одинаковые - выигрывает, у кого больше кикер. вернуть Player победителя 
    
    def calculate(self): pass # расчитать вероятности победы всех игроков. вернуть {Player : float,...}
    

def main():
    printcard = lambda x: f"[{x.rank.name}{x.suit.name[0]}]"

    players = [
        Player("BIBA"),
        Player("BOBA"),
        Player("YOBA"),
        Player("POOPA"),
        Player("LOOPA")
    ]
    
    deck = Deck()
    board = [deck.get_card() for _ in range(5)]

    for p in players:
        p.receive_card(deck.get_card())
        p.receive_card(deck.get_card())

    print("board:", ' '.join(map(printcard, board)))
    ranked = sorted(players, key=cmp_to_key(lambda p1, p2: compare_hands(p1, p2, board=board)), reverse=True)
    combos = [p.get_max_combo(board) for p in ranked]
    for i in range(len(ranked)):
        p = ranked[i]
        c = combos[i]
        print(f"name: {p.name:^5}, cards: ({' '.join(map(printcard, p.hand)):^12}), combo: {c[0].name}", end = "\n")

if __name__ == "__main__": main()