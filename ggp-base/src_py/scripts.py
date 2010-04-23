# This is a library of short scripts for use in the Python console.
# These should make is easier to experiment with the GGP base classes
# from a Python command line.
#
# -Sam

from util.kif import KifReader
from util.configuration import ProjectConfiguration
from util.statemachine.implementation.prover import ProverStateMachine

def load_game(game_name):
    game_filename = ProjectConfiguration.gameRulesheetsPath + game_name + ".kif"
    game_description = KifReader.read(game_filename)
    machine = ProverStateMachine()
    machine.initialize(game_description)
    return machine
    
def display_random_walk(machine):
    state = machine.getInitialState()
    while not machine.isTerminal(state):
        print state
        state = machine.getRandomNextState(state)