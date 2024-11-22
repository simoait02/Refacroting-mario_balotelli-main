import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MatchTracer {
    private Tournoi currentTournoi;
    private final JPanel cardPanel;
    private JTable matchTable;
    private JScrollPane matchScrollPane;
    private AbstractTableModel matchTableModel;
    private JLabel matchStatusLabel;
    private JButton validateMatchButton;
    private JPanel matchPanel;
    private boolean matchTraced;
    Runnable action ;

    Statement statement;
    TournamentResultsTracer tournamentResultsTracer;

    public MatchTracer(Tournoi currentTournoi, JPanel cardPanel,Runnable action,TournamentResultsTracer tournamentResultsTracer,Statement statement) {
        this.statement=statement;
        this.currentTournoi = currentTournoi;
        this.cardPanel = cardPanel;
        this.tournamentResultsTracer = tournamentResultsTracer;
        this.action = action;
        this.matchTraced = false;

    }

    public void tracerMatchsTournoi() {
        if (currentTournoi == null) {
            return;
        }

        action.run();

        if (matchTraced) {
            refreshMatchData();
        } else {
            initializeMatchView();
            matchTraced = true;
        }

        switchToMatchView();
    }

    private void refreshMatchData() {
        currentTournoi.updateData();
        matchTableModel.fireTableDataChanged();
        majStatutM();
    }

    private void initializeMatchView() {
        matchPanel = createMatchPanel();
        cardPanel.add(matchPanel, "MATCHS");
        majStatutM();
    }

    private JPanel createMatchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(new JLabel("Matchs du tournoi"));
        panel.add(createMatchTable());
        panel.add(createMatchFooter());

        return panel;
    }

    private JScrollPane createMatchTable() {
        matchTableModel = createMatchTableModel();
        matchTable = new JTable(matchTableModel);
        matchScrollPane = new JScrollPane(matchTable);
        return matchScrollPane;
    }

    private AbstractTableModel createMatchTableModel() {
        return new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getRowCount() {
                return (currentTournoi == null) ? 0 : currentTournoi.getNbMatchs();
            }

            @Override
            public int getColumnCount() {
                return 5;
            }

            @Override
            public String getColumnName(int col) {
                return switch (col) {
                    case 0 -> "Tour";
                    case 1 -> "Équipe 1";
                    case 2 -> "Équipe 2";
                    case 3 -> "Score équipe 1";
                    case 4 -> "Score équipe 2";
                    default -> "??";
                };
            }

            @Override
            public Object getValueAt(int row, int col) {
                if (currentTournoi == null) return null;
                MatchM match = currentTournoi.getMatch(row);

                return switch (col) {
                    case 0 -> match.num_tour;
                    case 1 -> match.eq1;
                    case 2 -> match.eq2;
                    case 3 -> match.score1;
                    case 4 -> match.score2;
                    default -> null;
                };
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col > 2 && currentTournoi.getMatch(row).num_tour == currentTournoi.getNbTours();
            }

            @Override
            public void setValueAt(Object aValue, int row, int col) {
                try {
                    MatchM match = currentTournoi.getMatch(row);

                    if (col == 3) {
                        match.score1 = Integer.parseInt(aValue.toString());
                    } else if (col == 4) {
                        match.score2 = Integer.parseInt(aValue.toString());
                    }

                    currentTournoi.updateMatch(row);
                    fireTableDataChanged();
                    action.run();
                } catch (NumberFormatException e) {
                    // Handle invalid input gracefully
                }
            }
        };
    }

    private JPanel createMatchFooter() {
        JPanel footer = new JPanel();
        matchStatusLabel = new JLabel("?? Matchs joués");
        validateMatchButton = new JButton("Afficher les résultats");
        validateMatchButton.setEnabled(false);

        footer.add(matchStatusLabel);
        footer.add(validateMatchButton);

        validateMatchButton.addActionListener(e -> tournamentResultsTracer.traceTournamentResults());
        return footer;
    }



    private void switchToMatchView() {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "MATCHS");
    }



    public void setCurrentTournoi(Tournoi currentTournoi) {
        this.currentTournoi = currentTournoi;
        this.matchTraced = false;
    }

    private void majStatutM() {
        int total = -1;
        int termines = -1;
        String query = """
        SELECT 
            COUNT(*) AS total, 
            (SELECT COUNT(*) 
             FROM matchs m2 
             WHERE m2.id_tournoi = m.id_tournoi 
               AND m2.termine = 'oui') AS termines 
        FROM matchs m 
        WHERE m.id_tournoi = ? 
        GROUP BY id_tournoi;
    """;

        try (PreparedStatement ps = statement.getConnection().prepareStatement(query)) {
            ps.setInt(1, this.currentTournoi.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total");
                    termines = rs.getInt("termines");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        updateMatchStatus(total, termines);
    }
    private void updateMatchStatus(int total, int termines) {
        if (total >= 0 && termines >= 0) {
            matchStatusLabel.setText(termines + "/" + total + " matchs terminés");
            validateMatchButton.setEnabled(total == termines);
        } else {
            matchStatusLabel.setText("Erreur lors de la récupération des données");
            validateMatchButton.setEnabled(false);
        }
    }

}
