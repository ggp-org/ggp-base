package player.request.factory.exceptions;

@SuppressWarnings("serial")
public final class RequestFormatException extends Exception
{

	private final String source;

	public RequestFormatException(String source)
	{
		this.source = source;
	}

	public String getSource()
	{
		return source;
	}

	@Override
	public String toString()
	{
		return "Improperly formatted request: " + source;
	}

}
