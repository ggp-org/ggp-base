package org.ggp.base.player.gamer.statemachine.sample

import static org.junit.Assert.assertNotNull

import org.ggp.base.game.TTTGameDescription
import org.ggp.base.util.game.Game
import org.ggp.base.util.gdl.grammar.GdlConstant
import org.ggp.base.util.gdl.grammar.GdlPool
import org.ggp.base.util.match.Match
import org.ggp.base.util.statemachine.Move

import spock.lang.Specification

class SampleMonteCarloGamerSpec extends Specification {
	private tttGameDescription = new TTTGameDescription()
	private String theRulesheet = tttGameDescription.ruleSheet

	def "Role in control with enough time will find the best move"() {
		given:
		GdlConstant WHITE_PLAYER = GdlPool.getConstant("white")
		long timeout = System.currentTimeMillis() + 6500

		SampleMonteCarloGamer theGamer = getGamer(WHITE_PLAYER)
		theGamer.metaGame(timeout)

		when:
		Move bestMove = theGamer.stateMachineSelectMove(timeout)

		then:
		bestMove.contents.toString() == '( mark 2 2 )'
	}

	def "Role not in control does not have a move to make"() {
		given:
		GdlConstant BLACK_PLAYER = GdlPool.getConstant("black")
		long timeout = 10000

		SampleMonteCarloGamer theGamer = getGamer(BLACK_PLAYER)
		theGamer.metaGame(timeout)

		when:
		Move bestMove = theGamer.stateMachineSelectMove(timeout)

		then:
		bestMove
		bestMove.contents == GdlPool.getConstant("noop")
	}

	private SampleMonteCarloGamer getGamer(GdlConstant WHITE_PLAYER) {
		Game theGame = getGame()
		Match theMatch = getMatch(theGame)

		getStrategicGamer(theMatch, WHITE_PLAYER)
	}

	private Game getGame() {
		def theKey
		def theName
		def theDescription
		def theRepositoryURL
		def theStylesheet

		Game theGame = new Game(theKey, theName, theDescription, theRepositoryURL, theStylesheet, theRulesheet)

		theGame
	}

	private Match getMatch(Game theGame) {
		Match theMatch = Mock(Match)
		theMatch.game >> theGame

		theMatch
	}

	private SampleMonteCarloGamer getStrategicGamer(Match theMatch, GdlConstant roleName) {
		SampleMonteCarloGamer gamer = new SampleMonteCarloGamer()
		gamer.match = theMatch
		gamer.roleName = roleName

		gamer
	}
}
