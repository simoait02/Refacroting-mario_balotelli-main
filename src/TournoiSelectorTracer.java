import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class TournoiSelectorTracer {
    private Tournoi currentTournoi;
    private final JPanel cardPanel;
    private final Statement statement;
    private final TournoiToursTracer tournoiTracer;
    private final EquipeTracer equipeTracer;
    private final TournoiDetailsTracer tournoiDetailsTracer;
    private JList<String> list;
    private JButton selectTournoi;
    private JButton deleteTournoi;
    Consumer<String> updateStatus;
    BiFunction<JPanel, JList<String>, JButton> select;
    Runnable updateButtonStates;

    public TournoiSelectorTracer(JPanel cardPanel, Statement statement, TournoiToursTracer tournoiTracer, EquipeTracer equipeTracer, TournoiDetailsTracer tournoiDetailsTracer, Consumer<String> updateStatus, Runnable updateButtonStates,BiFunction<JPanel, JList<String>, JButton> select) {
        this.cardPanel = cardPanel;
        this.statement = statement;
        this.select=select;
        this.updateStatus = updateStatus;
        this.updateButtonStates = updateButtonStates;
        this.tournoiTracer = tournoiTracer;
        this.equipeTracer = equipeTracer;
        this.tournoiDetailsTracer = tournoiDetailsTracer;
    }

    public void tracerSelectTournoi() {
        currentTournoi = null;
        updateButtonStates.run();
        updateStatus.accept("Sélection d'un tournoi");

        Vector<String> tournamentNames = fetchTournamentNames();

        if (isTournamentPanelTraced()) {
            updateTournamentList(tournamentNames);
            return;
        }

        JPanel tournamentPanel = createTournamentPanel(tournamentNames);
        cardPanel.add(tournamentPanel, "TOURNOIS");

        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "TOURNOIS");
    }

    private Vector<String> fetchTournamentNames() {
        Vector<String> tournamentNames = new Vector<>();
        try (ResultSet rs = statement.executeQuery("SELECT * FROM tournois")) {
            while (rs.next()) {
                tournamentNames.add(rs.getString("nom_tournoi"));
            }
        } catch (SQLException e) {
            showError("Erreur lors de la requête : " + e.getMessage());
        }
        return tournamentNames;
    }

    private boolean isTournamentPanelTraced() {
        return cardPanel.getComponentCount() > 0;
    }

    private void updateTournamentList(Vector<String> tournamentNames) {
        list.setListData(tournamentNames);
        boolean hasTournaments = !tournamentNames.isEmpty();
        selectTournoi.setEnabled(hasTournaments);
        deleteTournoi.setEnabled(hasTournaments);

        if (hasTournaments) {
            list.setSelectedIndex(0);
        }

        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "TOURNOIS");
    }

    private JPanel createTournamentPanel(Vector<String> tournamentNames) {
        JPanel tournamentPanel = new JPanel();
        tournamentPanel.setLayout(new BoxLayout(tournamentPanel, BoxLayout.Y_AXIS));

        JTextArea headerText = new JTextArea("Gestion des tournois\nXXXXX XXXXXXXX, juillet 2012");
        headerText.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerText.setEditable(false);
        tournamentPanel.add(headerText);

        populateTournamentList(tournamentNames, tournamentPanel);
        setupTournamentButtons(tournamentPanel, tournamentNames);

        return tournamentPanel;
    }

    private void populateTournamentList(Vector<String> tournamentNames, JPanel tournamentPanel) {
        list = new JList<>(tournamentNames);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 180));
        tournamentPanel.add(new JLabel("Liste des tournois"));
        tournamentPanel.add(listScroller);
    }

    private void setupTournamentButtons(JPanel tournamentPanel, Vector<String> tournamentNames) {
        JPanel buttonPanel = new JPanel();

        JButton creerTournoi = createActionButton("Créer un nouveau tournoi", e -> {
            Tournoi.creerTournoi(statement);
            tracerSelectTournoi();
        }, buttonPanel);

        selectTournoi = select.apply(buttonPanel,list);

        deleteTournoi = createActionButton("Supprimer le tournoi", e -> {
            String selectedTournament = list.getSelectedValue();
            if (selectedTournament != null) {
                Tournoi.deleteTournoi(statement, selectedTournament);
                tracerSelectTournoi();
            }
        }, buttonPanel);

        boolean hasTournaments = !tournamentNames.isEmpty();
        selectTournoi.setEnabled(hasTournaments);
        deleteTournoi.setEnabled(hasTournaments);

        tournamentPanel.add(buttonPanel);
    }

    protected JButton createActionButton(String text, ActionListener listener, JPanel parent) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(150, 30));
        button.addActionListener(listener);
        parent.add(button);
        return button;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(cardPanel, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }


}
