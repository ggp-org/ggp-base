package org.ggp.base.player.request.factory.exceptions;

@SuppressWarnings("serial")
public final class RequestFormatException extends Exception
{
	private final String source;
	private final Exception bad;

	public RequestFormatException(String source, Exception bad)
	{
		this.source = source;
		this.bad = bad;
	}

	public String getSource()
	{
		return source;
	}

	@Override
	public String toString()
	{
		return "Improperly formatted request: " + source + ", resulting in exception: " + bad;
	}

}
