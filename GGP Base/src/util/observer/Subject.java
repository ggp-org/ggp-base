package util.observer;

public interface Subject
{

	public void addObserver(Observer observer);

	public void notifyObservers(Event event);

}
