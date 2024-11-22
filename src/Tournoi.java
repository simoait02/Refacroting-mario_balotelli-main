import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Vector;

public class Tournoi {

	private String statutNom;
	private String nomTournoi;
	private int statut;
	private int idTournoi;
	private Vector<Equipe> dataEquipe = null;
	private Vector<MatchM> dataMatches = null;
	private Vector<Integer> idEquipes = null;
	private Statement statement;
	private TournoiDAO tournoiDAO;

	// Constructors
	public Tournoi() {}

	public Tournoi(String nomTournoi, Statement statement) {
		this.statement = statement;
		this.tournoiDAO = new TournoiDAO(statement);
		Tournoi tournoi = tournoiDAO.getTournoiByName(nomTournoi);
		this.statut = tournoi.getStatut();
		this.idTournoi = tournoi.getId();
		this.statutNom = getStatutNomFromStatut(this.statut);
		this.nomTournoi = nomTournoi;
	}

	// Statut handling
	private String getStatutNomFromStatut(int statut) {
		switch (statut) {
			case 0: return "Inscription des joueurs";
			case 1: return "Génération des matchs";
			case 2: return "Matchs en cours";
			case 3: return "Terminé";
			default: return "Inconnu";
		}
	}

	// Getters and Setters
	public int getId() { return idTournoi; }
	public String getNomTournoi() { return nomTournoi; }
	public void setNomTournoi(String nomTournoi) { this.nomTournoi = nomTournoi; }
	public int getStatut() { return statut; }
	public String getStatutNom() { return statutNom; }
	public void setStatut(int statut) {
		this.statut = statut;
		this.statutNom = getStatutNomFromStatut(statut);
	}

	public Vector<Equipe> getDataEquipe() {
		return dataEquipe;
	}

	public Vector<Integer> getIdEquipes() {
		return idEquipes;
	}

	// Data update and match handling
	public void updateData() {
		Map<String, Vector<?>> result = tournoiDAO.getUpdateData(idTournoi);
		dataEquipe = (Vector<Equipe>) result.get("equipes");
		idEquipes = (Vector<Integer>) result.get("equipesIds");
		dataMatches = (Vector<MatchM>) result.get("matchs");
	}

	public MatchM getMatch(int index) {
		if (dataMatches == null) updateData();
		return dataMatches.get(index);
	}

	public int getNbMatchs() {
		if (dataMatches == null) updateData();
		return dataMatches.size();
	}

	public Equipe getEquipe(int index) {
		if (dataEquipe == null) updateData();
		return dataEquipe.get(index);
	}

	public int getNbEquipes() {
		if (dataEquipe == null) updateData();
		return dataEquipe.size();
	}

	public int getNbTours() {
		try {
			ResultSet rs = statement.executeQuery("SELECT MAX(num_tour) FROM matchs WHERE id_tournoi=" + idTournoi + ";");
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return -1;
		}
	}

	public void genererMatchs() {
		tournoiDAO.genererMatchs(this);
	}

	public boolean ajouterTour() {
		if (getNbTours() >= (getNbEquipes() - 1)) return false;

		int nbtoursav = tournoiDAO.getMaxTourNumber(this);
		if (nbtoursav == -1) return false;

		if (nbtoursav == 0) {
			return tournoiDAO.createInitialMatches(nbtoursav, this);
		} else {
			return tournoiDAO.createNextRoundMatches(nbtoursav, this);
		}
	}

	public void supprimerTour() {
		tournoiDAO.supprimerTour(idTournoi);
	}

	public void setStatutNom(String statutNom) {
		this.statutNom = statutNom;
	}

	public int getIdTournoi() {
		return idTournoi;
	}

	public void setIdTournoi(int idTournoi) {
		this.idTournoi = idTournoi;
	}

	public void setDataEquipe(Vector<Equipe> dataEquipe) {
		this.dataEquipe = dataEquipe;
	}

	public Vector<MatchM> getDataMatches() {
		return dataMatches;
	}

	public void setDataMatches(Vector<MatchM> dataMatches) {
		this.dataMatches = dataMatches;
	}

	public void setIdEquipes(Vector<Integer> idEquipes) {
		this.idEquipes = idEquipes;
	}

	public Statement getStatement() {
		return statement;
	}

	public void setStatement(Statement statement) {
		this.statement = statement;
	}

	public TournoiDAO getTournoiDAO() {
		return tournoiDAO;
	}

	public void setTournoiDAO(TournoiDAO tournoiDAO) {
		this.tournoiDAO = tournoiDAO;
	}

	// Static methods for Tournament creation and deletion
	public static int deleteTournoi(Statement s2, String nomTournoi) {
		return TournoiDAO.deleteTournoi(s2, nomTournoi);
	}

	public static int creerTournoi(Statement s2) {
		return TournoiDAO.creerTournoi(s2);
	}

	// Team handling
	public void ajouterEquipe() {
		tournoiDAO.ajouterEquipe("\"Joueur 1\"", "\"Joueur 2\"", this);
	}
	public void updateEquipe(int index){
		tournoiDAO.updateEquipe(index,this);
	}
	public void updateMatch(int index) {
		tournoiDAO.updateMatch(index, this);
		updateData();
	}

	public void supprimerEquipe(int idEquipe) {
		tournoiDAO.supprimerEquipe(idEquipe, this);
	}

	// Match scheduling
	public static Vector<Vector<Match>> getMatchsToDo(int nbJoueurs, int nbTours) {
		if (nbTours >= nbJoueurs) {
			System.out.println("Erreur: Le nombre de tours ne peut pas être supérieur ou égal au nombre d'équipes.");
			return null;
		}

		int[] tabJoueurs;
		if (nbJoueurs % 2 == 1) {
			tabJoueurs = new int[nbJoueurs + 1];
			tabJoueurs[nbJoueurs] = -1;
			for (int z = 0; z < nbJoueurs; z++) {
				tabJoueurs[z] = z + 1;
			}
			nbJoueurs++;
		} else {
			tabJoueurs = new int[nbJoueurs];
			for (int z = 0; z < nbJoueurs; z++) {
				tabJoueurs[z] = z + 1;
			}
		}

		Vector<Vector<Match>> allMatches = new Vector<>();
		for (int round = 1; round <= nbTours; round++) {
			if (round > 1) {
				int lastPlayer = tabJoueurs[nbJoueurs - 2];
				System.arraycopy(tabJoueurs, 0, tabJoueurs, 1, nbJoueurs - 2);
				tabJoueurs[0] = lastPlayer;
			}

			Vector<Match> roundMatches = new Vector<>();
			int i = 0;
			while (i < nbJoueurs / 2) {
				if (tabJoueurs[i] != -1 && tabJoueurs[nbJoueurs - 1 - i] != -1) {
					roundMatches.add(new Match(tabJoueurs[i], tabJoueurs[nbJoueurs - 1 - i]));
				}
				i++;
			}
			allMatches.add(roundMatches);
		}
		return allMatches;
	}
}
