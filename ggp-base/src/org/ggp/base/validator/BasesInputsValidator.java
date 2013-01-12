package org.ggp.base.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.game.CloudGameRepository;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;


public class BasesInputsValidator {
	private static final String theURL = "http://games.ggp.org/base/";

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		GameRepository gameRepo = new CloudGameRepository(theURL);

		for (String gameKey : gameRepo.getGameKeys()) {
			if (!gameKey.equals("amazons") //Skip games that currently result in out-of-memory errors
					&& !gameKey.equals("alexChess")) {
				Game game = gameRepo.getGame(gameKey);
				verifyBasesAndInputs(game.getName(), game.getRules(), 4000);
			}
		}
	}

	private static final GdlConstant BASE = GdlPool.getConstant("base");
	private static final GdlConstant INPUT = GdlPool.getConstant("input");
	private static final GdlConstant TRUE = GdlPool.getConstant("true");
	private static final GdlConstant LEGAL = GdlPool.getConstant("legal");
	private static final GdlVariable X = GdlPool.getVariable("?x");
	private static final GdlVariable Y = GdlPool.getVariable("?y");

	public static void verifyBasesAndInputs(String name, List<Gdl> rules, int millisecondsToTest) throws MoveDefinitionException, TransitionDefinitionException {
		System.out.println("Verifying bases and inputs for " + name);
		try {
			StateMachine sm = new ProverStateMachine();
			sm.initialize(rules);

			AimaProver prover = new AimaProver(new HashSet<Gdl>(rules));
			GdlSentence basesQuery = GdlPool.getRelation(BASE, new GdlTerm[] {X});
			Set<GdlSentence> bases = prover.askAll(basesQuery, Collections.<GdlSentence>emptySet());
			GdlSentence inputsQuery = GdlPool.getRelation(INPUT, new GdlTerm[] {X, Y});
			Set<GdlSentence> inputs = prover.askAll(inputsQuery, Collections.<GdlSentence>emptySet());
			System.out.println("Bases: " + bases);
			System.out.println("Inputs: " + inputs);
			Set<GdlSentence> truesFromBases = new HashSet<GdlSentence>();
			for (GdlSentence base : bases) {
				truesFromBases.add(GdlPool.getRelation(TRUE, base.getBody()));
			}
			Set<GdlSentence> legalsFromInputs = new HashSet<GdlSentence>();
			for (GdlSentence input : inputs) {
				legalsFromInputs.add(GdlPool.getRelation(LEGAL, input.getBody()));
			}
			
			if (truesFromBases.isEmpty() && legalsFromInputs.isEmpty()) {
				System.out.println("No bases or inputs found.");
				return;
			}

			MachineState initialState = sm.getInitialState();
			MachineState state = initialState;
			long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() < startTime + millisecondsToTest) {
				//Check state against bases, inputs
				if (!truesFromBases.isEmpty()) {
					if (!truesFromBases.containsAll(state.getContents())) {
						Set<GdlSentence> missingBases = new HashSet<GdlSentence>();
						missingBases.addAll(state.getContents());
						missingBases.removeAll(truesFromBases);
						throw new RuntimeException("Found missing bases in " + name + ": " + missingBases);
					}
				}
				
				if (!legalsFromInputs.isEmpty()) {
					List<GdlSentence> legalSentences = new ArrayList<GdlSentence>();
					for (Role role : sm.getRoles()) {
						List<Move> legalMoves = sm.getLegalMoves(state, role);
						for (Move move : legalMoves) {
							legalSentences.add(GdlPool.getRelation(LEGAL, new GdlTerm[] {role.getName(), move.getContents()}));
						}
					}
					if (!legalsFromInputs.containsAll(legalSentences)) {
						Set<GdlSentence> missingInputs = new HashSet<GdlSentence>();
						missingInputs.addAll(legalSentences);
						missingInputs.removeAll(legalsFromInputs);
						throw new RuntimeException("Found missing inputs in " + name + ": " + missingInputs);
					}
				}
				
				state = sm.getRandomNextState(state);
				if (sm.isTerminal(state)) {
					state = initialState;
				}
			}
		} catch (RuntimeException e) {
			System.out.println("Ran into an error in game " + name);
			e.printStackTrace();
		} catch (StackOverflowError e) {
			System.out.println("Ran into an error in game " + name);
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			System.out.println("Ran into an error in game " + name);
			e.printStackTrace();
		}
	}

}
