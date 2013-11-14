package org.ggp.base.util.propnet.architecture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.statemachine.Role;


/**
 * The PropNet class is designed to represent Propositional Networks.
 *
 * A propositional network (also known as a "propnet") is a way of representing
 * a game as a logic circuit. States of the game are represented by assignments
 * of TRUE or FALSE to "base" propositions, each of which represents a single
 * fact that can be true about the state of the game. For example, in a game of
 * Tic-Tac-Toe, the fact (cell 1 1 x) indicates that the cell (1,1) has an 'x'
 * in it. That fact would correspond to a base proposition, which would be set
 * to TRUE to indicate that the fact is true in the current state of the game.
 * Likewise, the base corresponding to the fact (cell 1 1 o) would be false,
 * because in that state of the game there isn't an 'o' in the cell (1,1).
 *
 * A state of the game is uniquely determined by the assignment of truth values
 * to the base propositions in the propositional network. Every assignment of
 * truth values to base propositions corresponds to exactly one unique state of
 * the game.
 *
 * Given the values of the base propositions, you can use the connections in
 * the network (AND gates, OR gates, NOT gates) to determine the truth values
 * of other propositions. For example, you can determine whether the terminal
 * proposition is true: if that proposition is true, the game is over when it
 * reaches this state. Otherwise, if it is false, the game isn't over. You can
 * also determine the value of the goal propositions, which represent facts
 * like (goal xplayer 100). If that proposition is true, then that fact is true
 * in this state of the game, which means that xplayer has 100 points.
 *
 * You can also use a propositional network to determine the next state of the
 * game, given the current state and the moves for each player. First, you set
 * the input propositions which correspond to each move to TRUE. Once that has
 * been done, you can determine the truth value of the transitions. Each base
 * proposition has a "transition" component going into it. This transition has
 * the truth value that its base will take on in the next state of the game.
 *
 * For further information about propositional networks, see:
 *
 * "Decomposition of Games for Efficient Reasoning" by Eric Schkufza.
 * "Factoring General Games using Propositional Automata" by Evan Cox et al.
 *
 * @author Sam Schreiber
 */

public final class PropNet
{
	/** References to every component in the PropNet. */
	private final Set<Component> components;

	/** References to every Proposition in the PropNet. */
	private final Set<Proposition> propositions;

	/** References to every BaseProposition in the PropNet, indexed by name. */
	private final Map<GdlSentence, Proposition> basePropositions;

	/** References to every InputProposition in the PropNet, indexed by name. */
	private final Map<GdlSentence, Proposition> inputPropositions;

	/** References to every LegalProposition in the PropNet, indexed by role. */
	private final Map<Role, Set<Proposition>> legalPropositions;

	/** References to every GoalProposition in the PropNet, indexed by role. */
	private final Map<Role, Set<Proposition>> goalPropositions;

	/** A reference to the single, unique, InitProposition. */
	private final Proposition initProposition;

	/** A reference to the single, unique, TerminalProposition. */
	private final Proposition terminalProposition;

	/** A helper mapping between input/legal propositions. */
	private final Map<Proposition, Proposition> legalInputMap;

	/** A helper list of all of the roles. */
	private final List<Role> roles;

	public void addComponent(Component c)
	{
		components.add(c);
		if (c instanceof Proposition) propositions.add((Proposition)c);
	}

	/**
	 * Creates a new PropNet from a list of Components, along with indices over
	 * those components.
	 *
	 * @param components
	 *            A list of Components.
	 */
	public PropNet(List<Role> roles, Set<Component> components)
	{

	    this.roles = roles;
		this.components = components;
		this.propositions = recordPropositions();
		this.basePropositions = recordBasePropositions();
		this.inputPropositions = recordInputPropositions();
		this.legalPropositions = recordLegalPropositions();
		this.goalPropositions = recordGoalPropositions();
		this.initProposition = recordInitProposition();
		this.terminalProposition = recordTerminalProposition();
		this.legalInputMap = makeLegalInputMap();
	}

	public List<Role> getRoles()
	{
	    return roles;
	}

	public Map<Proposition, Proposition> getLegalInputMap()
	{
		return legalInputMap;
	}

	private Map<Proposition, Proposition> makeLegalInputMap() {
		Map<Proposition, Proposition> legalInputMap = new HashMap<Proposition, Proposition>();
		// Create a mapping from Body->Input.
		Map<List<GdlTerm>, Proposition> inputPropsByBody = new HashMap<List<GdlTerm>, Proposition>();
		for(Proposition inputProp : inputPropositions.values()) {
			List<GdlTerm> inputPropBody = (inputProp.getName()).getBody();
			inputPropsByBody.put(inputPropBody, inputProp);
		}
		// Use that mapping to map Input->Legal and Legal->Input
		// based on having the same Body proposition.
		for(Set<Proposition> legalProps : legalPropositions.values()) {
			for(Proposition legalProp : legalProps) {
				List<GdlTerm> legalPropBody = (legalProp.getName()).getBody();
				if (inputPropsByBody.containsKey(legalPropBody)) {
    				Proposition inputProp = inputPropsByBody.get(legalPropBody);
    				legalInputMap.put(inputProp, legalProp);
    				legalInputMap.put(legalProp, inputProp);
				}
			}
		}
		return legalInputMap;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every BaseProposition in the PropNet, indexed by
	 *         name.
	 */
	public Map<GdlSentence, Proposition> getBasePropositions()
	{
		return basePropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every Component in the PropNet.
	 */
	public Set<Component> getComponents()
	{
		return components;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every GoalProposition in the PropNet, indexed by
	 *         player name.
	 */
	public Map<Role, Set<Proposition>> getGoalPropositions()
	{
		return goalPropositions;
	}

	/**
	 * Getter method. A reference to the single, unique, InitProposition.
	 *
	 * @return
	 */
	public Proposition getInitProposition()
	{
		return initProposition;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every InputProposition in the PropNet, indexed by
	 *         name.
	 */
	public Map<GdlSentence, Proposition> getInputPropositions()
	{
		return inputPropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every LegalProposition in the PropNet, indexed by
	 *         player name.
	 */
	public Map<Role, Set<Proposition>> getLegalPropositions()
	{
		return legalPropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every Proposition in the PropNet.
	 */
	public Set<Proposition> getPropositions()
	{
		return propositions;
	}

	/**
	 * Getter method.
	 *
	 * @return A reference to the single, unique, TerminalProposition.
	 */
	public Proposition getTerminalProposition()
	{
		return terminalProposition;
	}

	/**
	 * Returns a representation of the PropNet in .dot format.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("digraph propNet\n{\n");
		for ( Component component : components )
		{
			sb.append("\t" + component.toString() + "\n");
		}
		sb.append("}");

		return sb.toString();
	}

	/**
     * Outputs the propnet in .dot format to a particular file.
     * This can be viewed with tools like Graphviz and ZGRViewer.
     *
     * @param filename the name of the file to output to
     */
    public void renderToFile(String filename) {
        try {
            File f = new File(filename);
            FileOutputStream fos = new FileOutputStream(f);
            OutputStreamWriter fout = new OutputStreamWriter(fos, "UTF-8");
            fout.write(toString());
            fout.close();
            fos.close();
        } catch(Exception e) {
            GamerLogger.logStackTrace("StateMachine", e);
        }
    }

	/**
	 * Builds an index over the BasePropositions in the PropNet.
	 *
	 * This is done by going over every single-input proposition in the network,
	 * and seeing whether or not its input is a transition, which would mean that
	 * by definition the proposition is a base proposition.
	 *
	 * @return An index over the BasePropositions in the PropNet.
	 */
	private Map<GdlSentence, Proposition> recordBasePropositions()
	{
		Map<GdlSentence, Proposition> basePropositions = new HashMap<GdlSentence, Proposition>();
		for (Proposition proposition : propositions) {
		    // Skip all propositions without exactly one input.
		    if (proposition.getInputs().size() != 1)
		        continue;

			Component component = proposition.getSingleInput();
			if (component instanceof Transition) {
				basePropositions.put(proposition.getName(), proposition);
			}
		}

		return basePropositions;
	}

	/**
	 * Builds an index over the GoalPropositions in the PropNet.
	 *
	 * This is done by going over every function proposition in the network
     * where the name of the function is "goal", and extracting the name of the
     * role associated with that goal proposition, and then using those role
     * names as keys that map to the goal propositions in the index.
	 *
	 * @return An index over the GoalPropositions in the PropNet.
	 */
	private Map<Role, Set<Proposition>> recordGoalPropositions()
	{
		Map<Role, Set<Proposition>> goalPropositions = new HashMap<Role, Set<Proposition>>();
		for (Proposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlRelations.
		    if (!(proposition.getName() instanceof GdlRelation))
		        continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (!relation.getName().getValue().equals("goal"))
			    continue;

			Role theRole = new Role((GdlConstant) relation.get(0));
			if (!goalPropositions.containsKey(theRole)) {
				goalPropositions.put(theRole, new HashSet<Proposition>());
			}
			goalPropositions.get(theRole).add(proposition);
		}

		return goalPropositions;
	}

	/**
	 * Returns a reference to the single, unique, InitProposition.
	 *
	 * @return A reference to the single, unique, InitProposition.
	 */
	private Proposition recordInitProposition()
	{
		for (Proposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlPropositions.
			if (!(proposition.getName() instanceof GdlProposition))
			    continue;

			GdlConstant constant = ((GdlProposition) proposition.getName()).getName();
			if (constant.getValue().toUpperCase().equals("INIT")) {
				return proposition;
			}
		}
		return null;
	}

	/**
	 * Builds an index over the InputPropositions in the PropNet.
	 *
	 * @return An index over the InputPropositions in the PropNet.
	 */
	private Map<GdlSentence, Proposition> recordInputPropositions()
	{
		Map<GdlSentence, Proposition> inputPropositions = new HashMap<GdlSentence, Proposition>();
		for (Proposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlFunctions.
			if (!(proposition.getName() instanceof GdlRelation))
			    continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (relation.getName().getValue().equals("does")) {
				inputPropositions.put(proposition.getName(), proposition);
			}
		}

		return inputPropositions;
	}

	/**
	 * Builds an index over the LegalPropositions in the PropNet.
	 *
	 * @return An index over the LegalPropositions in the PropNet.
	 */
	private Map<Role, Set<Proposition>> recordLegalPropositions()
	{
		Map<Role, Set<Proposition>> legalPropositions = new HashMap<Role, Set<Proposition>>();
		for (Proposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlRelations.
			if (!(proposition.getName() instanceof GdlRelation))
			    continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (relation.getName().getValue().equals("legal")) {
				GdlConstant name = (GdlConstant) relation.get(0);
				Role r = new Role(name);
				if (!legalPropositions.containsKey(r)) {
					legalPropositions.put(r, new HashSet<Proposition>());
				}
				legalPropositions.get(r).add(proposition);
			}
		}

		return legalPropositions;
	}

	/**
	 * Builds an index over the Propositions in the PropNet.
	 *
	 * @return An index over Propositions in the PropNet.
	 */
	private Set<Proposition> recordPropositions()
	{
		Set<Proposition> propositions = new HashSet<Proposition>();
		for (Component component : components)
		{
			if (component instanceof Proposition) {
				propositions.add((Proposition) component);
			}
		}
		return propositions;
	}

	/**
	 * Records a reference to the single, unique, TerminalProposition.
	 *
	 * @return A reference to the single, unqiue, TerminalProposition.
	 */
	private Proposition recordTerminalProposition()
	{
		for ( Proposition proposition : propositions )
		{
			if ( proposition.getName() instanceof GdlProposition )
			{
				GdlConstant constant = ((GdlProposition) proposition.getName()).getName();
				if ( constant.getValue().equals("terminal") )
				{
					return proposition;
				}
			}
		}

		return null;
	}

	public int getSize() {
		return components.size();
	}

	public int getNumAnds() {
		int andCount = 0;
		for(Component c : components) {
			if(c instanceof And)
				andCount++;
		}
		return andCount;
	}

	public int getNumOrs() {
		int orCount = 0;
		for(Component c : components) {
			if(c instanceof Or)
				orCount++;
		}
		return orCount;
	}

	public int getNumNots() {
		int notCount = 0;
		for(Component c : components) {
			if(c instanceof Not)
				notCount++;
		}
		return notCount;
	}

	public int getNumLinks() {
		int linkCount = 0;
		for(Component c : components) {
			linkCount += c.getOutputs().size();
		}
		return linkCount;
	}

	/**
	 * Removes a component from the propnet. Be very careful when using
	 * this method, as it is not thread-safe. It is highly recommended
	 * that this method only be used in an optimization period between
	 * the propnet's creation and its initial use, during which it
	 * should only be accessed by a single thread.
	 *
	 * The INIT and terminal components cannot be removed.
	 */
	public void removeComponent(Component c) {


		//Go through all the collections it could appear in
		if(c instanceof Proposition) {
			Proposition p = (Proposition) c;
			GdlSentence name = p.getName();
			if(basePropositions.containsKey(name)) {
				basePropositions.remove(name);
			} else if(inputPropositions.containsKey(name)) {
				inputPropositions.remove(name);
				//The map goes both ways...
				Proposition partner = legalInputMap.get(p);
				if(partner != null) {
					legalInputMap.remove(partner);
					legalInputMap.remove(p);
				}
			} else if(name == GdlPool.getProposition(GdlPool.getConstant("INIT"))) {
				throw new RuntimeException("The INIT component cannot be removed. Consider leaving it and ignoring it.");
			} else if(name == GdlPool.getProposition(GdlPool.getConstant("terminal"))) {
				throw new RuntimeException("The terminal component cannot be removed.");
			} else {
				for(Set<Proposition> propositions : legalPropositions.values()) {
					if(propositions.contains(p)) {
						propositions.remove(p);
						Proposition partner = legalInputMap.get(p);
						if(partner != null) {
							legalInputMap.remove(partner);
							legalInputMap.remove(p);
						}
					}
				}
				for(Set<Proposition> propositions : goalPropositions.values()) {
					propositions.remove(p);
				}
			}
			propositions.remove(p);
		}
		components.remove(c);

		//Remove all the local links to the component
		for(Component parent : c.getInputs())
			parent.removeOutput(c);
		for(Component child : c.getOutputs())
			child.removeInput(c);
		//These are actually unnecessary...
		//c.removeAllInputs();
		//c.removeAllOutputs();
	}
}