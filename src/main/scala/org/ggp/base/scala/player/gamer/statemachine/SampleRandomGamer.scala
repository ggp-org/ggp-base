package org.ggp.base.scala.player.gamer.statemachine

import org.ggp.base.player.gamer.statemachine.StateMachineGamer
import org.ggp.base.util.game.Game
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine
import org.ggp.base.util.statemachine.{Move, StateMachine}

/**
 * Created by steve on 11/1/2015.
 */
class SampleRandomGamer extends StateMachineGamer {
  def stateMachineSelectMove(l: Long): Move = {
    val sm = getStateMachine
    val state = getCurrentState
    val role = getRole

    sm.getRandomMove(state, role)
  }

  def stateMachineAbort(): Unit = {}

  def stateMachineMetaGame(l: Long): Unit = {}

  def stateMachineStop(): Unit = {}

  def getInitialStateMachine: StateMachine = new ProverStateMachine()

  def getName: String = "Sample Scala legal gamer"

  def preview(game: Game, l: Long): Unit = {}
}
