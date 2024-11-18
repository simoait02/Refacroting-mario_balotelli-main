import FenetreFactory.FenetreFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Page4 {
    private Statement s;
    public Page4(Statement s) {
        this.s = s;
    }

    public void tracer_tours_tournoi(Tournoi t, FenetreFactory fenetreFactory, CardLayout layout) {
        if (t == null) {
            return;
        }

        // Table Model for Tours
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{
                "Numéro du tour", "Nombre de matchs", "Matchs joués"
        }, 0);

        boolean peutAjouter = true; // To control button enable/disable state

        try {
            // Fetch data about tours
            ResultSet rs = s.executeQuery(
                    "SELECT num_tour, COUNT(*) as tmatchs, " +
                            "(SELECT COUNT(*) FROM matchs m2 WHERE m2.id_tournoi = m.id_tournoi AND m2.num_tour = m.num_tour AND m2.termine = 'oui') as termines " +
                            "FROM matchs m WHERE m.id_tournoi = " + t.id_tournoi+ " GROUP BY m.num_tour, m.id_tournoi;"
            );

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("num_tour"),
                        rs.getInt("tmatchs"),
                        rs.getString("termines")
                });
                peutAjouter &= (rs.getInt("tmatchs") == rs.getInt("termines"));
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Log SQL error
        }

        JTable toursTable = new JTable(tableModel);

        JPanel toursPanel = new JPanel();
        toursPanel.setLayout(new BoxLayout(toursPanel, BoxLayout.Y_AXIS));
        toursPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel toursDesc = new JLabel("Tours");
        toursPanel.add(toursDesc);

        JScrollPane toursScrollPane = new JScrollPane(toursTable);
        toursPanel.add(toursScrollPane);

        // Button Panel
        JPanel buttonPanel = new JPanel();

        // Add Tour Button
        JButton addTourButton = new JButton("Ajouter un tour");
        addTourButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                t.ajouterTour();
                tracer_tours_tournoi(t, fenetreFactory,layout); // Refresh view
            }
        });

        // Remove Last Tour Button
        JButton removeTourButton = new JButton("Supprimer le dernier tour");
        removeTourButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                t.supprimerTour();
                tracer_tours_tournoi(t, fenetreFactory,layout); // Refresh view
            }
        });

        buttonPanel.add(addTourButton);
        buttonPanel.add(removeTourButton);
        toursPanel.add(buttonPanel);

        // Add info labels
        toursPanel.add(new JLabel("Pour pouvoir ajouter un tour, terminez tous les matchs du précédent."));
        toursPanel.add(new JLabel("Le nombre maximum de tours est \"le nombre total d'équipes - 1\"."));

        // Enable/Disable buttons based on the state
        if (tableModel.getRowCount() == 0) {
            removeTourButton.setEnabled(false);
            addTourButton.setEnabled(true);
        } else {
            removeTourButton.setEnabled(t.getNbTours() > 1);
            addTourButton.setEnabled(peutAjouter && t.getNbTours() < t.getNbEquipes() - 1);
        }

        // Display the panel
        layout.show(toursPanel, "TOURS");
    }
}
