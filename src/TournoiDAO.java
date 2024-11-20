import javax.swing.*;
import java.sql.*;
import java.util.*;

public class TournoiDAO {
    private final Statement statement;
    Tournoi tournoi =new Tournoi();
    public TournoiDAO(Statement statement) {
        this.statement = statement;
    }

    public Tournoi getTournoiByName(String nomTournoi) {
        String query = "SELECT * FROM tournois WHERE nom_tournoi = ?";
        try (PreparedStatement preparedStatement = statement.getConnection().prepareStatement(query)) {
            preparedStatement.setString(1, nomTournoi);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                Tournoi tournoi = new Tournoi();
                tournoi.setStatut(rs.getInt("statut"));
                tournoi.setIdTournoi(rs.getInt("id_tournoi"));
                rs.close();
                return tournoi;
            }
        } catch (SQLException e) {
            System.out.println("Error fetching tournament by name: " + e.getMessage());
        }
        return null;
    }


    public Map<String, Vector<?>> getUpdateData(int idTournoi) {
        Map<String, Vector<?>> result = new HashMap<>(); // To store both Equipe and MatchM data
        Vector<Equipe> dataEquipe = new Vector<>();
        Vector<Integer> ideqs = new Vector<>();
        Vector<MatchM> dataMatch = new Vector<>();

        try {
            // Fetch Equipe data
            ResultSet rsEquipe = statement.executeQuery(
                    "SELECT * FROM equipes WHERE id_tournoi = " + idTournoi + " ORDER BY num_equipe;"
            );
            while (rsEquipe.next()) {
                dataEquipe.add(new Equipe(
                        rsEquipe.getInt("id_equipe"),
                        rsEquipe.getInt("num_equipe"),
                        rsEquipe.getString("nom_j1"),
                        rsEquipe.getString("nom_j2")
                ));
                ideqs.add(rsEquipe.getInt("num_equipe"));
            }
            rsEquipe.close();

            ResultSet rsMatch = statement.executeQuery(
                    "SELECT * FROM matchs WHERE id_tournoi = " + idTournoi + ";"
            );
            while (rsMatch.next()) {
                dataMatch.add(new MatchM(
                        rsMatch.getInt("id_match"),
                        rsMatch.getInt("equipe1"),
                        rsMatch.getInt("equipe2"),
                        rsMatch.getInt("score1"),
                        rsMatch.getInt("score2"),
                        rsMatch.getInt("num_tour"),
                        rsMatch.getString("termine").equalsIgnoreCase("oui") // Correct string comparison
                ));
            }
            rsMatch.close();

            // Store data in the map
            result.put("equipes", dataEquipe);
            result.put("equipesIds", ideqs);
            result.put("matchs", dataMatch);

            return result;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    public void genererMatchs(Tournoi tournoi) {
        int nbt = 1;

        System.out.println("Nombre d'�quipes : " + tournoi.getNbEquipes());
        System.out.println("Nombre de tours  : " + nbt);
        String req = "INSERT INTO matchs ( id_match, id_tournoi, num_tour, equipe1, equipe2, termine ) VALUES\n";
        Vector<Vector<Match>> ms;
        ms = Tournoi.getMatchsToDo(tournoi.getNbEquipes(), nbt);
        int z = 1;
        char v = ' ';
        for(Vector<Match> t :ms){
            for(Match m:t){
                req += v + "(NULL," + tournoi.getId() + ", " + z + ", "+  m.eq1 + ", " +  m.eq2 + ", 'non')";
                v = ',';
            }
            req += "\n";
            z++;
        }
        System.out.println(req);
        try{
            statement.executeUpdate(req);
            statement.executeUpdate("UPDATE tournois SET statut=2 WHERE id_tournoi=" + tournoi.getId() + ";");
            tournoi.setStatut(2);
        }catch(SQLException e){
            System.out.println("Erreur validation �quipes : " + e.getMessage());
        }
    }




    protected int getMaxTourNumber(Tournoi tournoi) {
        try {
            ResultSet rs = statement.executeQuery(
                    "SELECT MAX(num_tour) FROM matchs WHERE id_tournoi = " + tournoi.getId() + ";"
            );
            if (rs.next()) {
                int maxTour = rs.getInt(1);
                rs.close();
                return maxTour;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération du nombre maximum de tours: " + e.getMessage());
        }
        return -1; // Error case
    }

    protected boolean createInitialMatches(int nbtoursav, Tournoi tournoi) {
        try {
            Vector<Match> matches = Tournoi.getMatchsToDo(tournoi.getNbEquipes(), nbtoursav + 1).lastElement();
            StringBuilder queryBuilder = new StringBuilder(
                    "INSERT INTO matchs (id_match, id_tournoi, num_tour, equipe1, equipe2, termine) VALUES\n"
            );

            char delimiter = ' ';
            for (Match match : matches) {
                queryBuilder.append(delimiter)
                        .append("(NULL, ")
                        .append(tournoi.getId())
                        .append(", ")
                        .append(nbtoursav + 1)
                        .append(", ")
                        .append(match.eq1)
                        .append(", ")
                        .append(match.eq2)
                        .append(", 'non')");
                delimiter = ',';
            }

            statement.executeUpdate(queryBuilder.toString());
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur ajout tour : " + e.getMessage());
            return false;
        }
    }

    protected boolean createNextRoundMatches(int nbtoursav, Tournoi tournoi) {
        try {
            ArrayList<Integer> teamOrder = getTeamOrder(tournoi);
            if (teamOrder.isEmpty()) {
                return false;
            }

            StringBuilder queryBuilder = new StringBuilder(
                    "INSERT INTO matchs (id_match, id_tournoi, num_tour, equipe1, equipe2, termine) VALUES\n"
            );
            char delimiter = ' ';

            while (teamOrder.size() > 1) {
                for (int i = 1; i < teamOrder.size(); i++) {
                    if (!isMatchPlayed(teamOrder.get(0), teamOrder.get(i))) {
                        queryBuilder.append(delimiter)
                                .append("(NULL, ")
                                .append(tournoi.getId())
                                .append(", ")
                                .append(nbtoursav + 1)
                                .append(", ")
                                .append(teamOrder.get(0))
                                .append(", ")
                                .append(teamOrder.get(i))
                                .append(", 'non')");
                        delimiter = ',';

                        teamOrder.remove(i);
                        teamOrder.remove(0);
                        break;
                    }
                }
            }

            System.out.println(queryBuilder);
            statement.executeUpdate(queryBuilder.toString());
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur création des matchs pour le prochain tour : " + e.getMessage());
            return false;
        }
    }

    private ArrayList<Integer> getTeamOrder(Tournoi tournoi) throws SQLException {
        ArrayList<Integer> teamOrder = new ArrayList<>();
        String query = """
        SELECT equipe, 
               (SELECT COUNT(*) 
                FROM matchs m 
                WHERE (m.equipe1 = equipe AND m.score1 > m.score2 AND m.id_tournoi = id_tournoi) 
                   OR (m.equipe2 = equipe AND m.score2 > m.score1)) AS matchs_gagnes 
        FROM (SELECT equipe1 AS equipe FROM matchs WHERE id_tournoi = %d
              UNION 
              SELECT equipe2 AS equipe FROM matchs WHERE id_tournoi = %d) t
        GROUP BY equipe 
        ORDER BY matchs_gagnes DESC;
        """.formatted(tournoi.getId(), tournoi.getId());

        ResultSet rs = statement.executeQuery(query);
        while (rs.next()) {
            teamOrder.add(rs.getInt("equipe"));
        }
        rs.close();
        return teamOrder;
    }

    private boolean isMatchPlayed(int team1, int team2) throws SQLException {
        String query = """
        SELECT COUNT(*) 
        FROM matchs m 
        WHERE ((m.equipe1 = %d AND m.equipe2 = %d) 
            OR (m.equipe2 = %d AND m.equipe1 = %d))
        """.formatted(team1, team2, team1, team2);

        ResultSet rs = statement.executeQuery(query);
        rs.next();
        boolean isPlayed = rs.getInt(1) > 0;
        rs.close();
        return isPlayed;
    }


    public void supprimerTour(int idTournoi) {
        int nbtoursav = -1;

        // Step 1: Fetch the maximum tour number for the tournament
        String getMaxTourQuery = "SELECT MAX(num_tour) FROM matchs WHERE id_tournoi = ?";
        try (PreparedStatement psGetMaxTour = statement.getConnection().prepareStatement(getMaxTourQuery)) {
            psGetMaxTour.setInt(1, idTournoi);
            ResultSet rs = psGetMaxTour.executeQuery();
            if (rs.next()) {
                nbtoursav = rs.getInt(1);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error fetching maximum tour number: " + e.getMessage());
            return;
        }

        // If no rounds exist, exit the method
        if (nbtoursav == -1) {
            System.out.println("No tours found for tournament ID: " + idTournoi);
            return;
        }

        // Step 2: Delete the latest tour
        String deleteTourQuery = "DELETE FROM matchs WHERE id_tournoi = ? AND num_tour = ?";
        try (PreparedStatement psDeleteTour = statement.getConnection().prepareStatement(deleteTourQuery)) {
            psDeleteTour.setInt(1, idTournoi);
            psDeleteTour.setInt(2, nbtoursav);
            int rowsAffected = psDeleteTour.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Successfully deleted tour " + nbtoursav + " for tournament ID: " + idTournoi);
            } else {
                System.out.println("No matches were deleted. Check if the tour exists.");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting tour: " + e.getMessage());
        }
    }

    public static int deleteTournoi(Statement s2, String nomtournoi) {
        String getIdQuery = "SELECT id_tournoi FROM tournois WHERE nom_tournoi = ?";
        String deleteMatchQuery = "DELETE FROM matchs WHERE id_tournoi = ?";
        String deleteEquipeQuery = "DELETE FROM equipes WHERE id_tournoi = ?";
        String deleteTournoiQuery = "DELETE FROM tournois WHERE id_tournoi = ?";

        try (
                PreparedStatement psGetId = s2.getConnection().prepareStatement(getIdQuery);
                PreparedStatement psDeleteMatch = s2.getConnection().prepareStatement(deleteMatchQuery);
                PreparedStatement psDeleteEquipe = s2.getConnection().prepareStatement(deleteEquipeQuery);
                PreparedStatement psDeleteTournoi = s2.getConnection().prepareStatement(deleteTournoiQuery)
        ) {
            // Step 1: Retrieve the tournament ID
            psGetId.setString(1, nomtournoi);
            ResultSet rs = psGetId.executeQuery();
            if (!rs.next()) {
                System.out.println("Tournament not found: " + nomtournoi);
                return -1;  // Tournament not found
            }

            int idt = rs.getInt(1);
            rs.close();
            System.out.println("ID of the tournament to delete: " + idt);

            // Step 2: Delete associated matches, teams, and the tournament
            psDeleteMatch.setInt(1, idt);
            psDeleteEquipe.setInt(1, idt);
            psDeleteTournoi.setInt(1, idt);

            // Execute deletion queries
            psDeleteMatch.executeUpdate();
            psDeleteEquipe.executeUpdate();
            int rowsDeleted = psDeleteTournoi.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("Tournament deleted successfully.");
                return 1;  // Success
            } else {
                System.out.println("Failed to delete tournament.");
                return 0;  // Failure
            }

        } catch (SQLException e) {
            System.out.println("SQL error during tournament deletion: " + e.getMessage());
            return -2;  // SQL error
        } catch (Exception e) {
            System.out.println("Unknown error occurred during tournament deletion.");
            return -3;  // Unknown error
        }
    }

    public static int creerTournoi(Statement s2) {
        String tournoiName = (String) JOptionPane.showInputDialog(
                null,
                "Entrez le nom du tournoi",
                "Nom du tournoi",
                JOptionPane.PLAIN_MESSAGE
        );

        // Check if the user canceled the input
        if (tournoiName == null || tournoiName.trim().isEmpty()) {
            return 1; // Cancelled or empty input
        }

        // Escape special characters in the tournament name
        try {
            tournoiName = mysql_real_escape_string(tournoiName);
            if (tournoiName.length() < 3) {
                JOptionPane.showMessageDialog(null, "Le tournoi n'a pas été créé. Nom trop court.");
                return 2; // Name is too short
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erreur lors de la validation du nom.");
            return 3; // Error in name validation
        }

        // Check for invalid characters in the name
        if (tournoiName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Le tournoi n'a pas été créé. Ne pas mettre de caractères spéciaux ou accents dans le nom.");
            return 2; // Invalid characters in name
        }

        // Check if a tournament with the same name already exists
        String queryCheckExists = "SELECT id_tournoi FROM tournois WHERE nom_tournoi = ?";
        try (PreparedStatement psCheck = s2.getConnection().prepareStatement(queryCheckExists)) {
            psCheck.setString(1, tournoiName);
            ResultSet rs = psCheck.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(null, "Le tournoi n'a pas été créé. Un tournoi du même nom existe déjà.");
                return 2; // Tournament with the same name exists
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la vérification de l'existence du tournoi: " + e.getMessage());
            return 4; // Error checking if tournament exists
        }

        // Proceed to insert the new tournament
        String queryInsert = "INSERT INTO tournois (id_tournoi, nb_matchs, nom_tournoi, statut) VALUES (NULL, 10, ?, 0)";
        try (PreparedStatement psInsert = s2.getConnection().prepareStatement(queryInsert)) {
            psInsert.setString(1, tournoiName);
            psInsert.executeUpdate();
            JOptionPane.showMessageDialog(null, "Tournoi créé avec succès.");
            return 0; // Success
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'insertion du tournoi: " + e.getMessage());
            return 5; // Error inserting the tournament
        }
    }

    public void ajouterEquipe(String joueur1, String joueur2,Tournoi tournoi) {
        // Determine the next available team number
        int a_aj = tournoi.getDataEquipe().size() + 1;  // Assuming the size of dataEquipe is a good indicator of the team number

        // Try to find the next available team number that is not in idEquipes
        for (int i = 1; i <= tournoi.getDataEquipe().size(); i++) {
            if (!tournoi.getIdEquipes().contains(i)) {
                a_aj = i;
                break;
            }
        }

        // Insert the new team into the 'equipes' table
        String query = "INSERT INTO equipes (id_equipe, num_equipe, id_tournoi, nom_j1, nom_j2) VALUES (NULL, ?, ?, ?, ?)";
        try (PreparedStatement ps = statement.getConnection().prepareStatement(query)) {
            // Set the values dynamically
            ps.setInt(1, a_aj);                // Set the team number
            ps.setInt(2, tournoi.getId());           // Set the tournament ID
            ps.setString(3, joueur1);          // Set player 1 name
            ps.setString(4, joueur2);          // Set player 2 name

            // Execute the update
            ps.executeUpdate();
            tournoi.updateData();  // Update the data after inserting the new team
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout de l'équipe : " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void updateMatch(int index,Tournoi tournoi) {
        MatchM match = tournoi.getMatch(index);  // Call getMatch once and store the result in a variable

        // Determine whether the match is finished
        String termine = (match.score1 > 0 || match.score2 > 0) ? "oui" : "non";
        System.out.println(termine);

        // Create the SQL query using placeholders
        String query = "UPDATE matchs SET equipe1 = ?, equipe2 = ?, score1 = ?, score2 = ?, termine = ? WHERE id_match = ?";

        try (PreparedStatement ps = statement.getConnection().prepareStatement(query)) {
            // Set the values dynamically using the PreparedStatement
            ps.setInt(1, match.eq1);      // Set equipe1
            ps.setInt(2, match.eq2);      // Set equipe2
            ps.setInt(3, match.score1);   // Set score1
            ps.setInt(4, match.score2);   // Set score2
            ps.setString(5, termine);     // Set termine ("oui" or "non")
            ps.setInt(6, match.idmatch);  // Set the match ID

            // Execute the update
            ps.executeUpdate();
        } catch (SQLException e) {
            // Improved error handling with message context
            System.out.println("Erreur lors de la mise à jour du match (ID: " + match.idmatch + "): " + e.getMessage());
            e.printStackTrace();
        }

    }
    public void supprimerEquipe(int idEquipe,Tournoi tournoi) {
        // Query to get the current num_equipe of the team to delete
        String queryGetNumEquipe = "SELECT num_equipe FROM equipes WHERE id_equipe = ?";
        String queryDeleteEquipe = "DELETE FROM equipes WHERE id_tournoi = ? AND id_equipe = ?";
        String queryUpdateEquipes = "UPDATE equipes SET num_equipe = num_equipe - 1 WHERE id_tournoi = ? AND num_equipe > ?";

        try (PreparedStatement psGetNumEquipe = statement.getConnection().prepareStatement(queryGetNumEquipe);
             PreparedStatement psDeleteEquipe = statement.getConnection().prepareStatement(queryDeleteEquipe);
             PreparedStatement psUpdateEquipes = statement.getConnection().prepareStatement(queryUpdateEquipes)) {

            // Set parameters for the SELECT query to get the num_equipe
            psGetNumEquipe.setInt(1, idEquipe);
            ResultSet rs = psGetNumEquipe.executeQuery();

            if (rs.next()) {
                int numeq = rs.getInt(1);  // Get the current num_equipe
                rs.close();

                // Delete the team
                psDeleteEquipe.setInt(1, tournoi.getId());
                psDeleteEquipe.setInt(2, idEquipe);
                psDeleteEquipe.executeUpdate();

                // Update other teams' num_equipe values
                psUpdateEquipes.setInt(1, tournoi.getId());
                psUpdateEquipes.setInt(2, numeq);
                psUpdateEquipes.executeUpdate();

                // Refresh data after the update
                tournoi.updateData();
            } else {
                System.out.println("Equipe non trouvée (ID: " + idEquipe + ")");
            }

        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression de l'équipe (ID: " + idEquipe + "): " + e.getMessage());
            e.printStackTrace();
        }
    }


    // Basic escaping method for strings to avoid SQL injection (use carefully)
    public static String mysql_real_escape_string( String str) throws Exception {
        if (str == null) {
            return null;
        }

        if (str.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "").isEmpty()) {
            return str;
        }

        String clean_string = str;
        clean_string = clean_string.replaceAll("\\n","\\\\n");
        clean_string = clean_string.replaceAll("\\r", "\\\\r");
        clean_string = clean_string.replaceAll("\\t", "\\\\t");
        clean_string = clean_string.replaceAll("\\00", "\\\\0");
        clean_string = clean_string.replaceAll("'", "''");
        return clean_string;

    }
}
