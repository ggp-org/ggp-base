package org.ggp.base.scala.player.gamer.statemachine

import org.ggp.base.player.gamer.statemachine.StateMachineGamer
import org.ggp.base.util.game.Game
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine
import org.ggp.base.util.statemachine.{Move, StateMachine}
import scala.collection.JavaConversions

/**
 * Created by steve on 11/1/2015.
 */
class SampleMonteCarloGamer extends StateMachineGamer {
  import SampleMonteCarloGamer._

  def stateMachineSelectMove(l: Long): Move = {
    val endByTime = l - SAFETY_MARGIN
    val role = getRole
    val sm = getStateMachine
    val currState = getCurrentState
    val moves = JavaConversions.asScalaIterator(sm.getLegalMoves(currState, role).iterator())

    def findBestMoveFromMoves(moves: List[Move]): Move = {
      def improveMoveEstimatesUntil(estimates: Map[Move,Double], untilCond: => Boolean)(iteration: Int = 0): Map[Move,Double] = {
        def improveEstimate(currentEstimate: (Move, Double)): (Move, Double) =  currentEstimate match {
          case (move, score) => {
            val sampleScore: Double = sm.getGoal(sm.performDepthCharge(sm.getRandomNextState(currState, role, move), null), role)
            (move, (score*iteration + sampleScore)/(iteration+1))
          }
        }

        if ( untilCond ) {
          //println(s"$iteration iterations performed on ${moves.length} moves")
          estimates
        }
        else {
          val newEstimates = estimates map improveEstimate
          improveMoveEstimatesUntil(newEstimates, untilCond)(iteration+1)
        }
      }

      val initialEstimates = Map(moves map { m => (m,0.0) }: _*)
      val finalEstimates = improveMoveEstimatesUntil(initialEstimates, System.currentTimeMillis() >= endByTime)(0)
      //println(s"Final estimates: ${finalEstimates.toString}")

      finalEstimates.maxBy(_._2)._1
    }

    findBestMoveFromMoves(moves.toList)
  }

  def stateMachineAbort(): Unit = {}

  def stateMachineMetaGame(l: Long): Unit = {}

  def stateMachineStop(): Unit = {}

  def getInitialStateMachine: StateMachine = new ProverStateMachine()

  def getName: String = "Sample Scala MonteCarlo gamer"

  def preview(game: Game, l: Long): Unit = {}}

object SampleMonteCarloGamer
{
  val SAFETY_MARGIN = 1000
}
