package player.request.grammar;

public abstract class Request
{

	public abstract String process(long receptionTime);

	@Override
	public abstract String toString();

}
