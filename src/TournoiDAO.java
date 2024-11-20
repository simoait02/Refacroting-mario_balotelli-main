import java.sql.*;

public class TournoiDAO {
    private final Statement statement;
    Tournoi tournoi =new Tournoi();
    public TournoiDAO(Statement statement) {
        this.statement = statement;
    }

    public Tournoi getTournoiByName(String nomTournoi) {

        try {
            ResultSet rs = statement.executeQuery("SELECT * FROM tournois WHERE nom_tournoi = '" + mysql_real_escape_string(nomTournoi) + "';");
            if(!rs.next()){
                return null;
            }
            tournoi.setStatut( rs.getInt("statut"));
            tournoi.setIdTournoi(rs.getInt("id_tournoi"));
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tournoi;
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
