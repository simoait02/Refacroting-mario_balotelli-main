package observer;

import javax.swing.*;

public interface Subject {
    void addObserver(JButton observer, int activeValue);
    void removeObserver(JButton observer);
    void notifyObservers(int message); // Calls the update() method for each observer
}
