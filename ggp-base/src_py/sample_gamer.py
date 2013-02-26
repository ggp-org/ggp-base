'''
@author: Sam
'''

import random

from org.ggp.base.util.statemachine import MachineState
from org.ggp.base.util.statemachine.implementation.prover import ProverStateMachine
from org.ggp.base.player.gamer.statemachine import StateMachineGamer

class SamplePythonGamer(StateMachineGamer):

    def getName(self):
        pass
        
    def stateMachineMetaGame(self, timeout):
        pass

    def stateMachineSelectMove(self, timeout):
        moves = self.getStateMachine().getLegalMoves(self.getCurrentState(), self.getRole())
        selection = random.choice(moves)
        return selection
        
    def stateMachineStop(self):
        pass
        
    def stateMachineAbort(self):
        pass        
        
    def getInitialStateMachine(self):
        return ProverStateMachine()