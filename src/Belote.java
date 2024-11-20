
import javax.swing.*;

public class Belote {

	public static void main(String[] args) {
		try {
			DatabaseManager dbManager = DatabaseManager.getInstance();
			dbManager.setupDatabase("create.sql");

			SwingUtilities.invokeLater(() -> {
				Fenetre mainWindow = new Fenetre(dbManager.getStatement());
				mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				mainWindow.setVisible(true);
			});
		} catch (Exception e) {
			displayError(e);
		}
	}

	private static void displayError(Exception exception) {
		JOptionPane.showMessageDialog(null, "An error occurred while starting the application.");
		System.err.println(exception.getMessage());
		System.exit(0);
	}
}
