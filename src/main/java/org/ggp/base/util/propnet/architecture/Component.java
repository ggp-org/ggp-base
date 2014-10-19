package org.ggp.base.util.propnet.architecture;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The root class of the Component hierarchy, which is designed to represent
 * nodes in a PropNet. The general contract of derived classes is to override
 * all methods.
 */

public abstract class Component implements Serializable
{

	private static final long serialVersionUID = 352524175700224447L;
    /** The inputs to the component. */
    private final Set<Component> inputs;
    /** The outputs of the component. */
    private final Set<Component> outputs;

    /**
     * Creates a new Component with no inputs or outputs.
     */
    public Component()
    {
        this.inputs = new HashSet<Component>();
        this.outputs = new HashSet<Component>();
    }

    /**
     * Adds a new input.
     *
     * @param input
     *            A new input.
     */
    public void addInput(Component input)
    {
        inputs.add(input);
    }

    public void removeInput(Component input)
    {
    	inputs.remove(input);
    }

    public void removeOutput(Component output)
    {
    	outputs.remove(output);
    }

    public void removeAllInputs()
    {
		inputs.clear();
	}

	public void removeAllOutputs()
	{
		outputs.clear();
	}

    /**
     * Adds a new output.
     *
     * @param output
     *            A new output.
     */
    public void addOutput(Component output)
    {
        outputs.add(output);
    }

    /**
     * Getter method.
     *
     * @return The inputs to the component.
     */
    public Set<Component> getInputs()
    {
        return inputs;
    }

    /**
     * A convenience method, to get a single input.
     * To be used only when the component is known to have
     * exactly one input.
     *
     * @return The single input to the component.
     */
    public Component getSingleInput() {
        assert inputs.size() == 1;
        return inputs.iterator().next();
    }

    /**
     * Getter method.
     *
     * @return The outputs of the component.
     */
    public Set<Component> getOutputs()
    {
        return outputs;
    }

    /**
     * A convenience method, to get a single output.
     * To be used only when the component is known to have
     * exactly one output.
     *
     * @return The single output to the component.
     */
    public Component getSingleOutput() {
        assert outputs.size() == 1;
        return outputs.iterator().next();
    }

    /**
     * Returns the value of the Component.
     *
     * @return The value of the Component.
     */
    public abstract boolean getValue();

    /**
     * Returns a representation of the Component in .dot format.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

    /**
     * Returns a configurable representation of the Component in .dot format.
     *
     * @param shape
     *            The value to use as the <tt>shape</tt> attribute.
     * @param fillcolor
     *            The value to use as the <tt>fillcolor</tt> attribute.
     * @param label
     *            The value to use as the <tt>label</tt> attribute.
     * @return A representation of the Component in .dot format.
     */
    protected String toDot(String shape, String fillcolor, String label)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\"@" + Integer.toHexString(hashCode()) + "\"[shape=" + shape + ", style= filled, fillcolor=" + fillcolor + ", label=\"" + label + "\"]; ");
        for ( Component component : getOutputs() )
        {
            sb.append("\"@" + Integer.toHexString(hashCode()) + "\"->" + "\"@" + Integer.toHexString(component.hashCode()) + "\"; ");
        }

        return sb.toString();
    }

}