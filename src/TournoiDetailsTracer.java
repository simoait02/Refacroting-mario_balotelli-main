import javax.swing.*;
import java.awt.*;

public class TournoiDetailsTracer {
    private Tournoi currentTournoi;
    private final JPanel cardPanel;
    private boolean detailsTrace = false;
    private JLabel detailtNom;
    private JLabel detailtStatut;
    private JLabel detailtNbTours;
    Runnable action;

    public TournoiDetailsTracer(Tournoi currentTournoi, JPanel cardPanel, Runnable action) {
        this.action = action;
        this.currentTournoi = currentTournoi;
        this.cardPanel = cardPanel;
    }


    public void tracerDetailsTournoi() {
        if (currentTournoi == null) {
            showError("Aucun tournoi sélectionné !");
            return;
        }

        action.run();

        if (detailsTrace) {
            updateDetailsPanel();
        } else {
            initializeDetailsPanel();
        }
        switchToDetailsView();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(cardPanel, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void updateDetailsPanel() {
        detailtNom.setText(currentTournoi.getNomTournoi());
        detailtStatut.setText(currentTournoi.getStatutNom());
        detailtNbTours.setText(Integer.toString(currentTournoi.getNbTours()));
    }

    private void initializeDetailsPanel() {
        detailsTrace = true;

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel headerLabel = new JLabel("Détails du tournoi");
        detailsPanel.add(headerLabel);

        JPanel infoPanel = new JPanel(new GridLayout(3, 2));

        detailtNom = new JLabel(currentTournoi.getNomTournoi());
        infoPanel.add(new JLabel("Nom du tournoi"));
        infoPanel.add(detailtNom);

        detailtStatut = new JLabel(currentTournoi.getStatutNom());
        infoPanel.add(new JLabel("Statut"));
        infoPanel.add(detailtStatut);

        detailtNbTours = new JLabel(Integer.toString(currentTournoi.getNbTours()));
        infoPanel.add(new JLabel("Nombre de tours"));
        infoPanel.add(detailtNbTours);

        detailsPanel.add(infoPanel);

        cardPanel.add(detailsPanel, "DETAIL");
    }

    private void switchToDetailsView() {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "DETAIL");
    }



    public void setCurrentTournoi(Tournoi currentTournoi) {
        this.currentTournoi = currentTournoi;
    }
}
