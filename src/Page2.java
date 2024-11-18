import javax.swing.*;
import java.awt.*;

public class Page2 {
    private JPanel contentPanel;
    private JLabel detailt_nom;
    private JLabel detailt_statut;
    private JLabel detailt_nbtours;
    private boolean detailsTrace = false;

    public Page2(JPanel contentPanel) {
        this.contentPanel = contentPanel;
    }

    public void tracerDetailsTournoi(Tournoi t, CardLayout layout) {
        if (t == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (detailsTrace) {
                updateDetails(t);
            } else {
                initializeDetailsPanel(t);
                detailsTrace = true;
            }
            layout.show(contentPanel, "DETAIL");
        });
    }

    private void updateDetails(Tournoi t) {
        detailt_nom.setText(t.getNom());
        detailt_statut.setText(t.getNStatut());
        detailt_nbtours.setText(Integer.toString(t.getNbTours()));
    }

    private void initializeDetailsPanel(Tournoi t) {
        JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.add(new JLabel("Détail du tournoi"));

        JPanel gridPanel = new JPanel(new GridLayout(4, 2));
        detailt_nom = new JLabel(t.getNom());
        gridPanel.add(new JLabel("Nom du tournoi:"));
        gridPanel.add(detailt_nom);

        detailt_statut = new JLabel(t.getNStatut());
        gridPanel.add(new JLabel("Statut:"));
        gridPanel.add(detailt_statut);

        detailt_nbtours = new JLabel(Integer.toString(t.getNbTours()));
        gridPanel.add(new JLabel("Nombre de tours:"));
        gridPanel.add(detailt_nbtours);

        detailPanel.add(gridPanel);
        detailPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        contentPanel.add(detailPanel, "DETAIL");
    }
}
