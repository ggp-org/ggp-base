'''
@author: Sam
'''

import random

from org.ggp.base.util.statemachine import MachineState
from org.ggp.base.util.statemachine.implementation.prover import ProverStateMachine

from org.ggp.base.player.gamer.statemachine import StateMachineGamer
from org.ggp.base.player.gamer.statemachine.reflex.event import ReflexMoveSelectionEvent

class PythonRandomGamer(StateMachineGamer):

    def getName(self):
        pass
        
    def stateMachineMetaGame(self, timeout):
        pass

    def stateMachineSelectMove(self, timeout):
        moves = self.getStateMachine().getLegalMoves(self.getCurrentState(), self.getRole())
        selection = random.choice(moves)
        self.notifyObservers(ReflexMoveSelectionEvent(moves, selection, 1))
        return selection
        
    def stateMachineStop(self):
        pass
        
    def stateMachineAbort(self):
        pass        
        
    def getInitialStateMachine(self):
        return ProverStateMachine()