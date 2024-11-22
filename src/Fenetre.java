import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.sql.*;

import java.util.function.BiFunction;


public class Fenetre extends JFrame {

	protected JPanel cardPanel;
	protected TournoiToursTracer tournoiTousTracer;
	private JLabel statutSelect;
	private JButton btnTournois, btnParams, btnEquipes, btnTours, btnMatchs, btnResultats;


	private Statement statement;

	private Tournoi currentTournoi;
	TournoiToursTracer tournoiTracer;
	EquipeTracer equipeTracer;
	TournoiDetailsTracer tournoiDetailsTracer;
	TournoiSelectorTracer tournoiSelectorTracer;
	MatchTracer matchTracer;
	TournamentResultsTracer tournamentResultsTracer;

	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 30;
	private static final String DEFAULT_STATUT = "Statut : ";
	private static final String DEFAULT_TITLE = "Gestion de tournoi de Belote";
	private static final String TOURNOIS = "TOURNOIS";

	public Fenetre(Statement statement) {
		this.statement = statement;
		setupUI();
		setupActions();
		tournoiSelectorTracer=new TournoiSelectorTracer(cardPanel,statement,tournoiTracer,equipeTracer,tournoiDetailsTracer, msg -> updateStatus(msg),this::updateButtonStates,select);
		updateStatus("Pas de tournoi sélectionné");
		tournoiSelectorTracer.tracerSelectTournoi();
		tournoiTracer=new TournoiToursTracer(currentTournoi,statement,cardPanel,this::updateButtonStates);
		equipeTracer=new EquipeTracer(currentTournoi,cardPanel,tournoiTracer, this::updateButtonStates);
		tournoiDetailsTracer=new TournoiDetailsTracer(currentTournoi,cardPanel,this::updateButtonStates);
		tournamentResultsTracer=new TournamentResultsTracer(cardPanel,statement);
		matchTracer=new MatchTracer(currentTournoi,cardPanel,this::updateButtonStates,tournamentResultsTracer,statement);
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


	BiFunction<JPanel, JList<String>, JButton> select = (parent, list) -> {
		JButton button = createActionButton("Sélectionner le tournoi", e -> {
			String selectedTournament = list.getSelectedValue();
			if (selectedTournament != null) {
				currentTournoi = new Tournoi(selectedTournament, statement);
				tournoiTracer.setCurrentTournoie(currentTournoi);
				equipeTracer.setCurrentTournoi(currentTournoi);
				tournoiDetailsTracer.setCurrentTournoi(currentTournoi);
				matchTracer.setCurrentTournoi(currentTournoi);
				tournamentResultsTracer.setCurrentTournoi(currentTournoi);
				tournoiDetailsTracer.tracerDetailsTournoi();
				updateStatus("Tournoi \"" + selectedTournament + "\"");
			}
		}, parent);

		parent.add(button);
		return button;
	};


	private void setupActions() {
		btnTournois.addActionListener(e -> tournoiSelectorTracer.tracerSelectTournoi());
		btnParams.addActionListener(e -> tournoiDetailsTracer.tracerDetailsTournoi());
		btnEquipes.addActionListener(e -> equipeTracer.traceTeams());
		btnTours.addActionListener(e -> tournoiTracer.traceTours());
		btnMatchs.addActionListener(e -> matchTracer.tracerMatchsTournoi());
		btnResultats.addActionListener(e -> tournamentResultsTracer.traceTournamentResults());
	}

	private void updateStatus(String text) {
		statutSelect.setText(DEFAULT_STATUT + text);
	}

	protected void updateButtonStates() {
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


	protected JButton createActionButton(String text, ActionListener listener, JPanel parent) {
		JButton button = new JButton(text);
		button.setPreferredSize(new Dimension(150, 30));
		button.addActionListener(listener);
		parent.add(button);
		return button;
	}


}