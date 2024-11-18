import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.Statement;

public class Page5 {
    private Statement s; // Database statement
    private JPanel matchPanel; // Main panel for matches
    private AbstractTableModel matchTableModel; // Table model for matches
    private JTable matchTable; // JTable for displaying matches
    private JLabel matchStatus; // Status label
    private JButton validateButton; // Button to display results
    private boolean matchTrace = false; // Flag to check if matches are already traced
    private CardLayout cardLayout; // Card layout manager for switching views
    private JPanel mainContainer; // Main container panel

    public Page5(Statement s, JPanel mainContainer, CardLayout cardLayout) {
        this.s = s;
        this.mainContainer = mainContainer;
        this.cardLayout = cardLayout;
    }

    public void tracerTournoiMatchs(Tournoi t) {
        if (t == null) {
            return;
        }

        if (matchTrace) {
            t.majMatch(); // Update matches from the backend
            matchTableModel.fireTableDataChanged(); // Notify table of data changes
            updateMatchStatus(t);
        } else {
            matchTrace = true;

            // Create the main match panel
            matchPanel = new JPanel();
            matchPanel.setLayout(new BoxLayout(matchPanel, BoxLayout.Y_AXIS));
            matchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // Description label
            JLabel matchDescription = new JLabel("Matchs du tournoi");
            matchPanel.add(matchDescription);

            // Initialize table model
            matchTableModel = new AbstractTableModel() {
                @Override
                public int getRowCount() {
                    return t.getNbMatchs();
                }

                @Override
                public int getColumnCount() {
                    return 5; // Fixed number of columns
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    MatchM match = t.getMatch(rowIndex);
                    return switch (columnIndex) {
                        case 0 -> match.num_tour;
                        case 1 -> match.eq1;
                        case 2 -> match.eq2;
                        case 3 -> match.score1;
                        case 4 -> match.score2;
                        default -> null;
                    };
                }

                @Override
                public String getColumnName(int column) {
                    return switch (column) {
                        case 0 -> "Tour";
                        case 1 -> "Équipe 1";
                        case 2 -> "Équipe 2";
                        case 3 -> "Score équipe 1";
                        case 4 -> "Score équipe 2";
                        default -> "??";
                    };
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnIndex > 2 && t.getMatch(rowIndex).num_tour == t.getNbTours();
                }

                @Override
                public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                    MatchM match = t.getMatch(rowIndex);
                    try {
                        if (columnIndex == 3) {
                            match.score1 = Integer.parseInt(aValue.toString());
                        } else if (columnIndex == 4) {
                            match.score2 = Integer.parseInt(aValue.toString());
                        }
                        t.majMatch(rowIndex); // Update match in backend
                        fireTableDataChanged(); // Notify table of changes
                        updateMatchStatus(t); // Update match status
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Invalid score entered.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            // Create the JTable
            matchTable = new JTable(matchTableModel);

            // Add table to scroll pane
            JScrollPane scrollPane = new JScrollPane(matchTable);
            matchPanel.add(scrollPane);

            // Status and button panel
            JPanel bottomPanel = new JPanel();
            matchStatus = new JLabel("?? Matchs joués");
            validateButton = new JButton("Afficher les résultats");
            validateButton.setEnabled(false);
            validateButton.addActionListener(e -> {
                // Action for validating matches
                JOptionPane.showMessageDialog(null, "Displaying results!", "Info", JOptionPane.INFORMATION_MESSAGE);
            });
            bottomPanel.add(matchStatus);
            bottomPanel.add(validateButton);
            matchPanel.add(bottomPanel);

            // Add panel to container
            mainContainer.add(matchPanel, "MATCHS");
            updateMatchStatus(t);
        }

        // Show the "MATCHS" panel
        cardLayout.show(mainContainer, "MATCHS");
    }

    private void updateMatchStatus(Tournoi t) {
        int playedMatches = t.getNbMatchs();
        matchStatus.setText(playedMatches + " Matchs joués");
        validateButton.setEnabled(playedMatches == t.getNbMatchs());
    }
}
