'''
Created on Apr 2, 2010

@author: evancox
'''

from org.ggp.base.util.statemachine import MachineState
from org.ggp.base.util.statemachine.implementation.prover import ProverStateMachine

from org.ggp.base.player.gamer.statemachine import StateMachineGamer
from org.ggp.base.player.gamer.statemachine.reflex.event import ReflexMoveSelectionEvent

class LegalGamer(StateMachineGamer):

    def getName(self):
        return "LegalGamer (Python)"
        
    def stateMachineMetaGame(self, timeout):
        pass

    def stateMachineSelectMove(self, timeout):
        moves = self.getStateMachine().getLegalMoves(self.getCurrentState(), self.getRole())
        selection = moves[0]
        self.notifyObservers(ReflexMoveSelectionEvent(moves, selection, 1))
        return selection
        
    def stateMachineStop(self):
        pass
        
    def getInitialStateMachine(self):
        return ProverStateMachine()