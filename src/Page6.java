import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class Page6 {
    private Statement s; // SQL statement
    private JPanel resultatsPanel; // Panel for results
    private JScrollPane resultatsScrollPane; // Scroll pane for the results table
    private JTable resultatsTable; // Table to show results
    private JLabel resultatsStatut; // Status label
    private boolean resultatsTrace = false; // Flag to check if results are already traced
    private final String RESULTATS = "RESULTATS"; // Tab name
    private final JPanel c; // Container to which panels are added
    private final CardLayout fen; // Layout for showing different pages (CardLayout)

    // Constructor
    public Page6(Statement s, JPanel c, CardLayout fen) {
        this.s = s;
        this.c = c;
        this.fen = fen;
    }

    public void tracer_tournoi_resultats(Tournoi t) {
        if (t == null) {
            return;
        }

        // Initialize data for the table
        Vector<Vector<Object>> data = new Vector<>();
        Vector<Object> rowData;
        try {
            ResultSet rs = s.executeQuery(
                    "SELECT equipe, " +
                            "(SELECT nom_j1 FROM equipes e WHERE e.id_equipe = equipe AND e.id_tournoi = " + t.id_tournoi + ") as joueur1, " +
                            "(SELECT nom_j2 FROM equipes e WHERE e.id_equipe = equipe AND e.id_tournoi = " + t.id_tournoi + ") as joueur2, " +
                            "SUM(score) as score, " +
                            "(SELECT count(*) FROM matchs m WHERE (m.equipe1 = equipe AND m.score1 > m.score2 AND m.id_tournoi = " + t.id_tournoi + ") OR (m.equipe2 = equipe AND m.score2 > m.score1 AND m.id_tournoi = " + t.id_tournoi + ")) as matchs_gagnes, " +
                            "(SELECT COUNT(*) FROM matchs m WHERE m.equipe1 = equipe OR m.equipe2 = equipe) as matchs_joues " +
                            "FROM " +
                            "(SELECT equipe1 as equipe, score1 as score FROM matchs WHERE id_tournoi = " + t.id_tournoi + " " +
                            "UNION SELECT equipe2 as equipe, score2 as score FROM matchs WHERE id_tournoi = " + t.id_tournoi + ") " +
                            "GROUP BY equipe ORDER BY matchs_gagnes DESC;"
            );

            while (rs.next()) {
                rowData = new Vector<>();
                rowData.add(rs.getInt("equipe"));
                rowData.add(rs.getString("joueur1"));
                rowData.add(rs.getString("joueur2"));
                rowData.add(rs.getInt("score"));
                rowData.add(rs.getInt("matchs_gagnes"));
                rowData.add(rs.getInt("matchs_joues"));
                data.add(rowData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching results: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        // Column names for the table
        Vector<String> columnNames = new Vector<>();
        columnNames.add("Numéro d'équipe");
        columnNames.add("Nom joueur 1");
        columnNames.add("Nom joueur 2");
        columnNames.add("Score");
        columnNames.add("Matchs gagnés");
        columnNames.add("Matchs joués");

        // Create table model and table
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);
        resultatsTable = new JTable(tableModel);
        resultatsTable.setAutoCreateRowSorter(true); // Allow sorting by column

        // If the results panel is already created, just update the table
        if (resultatsTrace) {
            resultatsScrollPane.setViewportView(resultatsTable);
        } else {
            resultatsTrace = true;
            resultatsPanel = new JPanel();
            resultatsPanel.setLayout(new BoxLayout(resultatsPanel, BoxLayout.Y_AXIS));

            // Title label
            JLabel resultatsDescription = new JLabel("Résultats du tournoi");
            resultatsPanel.add(resultatsDescription);

            // Create scroll pane for table and add it
            resultatsScrollPane = new JScrollPane(resultatsTable);
            resultatsPanel.add(resultatsScrollPane);

            // Panel for the status and additional buttons
            JPanel resultatsBottomPanel = new JPanel();
            resultatsStatut = new JLabel("Gagnant:"); // Placeholder for winner
            resultatsBottomPanel.add(resultatsStatut);
            resultatsPanel.add(resultatsBottomPanel);

            // Add the results panel to the main container
            c.add(resultatsPanel, RESULTATS);
        }

        // Display the results panel using CardLayout
        fen.show(c, RESULTATS);
    }
}
