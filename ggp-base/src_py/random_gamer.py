'''
@author: Sam
'''

import random

from util.statemachine import MachineState
from util.statemachine.implementation.prover import ProverStateMachine

from player.gamer.statemachine import StateMachineGamer
from player.gamer.statemachine.reflex.event import ReflexMoveSelectionEvent

class RandomGamer(StateMachineGamer):

    def getName(self):
        return "RandomGamer (Python)"
        
    def stateMachineMetaGame(self, timeout):
        pass

    def stateMachineSelectMove(self, timeout):
        moves = self.getStateMachine().getLegalMoves(self.getCurrentState(), self.getRole())
        selection = random.choice(moves)
        self.notifyObservers(ReflexMoveSelectionEvent(moves, selection, 1))
        return selection
        
    def stateMachineStop():
    	pass
        
    def getInitialStateMachine(self):
        return ProverStateMachine()