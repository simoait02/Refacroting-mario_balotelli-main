import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
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
		currentTournoi = null;
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
			currentTournoi.majEquipes();
			eq_modele.fireTableDataChanged();
		} else {
			equipes_trace = true;

			eq_p = new JPanel();
			eq_p.setLayout(new BoxLayout(eq_p, BoxLayout.Y_AXIS));
			eq_p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

			JLabel eq_desc = new JLabel("Équipes du tournoi");
			eq_p.add(eq_desc);

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

			eq_jt = new JTable(eq_modele);
			eq_p.add(new JScrollPane(eq_jt));

			JPanel buttonPanel = new JPanel();

			eq_ajouter = createActionButton("Ajouter une équipe", e -> {
				currentTournoi.ajouterEquipe();
				eq_modele.fireTableDataChanged();
				updateTeamButtonStates();
			}, buttonPanel);

			eq_supprimer = createActionButton("Supprimer une équipe", e -> {
				if(Fenetre.this.eq_jt.getSelectedRow() != -1){
					currentTournoi.supprimerEquipe(currentTournoi.getEquipe(Fenetre.this.eq_jt.getSelectedRow()).id);
				}
				eq_valider.setEnabled(currentTournoi.getNbEquipes() > 0 && currentTournoi.getNbEquipes() % 2 == 0) ;
				eq_modele.fireTableDataChanged();
				if(currentTournoi.getNbEquipes() > 0){
					eq_jt.getSelectionModel().setSelectionInterval(0, 0);
				}
				updateTeamButtonStates();
			}, buttonPanel);

			eq_valider = createActionButton("Valider les équipes", e -> {
				if (currentTournoi.getNbEquipes() % 2 != 0) {
					JOptionPane.showMessageDialog(this, "Le nombre d'équipes doit être pair pour continuer.", "Erreur", JOptionPane.ERROR_MESSAGE);
				} else {
					currentTournoi.genererMatchs();
					Fenetre.this.updateButtonStates();
					Fenetre.this.tracerToursTournoi();

				}
			}, buttonPanel);

			eq_p.add(buttonPanel);

			cardPanel.add(eq_p, "EQUIPES");
		}


		if(currentTournoi.getStatut() != 0){
			eq_ajouter.setEnabled(false);
			eq_supprimer.setEnabled(false);
			eq_valider.setEnabled(currentTournoi.getStatut() == 1);
		}else{
			eq_ajouter.setEnabled(true);
			eq_supprimer.setEnabled(true);
			eq_valider.setEnabled(currentTournoi.getNbEquipes() > 0) ;
		}
		((CardLayout) cardPanel.getLayout()).show(cardPanel, "EQUIPES");
	}

	private void updateTeamButtonStates() {
		boolean hasTeams = currentTournoi.getNbEquipes() > 0;
		eq_ajouter.setEnabled(currentTournoi.getStatut() == 0);
		eq_supprimer.setEnabled(hasTeams && currentTournoi.getStatut() == 0);
		eq_valider.setEnabled(hasTeams && currentTournoi.getNbEquipes() % 2 == 0);
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
			detailt_nom.setText(currentTournoi.getNom());
			detailt_statut.setText(currentTournoi.getNStatut());
			detailt_nbtours.setText(Integer.toString(currentTournoi.getNbTours()));
		} else {
			details_trace = true;

			JPanel detailsPanel = new JPanel();
			detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

			JLabel headerLabel = new JLabel("Détails du tournoi");
			detailsPanel.add(headerLabel);

			JPanel infoPanel = new JPanel(new GridLayout(3, 2));

			detailt_nom = new JLabel(currentTournoi.getNom());
			infoPanel.add(new JLabel("Nom du tournoi"));
			infoPanel.add(detailt_nom);

			detailt_statut = new JLabel(currentTournoi.getNStatut());
			infoPanel.add(new JLabel("Statut"));
			infoPanel.add(detailt_statut);

			detailt_nbtours = new JLabel(Integer.toString(currentTournoi.getNbTours()));
			infoPanel.add(new JLabel("Nombre de tours"));
			infoPanel.add(detailt_nbtours);

			detailsPanel.add(infoPanel);

			cardPanel.add(detailsPanel, "DETAIL");

			((CardLayout) cardPanel.getLayout()).show(cardPanel, "DETAIL");
		}
	}
	JTable                     tours_t;
	JScrollPane                tours_js;
	JPanel                     tours_p;

	JButton                    tours_ajouter;
	JButton                    tours_supprimer;
	JButton                    tours_rentrer;
	boolean tours_trace;

	public void tracerToursTournoi() {
		if (currentTournoi == null) {
			JOptionPane.showMessageDialog(this, "Aucun tournoi sélectionné !", "Erreur", JOptionPane.ERROR_MESSAGE);
			return;
		}

		updateButtonStates();

		Vector<Vector<Object>> data = new Vector<>();
		Vector<String> columnNames = new Vector<>();
		columnNames.add("Numéro du tour");
		columnNames.add("Nombre de matchs");
		columnNames.add("Matchs joués");

		boolean canAddTour = true;

		String query = """
        SELECT num_tour, COUNT(*) AS total_matchs,
               (SELECT COUNT(*) 
                FROM matchs m2 
                WHERE m2.id_tournoi = m.id_tournoi 
                  AND m2.num_tour = m.num_tour 
                  AND m2.termine = 'oui') AS termines
        FROM matchs m 
        WHERE m.id_tournoi = ? 
        GROUP BY m.num_tour, m.id_tournoi
    """;

		try (PreparedStatement ps = statement.getConnection().prepareStatement(query)) {
			ps.setInt(1, currentTournoi.getId());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Vector<Object> row = new Vector<>();
					int numTour = rs.getInt("num_tour");
					int totalMatchs = rs.getInt("total_matchs");
					int matchesPlayed = rs.getInt("termines");

					row.add(numTour);
					row.add(totalMatchs);
					row.add(matchesPlayed);
					data.add(row);

					canAddTour &= (totalMatchs == matchesPlayed);
				}
			}
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "Erreur lors de la récupération des données : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (tours_trace) {
			tours_t.setModel(new DefaultTableModel(data, columnNames));
		} else {
			tours_trace = true;

			tours_p = new JPanel();
			tours_p.setLayout(new BoxLayout(tours_p, BoxLayout.Y_AXIS));
			tours_p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

			JLabel toursLabel = new JLabel("Tours");
			tours_p.add(toursLabel);

			tours_t = new JTable(new DefaultTableModel(data, columnNames));
			tours_js = new JScrollPane(tours_t);
			tours_p.add(tours_js);

			JPanel buttonPanel = new JPanel();

			tours_ajouter = createActionButton("Ajouter un tour", e -> {
				currentTournoi.ajouterTour();
				tracerToursTournoi();
			}, buttonPanel);

			tours_supprimer = createActionButton("Supprimer le dernier tour", e -> {
				currentTournoi.supprimerTour();
				tracerToursTournoi();
			}, buttonPanel);

			tours_p.add(buttonPanel);

			tours_p.add(new JLabel("Pour ajouter un tour, terminez tous les matchs du précédent."));
			tours_p.add(new JLabel("Le nombre maximum de tours est \"le nombre d'équipes - 1\"."));

			cardPanel.add(tours_p, "TOURS");
		}

		tours_ajouter.setEnabled(canAddTour && currentTournoi.getNbTours() < currentTournoi.getNbEquipes() - 1);
		tours_supprimer.setEnabled(currentTournoi.getNbTours() > 1);

		((CardLayout) cardPanel.getLayout()).show(cardPanel, "TOURS");
	}


	private AbstractTableModel match_modele;
	private JScrollPane        match_js;
	JTable                     match_jt;
	JPanel                     match_p;
	JLabel                     match_statut;
	JButton                    match_valider;
	private boolean match_trace = false;
	public void tracerMatchsTournoi() {
		if (currentTournoi == null) {
			return;
		}

		updateButtonStates();

		if (match_trace) {
			refreshMatchData();
		} else {
			initializeMatchView();
			match_trace = true;
		}

		switchToMatchView();
	}

	private void refreshMatchData() {
		currentTournoi.majMatch();
		match_modele.fireTableDataChanged();
		majStatutM();
	}

	private void initializeMatchView() {
		match_p = createMatchPanel();
		cardPanel.add(match_p, "MATCHS");
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
		match_modele = createMatchTableModel();
		match_jt = new JTable(match_modele);
		match_js = new JScrollPane(match_jt);
		return match_js;
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

					currentTournoi.majMatch(row);
					fireTableDataChanged();
					Fenetre.this.updateButtonStates();
				} catch (NumberFormatException e) {
					// Handle invalid input gracefully
				}
			}
		};
	}

	private JPanel createMatchFooter() {
		JPanel footer = new JPanel();
		match_statut = new JLabel("?? Matchs joués");
		match_valider = new JButton("Afficher les résultats");
		match_valider.setEnabled(false);

		footer.add(match_statut);
		footer.add(match_valider);

		return footer;
	}

	private void switchToMatchView() {
		((CardLayout) cardPanel.getLayout()).show(cardPanel, "MATCHS");
	}
	private JScrollPane        resultats_js;
	JTable                     resultats_jt;
	JPanel                     resultats_p;
	BoxLayout                  resultats_layout;
	JLabel                     resultats_desc;
	JPanel                     resultats_bas;
	JLabel                     resultats_statut;
	private boolean resultats_trace=false;
	public void tracerResultatsTournoi(){
		if(currentTournoi == null){
			return ;
		}

		Vector< Vector<Object>> to =new Vector<Vector<Object>>();
		Vector<Object> v;
		try {
			ResultSet rs = statement.executeQuery("SELECT equipe,(SELECT nom_j1 FROM equipes e WHERE e.id_equipe = equipe AND e.id_tournoi = " + this.currentTournoi.getId() + ") as joueur1,(SELECT nom_j2 FROM equipes e WHERE e.id_equipe = equipe AND e.id_tournoi = " + this.currentTournoi.getId() + ") as joueur2, SUM(score) as score, (SELECT count(*) FROM matchs m WHERE (m.equipe1 = equipe AND m.score1 > m.score2  AND m.id_tournoi = id_tournoi) OR (m.equipe2 = equipe AND m.score2 > m.score1 )) as matchs_gagnes, (SELECT COUNT(*) FROM matchs m WHERE m.equipe1 = equipe OR m.equipe2=equipe) as matchs_joues FROM  (select equipe1 as equipe,score1 as score from matchs where id_tournoi=" + this.currentTournoi.getId() + " UNION select equipe2 as equipe,score2 as score from matchs where id_tournoi=" + this.currentTournoi.getId() + ") GROUP BY equipe ORDER BY matchs_gagnes DESC;");
			while(rs.next()){
				v = new Vector<Object>();
				v.add(rs.getInt("equipe"));
				v.add(rs.getString("joueur1"));
				v.add(rs.getString("joueur2"));
				v.add(rs.getInt("score"));
				v.add(rs.getInt("matchs_gagnes"));
				v.add(rs.getInt("matchs_joues"));
				to.add(v);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		Vector<String> columnNames = new Vector<String>();
		columnNames.add("Numéro d'équipe");
		columnNames.add("Nom joueur 1");
		columnNames.add("Nom joueur 2");
		columnNames.add("Score");
		columnNames.add("Matchs gagnés");
		columnNames.add("Matchs joués");
		resultats_jt = new JTable(to,columnNames );
		resultats_jt.setAutoCreateRowSorter(true);

		if(resultats_trace){
			resultats_js.setViewportView(resultats_jt);
		}else{
			resultats_trace = true;
			resultats_p      = new JPanel();
			resultats_layout = new BoxLayout(resultats_p, BoxLayout.Y_AXIS);

			resultats_p.setLayout(resultats_layout);
			resultats_desc = new JLabel("Résultats du tournoi");
			resultats_p.add(resultats_desc);
			resultats_p.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
			cardPanel.add(resultats_p, "RESULTATS" );
			resultats_js = new JScrollPane(resultats_jt);
			resultats_p.add(resultats_js);
			resultats_bas = new JPanel();
			resultats_bas.add(resultats_statut = new JLabel("Gagnant:"));

			resultats_p.add(resultats_bas);
		}
		((CardLayout) cardPanel.getLayout()).show(cardPanel, "RESULTATS");

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
			match_statut.setText(termines + "/" + total + " matchs terminés");
			match_valider.setEnabled(total == termines);
		} else {
			match_statut.setText("Erreur lors de la récupération des données");
			match_valider.setEnabled(false);
		}
	}

}