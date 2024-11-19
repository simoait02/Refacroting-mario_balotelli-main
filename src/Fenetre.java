import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Vector;

public class Fenetre extends JFrame {
	private static final long serialVersionUID = 1L;

	// UI Components
	private JPanel cardPanel;
	private JLabel statutSelect;
	private JButton btnTournois, btnParams, btnEquipes, btnTours, btnMatchs, btnResultats;

	private JList<String> list;
	private JButton creerTournoi, selectTournoi, deleteTournoi;

	// Database Statement
	private final Statement statement;

	// Tournament Information
	private Tournoi currentTournoi;

	// Constants
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 30;
	private static final String DEFAULT_STATUT = "Statut : ";
	private static final String DEFAULT_TITLE = "Gestion de tournoi de Belote";
	private static final String TOURNOIS = "TOURNOIS";

	public Fenetre(Statement statement) {
		this.statement = statement;
		setupUI();
		setupActions();
		updateStatus("Pas de tournoi sélectionné");
		tracerSelectTournoi();
	}

	private void setupUI() {
		setTitle(DEFAULT_TITLE);
		setSize(800, 400);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		JPanel contentPanel = new JPanel(new BorderLayout());
		setContentPane(contentPanel);

		setupNorthPanel(contentPanel);
		setupWestPanel(contentPanel);
		setupCenterPanel(contentPanel);
	}

	private void setupNorthPanel(JPanel parent) {
		JPanel northPanel = new JPanel();
		statutSelect = new JLabel();
		northPanel.add(statutSelect);
		parent.add(northPanel, BorderLayout.NORTH);
	}

	private void setupWestPanel(JPanel parent) {
		JPanel westPanel = new JPanel();
		westPanel.setBackground(Color.RED);
		westPanel.setPreferredSize(new Dimension(130, 0));
		parent.add(westPanel, BorderLayout.WEST);

		btnTournois = createButton("Tournois", westPanel);
		btnParams = createButton("Paramètres", westPanel);
		btnEquipes = createButton("Équipes", westPanel);
		btnTours = createButton("Tours", westPanel);
		btnMatchs = createButton("Matchs", westPanel);
		btnResultats = createButton("Résultats", westPanel);
	}

	private void setupCenterPanel(JPanel parent) {
		cardPanel = new JPanel(new CardLayout());
		parent.add(cardPanel, BorderLayout.CENTER);
	}

	private JButton createButton(String text, JPanel parent) {
		JButton button = new JButton(text);
		button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
		parent.add(button);
		return button;
	}

	private void setupActions() {
		btnTournois.addActionListener(e -> tracerSelectTournoi());
		btnParams.addActionListener(e -> tracerDetailsTournoi());
		btnEquipes.addActionListener(e -> tracerEquipesTournoi());
		btnTours.addActionListener(e -> tracerToursTournoi());
		btnMatchs.addActionListener(e -> tracerMatchsTournoi());
		btnResultats.addActionListener(e -> tracerResultatsTournoi());
	}

	private void updateStatus(String text) {
		statutSelect.setText(DEFAULT_STATUT + text);
	}

	private void updateButtonStates() {
		boolean tournoiSelected = currentTournoi != null;

		btnTournois.setEnabled(true);
		btnParams.setEnabled(tournoiSelected);
		btnEquipes.setEnabled(tournoiSelected);
		btnTours.setEnabled(tournoiSelected && currentTournoi.getStatut() >= 2);
		btnMatchs.setEnabled(tournoiSelected && currentTournoi.getStatut() >= 2 && currentTournoi.getNbTours() > 0);

		if (tournoiSelected && currentTournoi.getStatut() == 2) {
			try {
				boolean allMatchesFinished = areAllMatchesFinished(currentTournoi.getId());
				btnResultats.setEnabled(allMatchesFinished);
			} catch (SQLException ex) {
				ex.printStackTrace();
				btnResultats.setEnabled(false);
			}
		} else {
			btnResultats.setEnabled(false);
		}
	}

	private boolean areAllMatchesFinished(int tournoiId) throws SQLException {
		String query = """
                SELECT COUNT(*) AS total, 
                       SUM(CASE WHEN termine = 'oui' THEN 1 ELSE 0 END) AS termines 
                FROM matchs 
                WHERE id_tournoi = ?;
                """;
		try (PreparedStatement ps = statement.getConnection().prepareStatement(query)) {
			ps.setInt(1, tournoiId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				int total = rs.getInt("total");
				int termines = rs.getInt("termines");
				return total > 0 && total == termines;
			}
		}
		return false;
	}

	public void tracerSelectTournoi() {
		currentTournoi = null; // Reset the selected tournament
		updateButtonStates();
		updateStatus("Sélection d'un tournoi");

		Vector<String> tournamentNames = fetchTournamentNames();

		if (isTournamentPanelTraced()) {
			updateTournamentList(tournamentNames);
			return;
		}

		JPanel tournamentPanel = createTournamentPanel(tournamentNames);
		cardPanel.add(tournamentPanel, TOURNOIS);

		((CardLayout) cardPanel.getLayout()).show(cardPanel, TOURNOIS);
	}

	private Vector<String> fetchTournamentNames() {
		Vector<String> tournamentNames = new Vector<>();
		try (ResultSet rs = statement.executeQuery("SELECT * FROM tournois")) {
			while (rs.next()) {
				tournamentNames.add(rs.getString("nom_tournoi"));
			}
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "Erreur lors de la requête : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
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

		((CardLayout) cardPanel.getLayout()).show(cardPanel, TOURNOIS);
	}

	private JPanel createTournamentPanel(Vector<String> tournamentNames) {
		JPanel tournamentPanel = new JPanel();
		tournamentPanel.setLayout(new BoxLayout(tournamentPanel, BoxLayout.Y_AXIS));

		JTextArea headerText = new JTextArea("Gestion des tournois\nXXXXX XXXXXXXX, juillet 2012");
		headerText.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerText.setEditable(false);
		tournamentPanel.add(headerText);

		populateTournamentList(tournamentNames, tournamentPanel);
		setupTournamentButtons(tournamentPanel,tournamentNames);

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

	private void setupTournamentButtons(JPanel tournamentPanel,Vector<String> tournamentNames) {
		JPanel buttonPanel = new JPanel();
		creerTournoi = createActionButton("Créer un nouveau tournoi", e -> {
			Tournoi.creerTournoi(statement);
			tracerSelectTournoi();
		}, buttonPanel);

		selectTournoi = createActionButton("Sélectionner le tournoi", e -> {
			String selectedTournament = list.getSelectedValue();
			if (selectedTournament != null) {
				currentTournoi = new Tournoi(selectedTournament, statement);
				tracerDetailsTournoi();
				updateStatus("Tournoi \"" + selectedTournament + "\"");
			}
		}, buttonPanel);

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

	private JButton createActionButton(String text, ActionListener listener, JPanel parent) {
		JButton button = new JButton(text);
		button.setPreferredSize(new Dimension(150, 30));
		button.addActionListener(listener);
		parent.add(button);
		return button;
	}

	private void tracerDetailsTournoi() {
		// Logic to display tournament details
	}

	private void tracerEquipesTournoi() {
		// Logic to display tournament teams
	}

	private void tracerToursTournoi() {
		// Logic to display tournament rounds
	}

	private void tracerMatchsTournoi() {
		// Logic to display tournament matches
	}

	private void tracerResultatsTournoi() {
		// Logic to display tournament results
	}
}
