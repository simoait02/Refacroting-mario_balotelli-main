import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
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

	private boolean equipes_trace = false;
	private JPanel eq_p;
	private JTable eq_jt;
	private AbstractTableModel eq_modele;
	private JButton eq_ajouter, eq_supprimer, eq_valider;

	public void tracerEquipesTournoi() {
		if (currentTournoi == null) {
			JOptionPane.showMessageDialog(this, "Aucun tournoi sélectionné !", "Erreur", JOptionPane.ERROR_MESSAGE);
			return;
		}

		updateButtonStates();

		if (equipes_trace) {
			// Update the team data if already traced
			currentTournoi.majEquipes();
			eq_modele.fireTableDataChanged();
		} else {
			equipes_trace = true;

			// Create the main panel for teams
			eq_p = new JPanel();
			eq_p.setLayout(new BoxLayout(eq_p, BoxLayout.Y_AXIS));
			eq_p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

			JLabel eq_desc = new JLabel("Équipes du tournoi");
			eq_p.add(eq_desc);

			// Define the table model for the teams
			eq_modele = new AbstractTableModel() {
				private static final long serialVersionUID = 1L;

				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					Equipe equipe = currentTournoi.getEquipe(rowIndex);
					switch (columnIndex) {
						case 0:
							return equipe.getNum();
						case 1:
							return equipe.getEq1();
						case 2:
							return equipe.getEq2();
						default:
							return "??";
					}
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
					return columnIndex > 0 && currentTournoi.getStatut() == 0; // Editable if in initial state
				}

				@Override
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
					Equipe equipe = currentTournoi.getEquipe(rowIndex);
					if (columnIndex == 1) {
						equipe.setEq1((String) aValue);
					} else if (columnIndex == 2) {
						equipe.setEq2((String) aValue);
					}
					String updateQuery = "UPDATE equipes SET joueur1 = ?, joueur2 = ? WHERE id = ?";
					try (PreparedStatement ps = statement.getConnection().prepareStatement(updateQuery)) {
						ps.setString(1, equipe.getEq1());
						ps.setString(2, equipe.getEq2());
						ps.setInt(3, equipe.getId()); // Assuming each team has a unique ID
						ps.executeUpdate();
					} catch (SQLException ex) {
						JOptionPane.showMessageDialog(Fenetre.this, "Erreur lors de la mise à jour de l'équipe : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
					}

					fireTableDataChanged();
				}
			};

			// Create and add the table
			eq_jt = new JTable(eq_modele);
			eq_p.add(new JScrollPane(eq_jt));

			// Add action buttons for managing teams
			JPanel buttonPanel = new JPanel();

			eq_ajouter = createActionButton("Ajouter une équipe", e -> {
				currentTournoi.ajouterEquipe();
				eq_modele.fireTableDataChanged();
				updateTeamButtonStates();
			}, buttonPanel);

			eq_supprimer = createActionButton("Supprimer une équipe", e -> {
				int selectedRow = eq_jt.getSelectedRow();
				if (selectedRow != -1) {
					currentTournoi.supprimerEquipe(selectedRow);
					eq_modele.fireTableDataChanged();
					updateTeamButtonStates();
				}
			}, buttonPanel);

			eq_valider = createActionButton("Valider les équipes", e -> {
				if (currentTournoi.getNbEquipes() % 2 != 0) {
					JOptionPane.showMessageDialog(this, "Le nombre d'équipes doit être pair pour continuer.", "Erreur", JOptionPane.ERROR_MESSAGE);
				} else {
					currentTournoi.genererMatchs();
					updateButtonStates();
					tracerToursTournoi();
				}
			}, buttonPanel);

			eq_p.add(buttonPanel);

			cardPanel.add(eq_p, "EQUIPES");
		}

		// Update button states
		updateTeamButtonStates();

		// Show the panel
		((CardLayout) cardPanel.getLayout()).show(cardPanel, "EQUIPES");
	}

	// Helper to manage button states for teams
	private void updateTeamButtonStates() {
		boolean hasTeams = currentTournoi.getNbEquipes() > 0;
		eq_ajouter.setEnabled(currentTournoi.getStatut() == 0);
		eq_supprimer.setEnabled(hasTeams && currentTournoi.getStatut() == 0);
		eq_valider.setEnabled(hasTeams && currentTournoi.getNbEquipes() % 2 == 0 && currentTournoi.getStatut() == 1);
	}

	private boolean details_trace = false;
	private JLabel detailt_nom, detailt_statut, detailt_nbtours;

	private void tracerDetailsTournoi() {
		if (currentTournoi == null) {
			JOptionPane.showMessageDialog(this, "Aucun tournoi sélectionné !", "Erreur", JOptionPane.ERROR_MESSAGE);
			return;
		}

		updateButtonStates();

		if (details_trace) {
			// If the tournament details are already traced, just update the labels
			detailt_nom.setText(currentTournoi.getNom());
			detailt_statut.setText(currentTournoi.getNStatut());
			detailt_nbtours.setText(Integer.toString(currentTournoi.getNbTours()));
		} else {
			// If the tournament details have not been traced yet, set them up
			details_trace = true;

			// Create the panel for displaying tournament details
			JPanel detailsPanel = new JPanel();
			detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

			// Create a label for the tournament header
			JLabel headerLabel = new JLabel("Détails du tournoi");
			detailsPanel.add(headerLabel);

			// Create a panel to organize the labels in a grid layout
			JPanel infoPanel = new JPanel(new GridLayout(3, 2));

			// Tournament name
			detailt_nom = new JLabel(currentTournoi.getNom());
			infoPanel.add(new JLabel("Nom du tournoi"));
			infoPanel.add(detailt_nom);

			// Tournament status
			detailt_statut = new JLabel(currentTournoi.getNStatut());
			infoPanel.add(new JLabel("Statut"));
			infoPanel.add(detailt_statut);

			// Number of rounds
			detailt_nbtours = new JLabel(Integer.toString(currentTournoi.getNbTours()));
			infoPanel.add(new JLabel("Nombre de tours"));
			infoPanel.add(detailt_nbtours);

			// Add the info panel to the details panel
			detailsPanel.add(infoPanel);

			// Add the details panel to the cardPanel under the "DETAIL" identifier
			cardPanel.add(detailsPanel, "DETAIL");

			// Set the cardLayout to show the tournament details view
			((CardLayout) cardPanel.getLayout()).show(cardPanel, "DETAIL");
		}
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