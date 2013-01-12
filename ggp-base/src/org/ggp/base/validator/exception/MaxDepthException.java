package org.ggp.base.validator.exception;

@SuppressWarnings("serial")
public final class MaxDepthException extends Exception
{

	private final int maxDepth;

	public MaxDepthException(int maxDepth)
	{
		this.maxDepth = maxDepth;
	}

	public int getMaxDepth()
	{
		return maxDepth;
	}

	@Override
	public String toString()
	{
		return "Max depth: " + maxDepth + " exceeded!";
	}

}
