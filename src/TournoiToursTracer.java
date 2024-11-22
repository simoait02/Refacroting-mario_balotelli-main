import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Vector;

public class TournoiToursTracer {

    private Runnable action;
    private Tournoi currentTournoi;
    private final Statement statement;
    private final JPanel cardPanel;
    private JPanel toursPanel;
    private JTable toursTable;
    private JScrollPane toursScrollPane;
    private JButton addTourButton;
    private JButton removeTourButton;
    private boolean toursTrace;

    public TournoiToursTracer(Tournoi currentTournoi, Statement statement, JPanel cardPanel,Runnable action) {
        this.currentTournoi = currentTournoi;
        this.action = action;
        this.statement = statement;
        this.cardPanel = cardPanel;
        this.toursTrace = false;
    }
    public void setCurrentTournoie(Tournoi currentTournoi) {
        this.currentTournoi=currentTournoi;
    }
    public void traceTours() {
        if (this.currentTournoi == null) {
            showError("Aucun tournoi sélectionné !", "Erreur");
            return;
        }
        action.run();
        Vector<Vector<Object>> data = new Vector<>();
        Vector<String> columnNames = initializeColumnNames();

        boolean canAddTour = populateData(data);

        if (!toursTrace) {
            initializeToursPanel(data, columnNames);
        } else {
            updateToursTable(data, columnNames);
        }

        updateButtonStates(canAddTour);
        switchToToursView();
    }

    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private Vector<String> initializeColumnNames() {
        Vector<String> columnNames = new Vector<>();
        columnNames.add("Numéro du tour");
        columnNames.add("Nombre de matchs");
        columnNames.add("Matchs joués");
        return columnNames;
    }

    private boolean populateData(Vector<Vector<Object>> data) {
        boolean canAddTour = true;
        String query = """
            SELECT num_tour, COUNT(*) AS total_matchs,
                   (SELECT COUNT(*) 
                    FROM matchs m2 
                    WHERE m2.id_tournoi = m.id_tournoi 
                      AND m2.num_tour = m.num_tour 
                      AND m2.termine = 'oui') AS termines
            FROM matchs m 
            WHERE m.id_tournoi = %d 
            GROUP BY m.num_tour, m.id_tournoi
        """.formatted(currentTournoi.getId());

        try (ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                Vector<Object> row = createDataRow(rs);
                data.add(row);

                int totalMatchs = rs.getInt("total_matchs");
                int matchesPlayed = rs.getInt("termines");
                canAddTour &= (totalMatchs == matchesPlayed);
            }
        } catch (SQLException e) {
            showError("Erreur lors de la récupération des données : " + e.getMessage(), "Erreur");
            canAddTour = false;
        }
        return canAddTour;
    }

    private Vector<Object> createDataRow(ResultSet rs) throws SQLException {
        Vector<Object> row = new Vector<>();
        row.add(rs.getInt("num_tour"));
        row.add(rs.getInt("total_matchs"));
        row.add(rs.getInt("termines"));
        return row;
    }

    private void initializeToursPanel(Vector<Vector<Object>> data, Vector<String> columnNames) {
        toursTrace = true;

        toursPanel = new JPanel();
        toursPanel.setLayout(new BoxLayout(toursPanel, BoxLayout.Y_AXIS));
        toursPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        toursPanel.add(new JLabel("Tours"));

        toursTable = new JTable(new DefaultTableModel(data, columnNames));
        toursScrollPane = new JScrollPane(toursTable);
        toursPanel.add(toursScrollPane);

        JPanel buttonPanel = new JPanel();
        addTourButton = createActionButton("Ajouter un tour", e -> addTour(), buttonPanel);
        removeTourButton = createActionButton("Supprimer le dernier tour", e -> removeTour(), buttonPanel);

        toursPanel.add(buttonPanel);

        toursPanel.add(new JLabel("Pour ajouter un tour, terminez tous les matchs du précédent."));
        toursPanel.add(new JLabel("Le nombre maximum de tours est \"le nombre d'équipes - 1\"."));

        cardPanel.add(toursPanel, "TOURS");
    }

    private void updateToursTable(Vector<Vector<Object>> data, Vector<String> columnNames) {
        toursTable.setModel(new DefaultTableModel(data, columnNames));
    }

    private JButton createActionButton(String text, ActionListener action, JPanel parentPanel) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        parentPanel.add(button);
        return button;
    }

    private void addTour() {
        currentTournoi.ajouterTour();
        traceTours();
    }

    private void removeTour() {
        currentTournoi.supprimerTour();
        traceTours();
    }

    private void updateButtonStates(boolean canAddTour) {
        addTourButton.setEnabled(canAddTour && currentTournoi.getNbTours() < currentTournoi.getNbEquipes() - 1);
        removeTourButton.setEnabled(currentTournoi.getNbTours() > 1);
    }

    private void switchToToursView() {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "TOURS");
    }
}
