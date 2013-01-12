package org.ggp.base.player.gamer.statemachine.parametric;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.ggp.base.apps.player.config.ConfigPanel;
import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import org.ggp.base.player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * ParametricPlayer is a player that's designed to be configured via a set of
 * parameters that can be adjusted without any code modifications. It presents
 * a nice user interface for setting these parameters, and stores them as JSON
 * when the user clicks "save" (and loads them automatically when new players
 * are created).
 * 
 * @author Sam Schreiber
 */
public final class ParametricGamer extends StateMachineGamer
{
	private JSONObject params;
	private boolean params_dirty;
	
	/**
	 * Does nothing
	 */
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Do nothing else.
	}

	private Random theRandom = new Random();
	
	/**
	 * Employs a configurable, parametrized algorithm that can be adjusted
	 * with knobs and parameters that can be tweaked without detailed knowledge
	 * of the code for the player.
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
	    StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishBy = timeout - 1000;
		
		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = (moves.get(new Random().nextInt(moves.size())));

		try {
			if (params.has("style") && params.getString("strategy").equals("Simple")) {
				// Shuffle the moves into a random order, so that when we find the first
				// move that doesn't give our opponent a forced win, we aren't always choosing
				// the first legal move over and over (which is visibly repetitive).
				List<Move> movesInRandomOrder = new ArrayList<Move>();
				while(!moves.isEmpty()) {
				    Move aMove = moves.get(theRandom.nextInt(moves.size()));
				    movesInRandomOrder.add(aMove);
				    moves.remove(aMove);
				}
				
				// Go through all of the legal moves in a random over, and consider each one.
				// For each move, we want to determine whether taking that move will give our
				// opponent a one-move win. We're also interested in whether taking that move
				// will immediately cause us to win or lose.
				//
				// Our main goal is to find a move which won't give our opponent a one-move win.
				// We will also continue considering moves for two seconds, in case we can stumble
				// upon a move which would cause us to win: if we find such a move, we will just
				// ithis.mmediately take it.
				boolean reasonableMoveFound = false;
				int maxGoal = 0;
				for(Move moveUnderConsideration : movesInRandomOrder) {
				    // Check to see if there's time to continue.
				    if(System.currentTimeMillis() > finishBy) break;
				    
				    // If we've found a reasonable move, only spend at most two seconds trying
				    // to find a winning move.
				    if(System.currentTimeMillis() > start + 2000 && reasonableMoveFound) break;
				    
				    // Get the next state of the game, if we take the move we're considering.
				    // Since it's our turn, in an alternating-play game the opponent will only
				    // have one legal move, and so calling "getRandomJointMove" with our move
				    // fixed will always return the joint move consisting of our move and the
				    // opponent's no-op. In a simultaneous-action game, however, the opponent
				    // may have many moves, and so we will randomly pick one of our opponent's
				    // possible actions and assume they do that.					
				    MachineState nextState = theMachine.getNextState(getCurrentState(), theMachine.getRandomJointMove(getCurrentState(), getRole(), moveUnderConsideration));
				    
				    // Does the move under consideration end the game? If it does, do we win
				    // or lose? If we lose, don't bother considering it. If we win, then we
				    // definitely want to take this move. If its goal is better than our current
				    // best goal, go ahead and tentatively select it
				    if(theMachine.isTerminal(nextState)) {
				        if(theMachine.getGoal(nextState, getRole()) == 0) {
				            continue;
				        } else if(theMachine.getGoal(nextState, getRole()) == 100) {
			                selection = moveUnderConsideration;
			                break;
				        } else { 	
				        	if (theMachine.getGoal(nextState, getRole()) > maxGoal)
				        	{
				        		selection = moveUnderConsideration;
				        		maxGoal = theMachine.getGoal(nextState, getRole()); 
				        	}
				        	continue;
				        }
				    }
				    
				    // Check whether any of the legal joint moves from this state lead to
				    // a loss for us. Again, this only makes sense in the context of an alternating
				    // play zero-sum game, in which this is the opponent's move and they are trying
				    // to make us lose, and so if they are offered any move that will make us lose
				    // they will take it.
				    boolean forcedLoss = false;		    		    
				    for(List<Move> jointMove : theMachine.getLegalJointMoves(nextState)) {
				        MachineState nextNextState = theMachine.getNextState(nextState, jointMove);
				        if(theMachine.isTerminal(nextNextState)) {
				            if(theMachine.getGoal(nextNextState, getRole()) == 0) {
				                forcedLoss = true;
				                break;
				            }
				        }
				        
				        // Check to see if there's time to continue.
				        if(System.currentTimeMillis() > finishBy) {
				            forcedLoss = true;
				            break;
				        }
				    }
				    
				    // If we've verified that this move isn't going to lead us to a state where
				    // our opponent can defeat us in one move, we should keep track of it.
				    if(!forcedLoss) {
				        selection = moveUnderConsideration;
				        reasonableMoveFound = true;
				    }
				}
			}
		} catch (JSONException je) {
			throw new RuntimeException(je);
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new ReflexMoveSelectionEvent(moves, selection, stop - start));
		return selection;
	}
	@Override
	public void stateMachineStop() {
		// Do nothing.
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}

	@Override
	public String getName() {
		return "Parametric";
	}
	
	class ParametricConfigPanel extends ConfigPanel implements ActionListener, DocumentListener {
		private static final long serialVersionUID = 1L;

		final JButton saveButton;
		final JTextField styleField;
		final JComboBox strategyField;
		public ParametricConfigPanel() {
			super(new FlowLayout());

			this.add(new JLabel("Parametric player config."));

			strategyField = new JComboBox(new String[] { "Simple", "Random" });			
		    this.add(strategyField);
			
			styleField = new JTextField();
			styleField.setColumns(20);
			styleField.setVisible(false); // not yet used
		    this.add(styleField);
		    		    
		    saveButton = new JButton(saveButtonMethod());
		    saveButton.setEnabled(false);
		    this.add(saveButton);
		    
			loadParamsJSON();
			setUIfromJSON();
			
			strategyField.addActionListener(this);
			styleField.getDocument().addDocumentListener(this);
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			syncJSONtoUI();
		}		
		@Override
		public void changedUpdate(DocumentEvent e) {
			syncJSONtoUI();
		}
		@Override
		public void insertUpdate(DocumentEvent e) {
			syncJSONtoUI();
		}
		@Override
		public void removeUpdate(DocumentEvent e) {
			syncJSONtoUI();
		}		
		
		void syncJSONtoUI() {
			JSONObject newParams = getJSONfromUI();
			if (!newParams.toString().equals(params.toString())) {
				params_dirty = true;
				params = newParams;				
			}
			saveButton.setEnabled(params_dirty);
		}
		
		JSONObject getJSONfromUI() {
			JSONObject newParams = new JSONObject();
			try {
				if (!styleField.getText().isEmpty()) {
					newParams.put("style", styleField.getText());
				}
				newParams.put("strategy", strategyField.getSelectedItem().toString());
			} catch (JSONException je) {
				je.printStackTrace();
			}
			return newParams;
		}
		
		void setUIfromJSON() {
			try {
				if (params.has("style")) {
					styleField.setText(params.getString("style"));					
				}
				if (params.has("strategy")) {
					strategyField.setSelectedItem(params.getString("strategy"));
				}
			} catch (JSONException je) {
				je.printStackTrace();
			}
		}
		
		void loadParamsJSON() {
			params = new JSONObject();
			String paramsFilename = System.getProperty("user.home") + "/.ggp-gamer-params.json";
			File paramsFile = new File(paramsFilename);
			if (!paramsFile.exists()) {				
				return;
			}
			try {
				String line;
				StringBuilder pdata = new StringBuilder();
				FileInputStream fis = new FileInputStream(paramsFilename);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
				try {
					while ((line = br.readLine()) != null) {
						pdata.append(line);
					}
				} finally {
					br.close();
				}
				params = new JSONObject(pdata.toString());
				params_dirty = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		void saveParamsJSON() {
			try {
				String paramsFilename = System.getProperty("user.home") + "/.ggp-gamer-params.json";
				File paramsFile = new File(paramsFilename);
				if (!paramsFile.exists()) {				
					paramsFile.createNewFile();
				}
				FileWriter fw = new FileWriter(paramsFile);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(params.toString());
				bw.close();
				params_dirty = false;
				saveButton.setEnabled(false);
			} catch (IOException ie) {
				ie.printStackTrace();
			}
		}
		
		AbstractAction saveButtonMethod() {
			return new AbstractAction("Save") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent evt) {
					saveParamsJSON();
				}
			};
		}
	}

	@Override
	public ConfigPanel getConfigPanel() {
		return new ParametricConfigPanel();
	}
	
	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}
}