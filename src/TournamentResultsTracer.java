import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class TournamentResultsTracer {
    private final JPanel cardPanel;
    private final Statement statement;
    private Tournoi currentTournoi;
    private boolean resultsTraced;
    private JTable resultsTable;
    private JScrollPane resultsScrollPane;
    private JPanel resultsPanel;
    private JLabel resultsStatus;
    private BoxLayout resultsLayout;

    public TournamentResultsTracer(JPanel cardPanel, Statement statement) {
        this.cardPanel = cardPanel;
        this.statement = statement;
        this.resultsTraced = false;
    }

    public void setCurrentTournoi(Tournoi tournoi) {
        this.currentTournoi = tournoi;
    }

    public void traceTournamentResults() {
        if (currentTournoi == null) {
            return;
        }

        Vector<Vector<Object>> rowData = fetchResultsData();
        Vector<String> columnNames = getColumnHeaders();

        if (resultsTraced) {
            updateResultsTable(rowData, columnNames);
        } else {
            initializeResultsPanel(rowData, columnNames);
            resultsTraced = true;
        }

        switchToResultsView();
    }

    private Vector<Vector<Object>> fetchResultsData() {
        Vector<Vector<Object>> rowData = new Vector<>();
        try {
            ResultSet rs = statement.executeQuery("SELECT equipe,(SELECT nom_j1 FROM equipes e WHERE e.id_equipe = equipe AND e.id_tournoi = " + this.currentTournoi.getId() + ") as joueur1,(SELECT nom_j2 FROM equipes e WHERE e.id_equipe = equipe AND e.id_tournoi = " + this.currentTournoi.getId() + ") as joueur2, SUM(score) as score, (SELECT count(*) FROM matchs m WHERE (m.equipe1 = equipe AND m.score1 > m.score2  AND m.id_tournoi = id_tournoi) OR (m.equipe2 = equipe AND m.score2 > m.score1 )) as matchs_gagnes, (SELECT COUNT(*) FROM matchs m WHERE m.equipe1 = equipe OR m.equipe2=equipe) as matchs_joues FROM  (select equipe1 as equipe,score1 as score from matchs where id_tournoi=" + this.currentTournoi.getId() + " UNION select equipe2 as equipe,score2 as score from matchs where id_tournoi=" + this.currentTournoi.getId() + ") GROUP BY equipe ORDER BY matchs_gagnes DESC;");


            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("equipe"));
                row.add(rs.getString("joueur1"));
                row.add(rs.getString("joueur2"));
                row.add(rs.getInt("score"));
                row.add(rs.getInt("matchs_gagnes"));
                row.add(rs.getInt("matchs_joues"));
                rowData.add(row);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Erreur lors de la récupération des résultats : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
        return rowData;
    }

    private Vector<String> getColumnHeaders() {
        Vector<String> columnNames = new Vector<>();
        columnNames.add("Numéro d'équipe");
        columnNames.add("Nom joueur 1");
        columnNames.add("Nom joueur 2");
        columnNames.add("Score");
        columnNames.add("Matchs gagnés");
        columnNames.add("Matchs joués");
        return columnNames;
    }

    private void updateResultsTable(Vector<Vector<Object>> rowData, Vector<String> columnNames) {
        resultsTable.setModel(new DefaultTableModel(rowData, columnNames));
        resultsScrollPane.setViewportView(resultsTable);
    }

    private void initializeResultsPanel(Vector<Vector<Object>> rowData, Vector<String> columnNames) {
        resultsPanel = new JPanel();
        resultsLayout = new BoxLayout(resultsPanel, BoxLayout.Y_AXIS);
        resultsPanel.setLayout(resultsLayout);
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel resultsDesc = new JLabel("Résultats du tournoi");
        resultsPanel.add(resultsDesc);

        resultsTable = new JTable(new DefaultTableModel(rowData, columnNames));
        resultsTable.setAutoCreateRowSorter(true);

        resultsScrollPane = new JScrollPane(resultsTable);
        resultsPanel.add(resultsScrollPane);

        JPanel footerPanel = new JPanel();
        resultsStatus = new JLabel("Gagnant :");
        footerPanel.add(resultsStatus);
        resultsPanel.add(footerPanel);

        cardPanel.add(resultsPanel, "RESULTATS");
    }

    private void switchToResultsView() {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "RESULTATS");
    }
}
