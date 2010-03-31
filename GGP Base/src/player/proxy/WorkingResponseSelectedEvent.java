package player.proxy;

import util.observer.Event;

public class WorkingResponseSelectedEvent extends Event {
    private String theResponse;
    
    public WorkingResponseSelectedEvent(String theResponse) {
        this.theResponse = theResponse;
    }
    
    public String getWorkingResponse() {
        return theResponse;
    }
}