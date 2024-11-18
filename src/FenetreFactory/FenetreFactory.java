package FenetreFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.util.HashMap;
import java.util.Map;

public class FenetreFactory {
    Map<String,JButton> map=new HashMap();

    public void createButton(String text, Dimension size, ActionListener actionListener) {
        JButton button= new JButton(text);
        button.setPreferredSize(size);
        button.addActionListener(actionListener);
        map.put(text,button);
    }
    public void createButton(String text,ActionListener actionListener) {
        JButton button= new JButton(text);
        button.addActionListener(actionListener);
        map.put(text,button);
    }
    public JTextArea createButton() {
        return new JTextArea();
    }
    public JButton getButton(String text) {
        return map.get(text);
    }
    public Map getMap() {
        return map;
    }
}
