package observer;


import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class TournamentButton implements Subject {

    private final Map<JButton,Integer> observers = new HashMap<JButton,Integer>();
    private int status;

    public void setStatus(int status) {
        this.status = status;
        notifyObservers(status);
    }

    @Override
    public void addObserver(JButton observer,int activeValue) {
        observers.put(observer,activeValue);
    }
    @Override
    public void removeObserver(JButton observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(int status) {
        for (JButton observer : observers.keySet()) {
            if(status==observers.get(observer)){
                observer.setVisible(true);
            }else observer.setVisible(observers.get(observer) == -1);
        }
    }
    public int getStatus() {
        return status;
    }
}
