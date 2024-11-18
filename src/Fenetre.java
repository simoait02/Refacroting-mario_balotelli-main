import FenetreFactory.FenetreFactory;
import observer.TournamentButton;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;



public class Fenetre extends JFrame {
	private static final long serialVersionUID = 1L;
	public JPanel c;
	Statement s;
	FenetreFactory fenetreFactory=new FenetreFactory();
    private JList<String> list;

    private boolean tournois_trace  = false;
    private boolean equipes_trace   = false;
	private boolean tours_trace     = false;
	private boolean match_trace     = false;
	private boolean resultats_trace = false;

	private final CardLayout fen;
	final static String TOURNOIS = "Tournois";
    final static String DETAIL   = "Paramètres du tournoi";
    final static String EQUIPES  = "Equipes";
    final static String TOURS    = "Tours";
    final static String MATCHS   = "Matchs";
    final static String RESULTATS= "Resultats";
    public Tournoi t = null;

    private final JLabel statut_slect;
	private Page1 Page1;
	private Page2 Page2;
	private Page3 Page3;
	private Page4 Page4;
	private Page5 Page5;
	private Page6 Page6;

	public Fenetre(Statement st){

		s = st;
		this.setTitle("Gestion de tournoi de Belote");
		setSize(800,400);
		this.setVisible(true);
		this.setLocationRelativeTo(this.getParent());
		JPanel contenu = new JPanel();
		contenu.setLayout(new BorderLayout());
		this.setContentPane(contenu);
		JPanel phaut = new JPanel();
		contenu.add(phaut,BorderLayout.NORTH);
		phaut.add(statut_slect = new JLabel());
		this.setStatutSelect("Pas de tournoi sélectionné");

		JPanel pgauche = new JPanel();
		pgauche.setBackground(Color.RED);
		pgauche.setPreferredSize(new Dimension(130,0));
		contenu.add(pgauche,BorderLayout.WEST);


		int taille_boutons = 100;
		int hauteur_boutons = 30;
		fenetreFactory.createButton("createTournoi",new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Tournoi.creerTournoi(Fenetre.this.s);
				Fenetre.this.Page1.tracerSelectTournoi(fenetreFactory);
			}
		});
		fenetreFactory.createButton("deleteTournoi",new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Tournoi.deleteTournoi(Fenetre.this.s, Fenetre.this.list.getSelectedValue());
				Fenetre.this.Page1.tracerSelectTournoi(fenetreFactory);
			}
		});
		fenetreFactory.createButton("selectTournoi",new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String nt = Fenetre.this.list.getSelectedValue();
				Fenetre.this.t = new Tournoi(nt, Fenetre.this.s);
				Fenetre.this.Page2.tracerDetailsTournoi(t,fen);
				Fenetre.this.setStatutSelect("Tournoi \" " + nt + " \"");

			}
		});
		fenetreFactory.createButton("Tournois",new Dimension(taille_boutons,hauteur_boutons),new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Page1.tracerSelectTournoi(fenetreFactory);
			}
		});
		fenetreFactory.createButton("Paramètres",new Dimension(taille_boutons,hauteur_boutons),new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Page4.tracer_tours_tournoi(t,fenetreFactory,fen);
			}
		});
		fenetreFactory.createButton("Equipes",new Dimension(taille_boutons,hauteur_boutons),new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Page2.tracerDetailsTournoi(t,fen);
			}
		});
		fenetreFactory.createButton("Tours",new Dimension(taille_boutons,hauteur_boutons),new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Page3.tracerTournoiEquipes(fenetreFactory,t);
			}
		});
		fenetreFactory.createButton("Matchs",new Dimension(taille_boutons,hauteur_boutons),new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Page5.tracerTournoiMatchs(t);
			}
		});
		fenetreFactory.createButton("Resultats",new Dimension(taille_boutons,hauteur_boutons),new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Page6.tracer_tournoi_resultats(t);
			}
		});

		pgauche.add(fenetreFactory.getButton("Tournois"));
		pgauche.add(fenetreFactory.getButton("Paramètres"));
		pgauche.add(fenetreFactory.getButton("Equipes"));
		pgauche.add(fenetreFactory.getButton("Tours"));
		pgauche.add(fenetreFactory.getButton("Matchs"));
		pgauche.add(fenetreFactory.getButton("Resultats"));
		TournamentButton tournamentButton= new TournamentButton();
		tournamentButton.addObserver(fenetreFactory.getButton("Tournois"),-1);
		tournamentButton.addObserver(fenetreFactory.getButton("Paramètres"),-1);
		tournamentButton.addObserver(fenetreFactory.getButton("Equipes"),-1);
		tournamentButton.addObserver(fenetreFactory.getButton("Tours"),2);
		tournamentButton.addObserver(fenetreFactory.getButton("Matchs"),2);
		tournamentButton.addObserver(fenetreFactory.getButton("Resultats"),2);
		fen = new CardLayout();
		c = new JPanel(fen);
		contenu.add(c,BorderLayout.CENTER);
		Page1.tracerSelectTournoi(fenetreFactory);
	}

	public void setStatutSelect(String t){
        String statut_deft = "Gestion de tournois de Belote v1.0 - ";
        statut_slect.setText(statut_deft + t);
	}

    JLabel match_statut;
    JButton match_valider;



	private void majStatutM(){
		int total=-1, termines=-1;
		try {
			ResultSet rs = s.executeQuery("Select count(*) as total, (Select count(*) from matchs m2  WHERE m2.id_tournoi = m.id_tournoi  AND m2.termine='oui' ) as termines from matchs m  WHERE m.id_tournoi=" + this.t.id_tournoi +" GROUP by id_tournoi ;");
			rs.next();
			total    = rs.getInt(1);
			termines = rs.getInt(2);
		} catch (SQLException e) {
			e.printStackTrace();
			return ;
		}
		match_statut.setText(termines + "/" + total + " matchs terminés");
		match_valider.setEnabled(total == termines);
	}
}