import FenetreFactory.FenetreFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Page3 {
    private JPanel mainPanel;
    private JTable teamTable;
    private AbstractTableModel teamTableModel;
    private boolean equipesTrace = false;

    public Page3() {
        this.mainPanel = new JPanel(new CardLayout());
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void tracerTournoiEquipes(FenetreFactory fenetreFactory, Tournoi tournoi) {
        if (tournoi == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (equipesTrace) {
                tournoi.majEquipes();
                teamTableModel.fireTableDataChanged();
            } else {
                equipesTrace = true;
                createTeamPanel(fenetreFactory, tournoi);
            }
            updateButtonStates(fenetreFactory, tournoi);
            ((CardLayout) mainPanel.getLayout()).show(mainPanel, "EQUIPES");
        });
    }

    private void createTeamPanel(FenetreFactory fenetreFactory, Tournoi tournoi) {
        JPanel teamPanel = new JPanel();
        teamPanel.setLayout(new BoxLayout(teamPanel, BoxLayout.Y_AXIS));
        teamPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Équipes du tournoi");
        teamPanel.add(titleLabel);

        teamTableModel = createTableModel(tournoi);
        teamTable = new JTable(teamTableModel);
        JScrollPane scrollPane = new JScrollPane(teamTable);
        teamPanel.add(scrollPane);

        JPanel buttonPanel = new JPanel();
        createButtons(fenetreFactory, tournoi, buttonPanel);

        teamPanel.add(buttonPanel);
        mainPanel.add(teamPanel, "EQUIPES");
    }

    private AbstractTableModel createTableModel(Tournoi tournoi) {
        return new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getRowCount() {
                return tournoi.getNbEquipes();
            }

            @Override
            public int getColumnCount() {
                return 3; // Number, Player 1, Player 2
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Equipe equipe = tournoi.getEquipe(rowIndex);
                if (equipe == null) return null;

                return switch (columnIndex) {
                    case 0 -> equipe.num;
                    case 1 -> equipe.eq1;
                    case 2 -> equipe.eq2;
                    default -> null;
                };
            }

            @Override
            public String getColumnName(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> "Numéro d'équipe";
                    case 1 -> "Joueur 1";
                    case 2 -> "Joueur 2";
                    default -> "??";
                };
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return tournoi.getStatut() == 0 && columnIndex > 0;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                Equipe equipe = tournoi.getEquipe(rowIndex);
                if (equipe == null) return;

                if (columnIndex == 1) equipe.eq1 = (String) aValue;
                if (columnIndex == 2) equipe.eq2 = (String) aValue;

                tournoi.majEquipe(rowIndex);
                fireTableDataChanged();
            }
        };
    }

    private void createButtons(FenetreFactory fenetreFactory, Tournoi tournoi, JPanel buttonPanel) {
        fenetreFactory.createButton("Ajouter une équipe", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tournoi.ajouterEquipe();
                teamTableModel.fireTableDataChanged();
                updateButtonStates(fenetreFactory, tournoi);
            }
        });

        fenetreFactory.createButton("Supprimer une équipe", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = teamTable.getSelectedRow();
                if (selectedRow != -1) {
                    tournoi.supprimerEquipe(tournoi.getEquipe(selectedRow).id);
                    teamTableModel.fireTableDataChanged();
                    updateButtonStates(fenetreFactory, tournoi);
                }
            }
        });

        fenetreFactory.createButton("Valider les équipes", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tournoi.genererMatchs();
            }
        });

        buttonPanel.add(fenetreFactory.getButton("Ajouter une équipe"));
        buttonPanel.add(fenetreFactory.getButton("Supprimer une équipe"));
        buttonPanel.add(fenetreFactory.getButton("Valider les équipes"));
    }

    private void updateButtonStates(FenetreFactory fenetreFactory, Tournoi tournoi) {
        boolean canEdit = tournoi.getStatut() == 0;
        boolean canValidate = tournoi.getNbEquipes() > 0 && tournoi.getNbEquipes() % 2 == 0;

        fenetreFactory.getButton("Ajouter une équipe").setEnabled(canEdit);
        fenetreFactory.getButton("Supprimer une équipe").setEnabled(canEdit);
        fenetreFactory.getButton("Valider les équipes").setEnabled(canValidate);
    }
}
