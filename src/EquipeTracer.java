import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.Serial;

public class EquipeTracer {

    Runnable action;
    private  Tournoi currentTournoi;
    private final JPanel cardPanel;
    private JTable eqTable;
    private AbstractTableModel eqModel;
    private JButton addTeamButton;
    private JButton removeTeamButton;
    private JButton validateTeamsButton;
    TournoiToursTracer tournoiToursTracer;
    private boolean equipesTrace;

    public EquipeTracer(Tournoi currentTournoi, JPanel cardPanel,TournoiToursTracer tournoiToursTracer,Runnable action) {
        this.tournoiToursTracer = tournoiToursTracer;
        this.currentTournoi = currentTournoi;
        this.action = action;
        this.cardPanel = cardPanel;
        this.equipesTrace = false;
    }

    public void traceTeams() {
        if (currentTournoi == null) {
            showError("Aucun tournoi sélectionné !");
            return;
        }
        action.run();
        if (!equipesTrace) {
            initializeTeamsPanel();
        } else {
            currentTournoi.updateData();
            eqModel.fireTableDataChanged();
        }
        updateTeamButtonStates();
        action.run();
        switchToTeamsView();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void initializeTeamsPanel() {
        equipesTrace = true;

        JPanel eqPanel = new JPanel();
        eqPanel.setLayout(new BoxLayout(eqPanel, BoxLayout.Y_AXIS));
        eqPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        eqPanel.add(new JLabel("Équipes du tournoi"));

        eqModel = createTeamsTableModel();
        eqTable = new JTable(eqModel);
        eqPanel.add(new JScrollPane(eqTable));

        JPanel buttonPanel = new JPanel();
        addTeamButton = createActionButton("Ajouter une équipe", e -> {
            currentTournoi.ajouterEquipe();
            eqModel.fireTableDataChanged();
            updateTeamButtonStates();
        }, buttonPanel);

        removeTeamButton = createActionButton("Supprimer une équipe", e -> {
            if(eqTable.getSelectedRow() != -1){
                currentTournoi.supprimerEquipe(currentTournoi.getEquipe(eqTable.getSelectedRow()).id);
            }
            validateTeamsButton.setEnabled(currentTournoi.getNbEquipes() > 0 && currentTournoi.getNbEquipes() % 2 == 0) ;
            eqModel.fireTableDataChanged();
            if(currentTournoi.getNbEquipes() > 0){
                eqTable.getSelectionModel().setSelectionInterval(0, 0);
            }
            updateTeamButtonStates();
        }, buttonPanel);

        validateTeamsButton = createActionButton("Valider les équipes", e -> {
            if (currentTournoi.getNbEquipes() % 2 != 0) {
                showError("Le nombre d'équipes doit être pair pour continuer.");
            } else {
                currentTournoi.genererMatchs();
                action.run();
                this.tournoiToursTracer.traceTours();

            }
        }, buttonPanel);


        eqPanel.add(buttonPanel);
        cardPanel.add(eqPanel, "EQUIPES");
        if(currentTournoi.getStatut() != 0){
            addTeamButton.setEnabled(false);
            removeTeamButton.setEnabled(false);
            System.out.println(currentTournoi.getStatut());
            validateTeamsButton.setEnabled(currentTournoi.getStatut() == 1);
        }else{
            addTeamButton.setEnabled(true);
            removeTeamButton.setEnabled(true);
            validateTeamsButton.setEnabled(currentTournoi.getNbEquipes() > 0) ;
        }
    }

    private void updateTeamButtonStates() {
        boolean hasTeams = currentTournoi.getNbEquipes() > 0;
        boolean isEven = currentTournoi.getNbEquipes() % 2 == 0;

        validateTeamsButton.setEnabled(hasTeams && isEven && (currentTournoi.getStatut() == 0 || currentTournoi.getStatut() == 1));

        addTeamButton.setEnabled(currentTournoi.getStatut() == 0);

        removeTeamButton.setEnabled(hasTeams && currentTournoi.getStatut() == 0);
    }



    private AbstractTableModel createTeamsTableModel() {
        return new AbstractTableModel() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Equipe equipe = currentTournoi.getEquipe(rowIndex);
                return switch (columnIndex) {
                    case 0 -> equipe.getNum();
                    case 1 -> equipe.getEq1();
                    case 2 -> equipe.getEq2();
                    default -> "??";
                };
            }

            @Override
            public String getColumnName(int column) {
                return switch (column) {
                    case 0 -> "Numéro d'équipe";
                    case 1 -> "Joueur 1";
                    case 2 -> "Joueur 2";
                    default -> "??";
                };
            }

            @Override
            public int getRowCount() {
                return currentTournoi != null ? currentTournoi.getNbEquipes() : 0;
            }

            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex > 0 && currentTournoi.getStatut() == 0;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                Equipe equipe = currentTournoi.getEquipe(rowIndex);
                if (columnIndex == 1) {
                    equipe.setEq1((String) aValue);
                } else if (columnIndex == 2) {
                    equipe.setEq2((String) aValue);
                }
                currentTournoi.updateEquipe(rowIndex);
                fireTableDataChanged();
            }
        };
    }

    protected JButton createActionButton(String text, ActionListener listener, JPanel parent) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(150, 30));
        button.addActionListener(listener);
        parent.add(button);
        return button;
    }


    public void setCurrentTournoi(Tournoi currentTournoi) {
        this.currentTournoi = currentTournoi;
    }
    private void switchToTeamsView() {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "EQUIPES");
    }
}
