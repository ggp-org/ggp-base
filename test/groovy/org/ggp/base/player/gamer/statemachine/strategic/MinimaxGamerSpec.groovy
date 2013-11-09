package org.ggp.base.player.gamer.statemachine.strategic

import static org.junit.Assert.assertNotNull

import org.ggp.base.util.game.Game
import org.ggp.base.util.gdl.grammar.Gdl
import org.ggp.base.util.gdl.grammar.GdlConstant
import org.ggp.base.util.gdl.grammar.GdlPool
import org.ggp.base.util.match.Match
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException

import spock.lang.Specification

//String theRulesheet = '( ( role white ) ( role black ) ( <= ( base ( cell ?M ?N x ) ) ( index ?M ) ( index ?N ) ) ( <= ( base ( cell ?M ?N o ) ) ( index ?M ) ( index ?N ) ) ( <= ( base ( cell ?M ?N b ) ) ( index ?M ) ( index ?N ) ) ( base ( control white ) ) ( base ( control black ) ) ( <= ( input ?R ( mark 2 1 ) ) ( role ?R ) ) ( <= ( input ?R ( mark 2 3 ) ) ( role ?R ) ) ( <= ( input ?R ( mark 3 1 ) ) ( role ?R ) ) ( <= ( input ?R noop ) ( role ?R ) ) ( index 1 ) ( index 2 ) ( index 3 ) ( init ( cell 1 1 x ) ) ( init ( cell 1 2 x ) ) ( init ( cell 1 3 o ) ) ( init ( cell 2 1 b ) ) ( init ( cell 2 2 o ) ) ( init ( cell 2 3 b ) ) ( init ( cell 3 1 b ) ) ( init ( cell 3 2 o ) ) ( init ( cell 3 3 x ) ) ( init ( control white ) ) ( <= ( legal ?W ( mark ?X ?Y ) ) ( true ( cell ?X ?Y b ) ) ( true ( control ?W ) ) ) ( <= ( legal white noop ) ( true ( control black ) ) ) ( <= ( legal black noop ) ( true ( control white ) ) ) ( <= ( next ( cell ?M ?N x ) ) ( does white ( mark ?M ?N ) ) ( true ( cell ?M ?N b ) ) ) ( <= ( next ( cell ?M ?N o ) ) ( does black ( mark ?M ?N ) ) ( true ( cell ?M ?N b ) ) ) ( <= ( next ( cell ?M ?N ?W ) ) ( true ( cell ?M ?N ?W ) ) ( distinct ?W b ) ) ( <= ( next ( cell ?M ?N b ) ) ( does ?W ( mark ?J ?K ) ) ( true ( cell ?M ?N b ) ) ( or ( distinct ?M ?J ) ( distinct ?N ?K ) ) ) ( <= ( next ( control white ) ) ( true ( control black ) ) ) ( <= ( next ( control black ) ) ( true ( control white ) ) ) ( <= ( row ?M ?X ) ( true ( cell ?M 1 ?X ) ) ( true ( cell ?M 2 ?X ) ) ( true ( cell ?M 3 ?X ) ) ) ( <= ( column ?N ?X ) ( true ( cell 1 ?N ?X ) ) ( true ( cell 2 ?N ?X ) ) ( true ( cell 3 ?N ?X ) ) ) ( <= ( diagonal ?X ) ( true ( cell 1 1 ?X ) ) ( true ( cell 2 2 ?X ) ) ( true ( cell 3 3 ?X ) ) ) ( <= ( diagonal ?X ) ( true ( cell 1 3 ?X ) ) ( true ( cell 2 2 ?X ) ) ( true ( cell 3 1 ?X ) ) ) ( <= ( line ?X ) ( row ?M ?X ) ) ( <= ( line ?X ) ( column ?M ?X ) ) ( <= ( line ?X ) ( diagonal ?X ) ) ( <= open ( true ( cell ?M ?N b ) ) ) ( <= ( goal white 100 ) ( line x ) ( not ( line o ) ) ) ( <= ( goal white 50 ) ( line x ) ( line o ) ) ( <= ( goal white 50 ) ( not ( line x ) ) ( not ( line o ) ) ) ( <= ( goal white 0 ) ( not ( line x ) ) ( line o ) ) ( <= ( goal black 100 ) ( not ( line x ) ) ( line o ) ) ( <= ( goal black 50 ) ( line x ) ( line o ) ) ( <= ( goal black 50 ) ( not ( line x ) ) ( not ( line o ) ) ) ( <= ( goal black 0 ) ( line x ) ( not ( line o ) ) ) ( <= terminal ( line x ) ) ( <= terminal ( line o ) ) ( <= terminal ( not open ) ) )'
class MinimaxGamerSpec extends Specification {

	def "game cannot proceed if there are no game rules"() {
		given:
		def noGameRules = new ArrayList<Gdl>()
		GdlConstant WHITE_PLAYER = GdlPool.getConstant("white")
		long timeout = 10000

		MinimaxGamer theGamer = getGamer(noGameRules, WHITE_PLAYER)
		theGamer.metaGame(timeout)

		when:
		theGamer.stateMachineSelectMove(timeout)

		then:
		thrown MoveDefinitionException
	}

	private MinimaxGamer getGamer(ArrayList noGameRules, GdlConstant WHITE_PLAYER) {
		Game theGame = getGame(noGameRules)
		Match theMatch = getMatch(theGame)

		getStrategicGamer(theMatch, WHITE_PLAYER)
	}

	private Game getGame(ArrayList gameRules) {
		Game theGame = Mock(Game) //new Game(theKey, theName, theDescription, theRepositoryURL, theStylesheet, theRulesheet)
		theGame.rules >> gameRules

		theGame
	}

	private Match getMatch(Game theGame) {
		Match theMatch = Mock(Match)//new Match( matchId,  previewClock,  startClock,  playClock,  theGame)
		theMatch.game >> theGame

		theMatch
	}

	private MinimaxGamer getStrategicGamer(Match theMatch, GdlConstant roleName) {
		MinimaxGamer gamer = new MinimaxGamer()
		gamer.match = theMatch
		gamer.roleName = roleName

		gamer
	}
}
