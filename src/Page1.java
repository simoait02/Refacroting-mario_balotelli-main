import FenetreFactory.FenetreFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class Page1 extends JFrame {
    private Statement statement;
    private JList<String> list;
    private JPanel contentPanel;
    private boolean tournoisTrace = false;

    public Page1(Statement statement) {
        this.statement = statement;
        this.contentPanel = new JPanel(new CardLayout());
        add(contentPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
    }

    public void tracerSelectTournoi(FenetreFactory fenetreFactory) {
        Vector<String> nomsTournois = new Vector<>();
        int nbDeLignes = fetchTournois(nomsTournois);

        SwingUtilities.invokeLater(() -> {
            if (tournoisTrace) {
                updateTournoisList(nomsTournois, fenetreFactory, nbDeLignes);
            } else {
                initializeTournoisPanel(nomsTournois, fenetreFactory, nbDeLignes);
                tournoisTrace = true;
            }
        });
    }

    private int fetchTournois(Vector<String> nomsTournois) {
        int nbDeLignes = 0;
        try (ResultSet rs = statement.executeQuery("SELECT * FROM tournois;")) {
            while (rs.next()) {
                nomsTournois.add(rs.getString("nom_tournoi"));
                nbDeLignes++;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la requête: " + e.getMessage());
        }
        return nbDeLignes;
    }

    private void updateTournoisList(Vector<String> nomsTournois, FenetreFactory fenetreFactory, int nbDeLignes) {
        list.setListData(nomsTournois);
        if (nbDeLignes == 0) {
            fenetreFactory.getButton("selectTournoi").setEnabled(false);
            fenetreFactory.getButton("deleteTournoi").setEnabled(false);
        } else {
            list.setSelectedIndex(0);
        }
    }

    private void initializeTournoisPanel(Vector<String> nomsTournois, FenetreFactory fenetreFactory, int nbDeLignes) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JTextArea header = new JTextArea("Gestion des tournois\nXXXXX XXXXXXXX, juillet 2012");
        header.setEditable(false);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(header);

        list = new JList<>(nomsTournois);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 180));
        panel.add(listScroller);

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(fenetreFactory.getButton("createTournoi"));
        buttonBox.add(fenetreFactory.getButton("selectTournoi"));
        buttonBox.add(fenetreFactory.getButton("deleteTournoi"));
        panel.add(buttonBox);

        contentPanel.add(panel, "TOURNOIS");
        if (nbDeLignes == 0) {
            fenetreFactory.getButton("selectTournoi").setEnabled(false);
            fenetreFactory.getButton("deleteTournoi").setEnabled(false);
        }
        ((CardLayout) contentPanel.getLayout()).show(contentPanel, "TOURNOIS");
    }
}
