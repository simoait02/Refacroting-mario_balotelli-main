import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import javax.swing.*;

public class Belote {

	// Define the Match class


	// Main method to start the application
	public static void main(String[] args) {
		// Connection and database statement setup
		Connection connection = null;
		Statement statement = null;

		try {
			// Connect to HSQLDB
			Class.forName("org.hsqldb.jdbcDriver").newInstance();

			String dos = System.getenv("APPDATA") + "\\jBelote";
			System.out.println("Dossier de stockage:" + dos);
			if (!new File(dos).isDirectory()) {
				new File(dos).mkdir();
			}

			connection = DriverManager.getConnection("jdbc:hsqldb:file:" + dos + "\\belote", "sa", "");
			statement = connection.createStatement();

			// Import SQL setup for the database
			importSQL(connection, new File("create.sql"));

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Impossible de se connecter à la base de données. Vérifier qu'une autre instance du logiciel n'est pas déjà ouverte.");
			System.out.println(e.getMessage());
			System.exit(0);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Erreur lors de l'initialisation du logiciel. Vérifiez votre installation Java et vos droits d'accès sur le dossier AppData.");
			System.out.println(e.getMessage());
			System.exit(0);
		}

		// Launch the GUI
		Statement finalStatement = statement;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Create the Fenetre object (your main window)
				Fenetre f = new Fenetre(finalStatement);
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				f.setVisible(true); // Ensure the frame is visible
			}
		});
	}

	// Method to import SQL script
	public static void importSQL(Connection conn, File in) throws SQLException, FileNotFoundException {
		Scanner s = new Scanner(in);
		s.useDelimiter("(;(\r)?\n)|(--\n)");
		Statement st = null;
		try {
			st = conn.createStatement();
			while (s.hasNext()) {
				String line = s.next();
				if (line.startsWith("/*!") && line.endsWith("*/")) {
					int i = line.indexOf(' ');
					line = line.substring(i + 1, line.length() - " */".length());
				}

				if (line.trim().length() > 0) {
					// Execute SQL commands
					st.execute(line);
				}
			}
		} finally {
			if (st != null) st.close();
		}
	}
}