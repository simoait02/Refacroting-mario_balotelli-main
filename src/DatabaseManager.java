import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private Statement statement;

    private DatabaseManager() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        initializeConnection();
    }

    public static DatabaseManager getInstance() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }


    private void initializeConnection() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class.forName("org.hsqldb.jdbcDriver").newInstance();

        String storagePath = System.getenv("APPDATA") + "\\jBelote";
        File storageDir = new File(storagePath);
        if (!storageDir.isDirectory()) {
            storageDir.mkdir();
        }

        System.out.println("Storage Directory: " + storagePath);

        connection = DriverManager.getConnection("jdbc:hsqldb:file:" + storagePath + "\\belote", "sa", "");
        statement = connection.createStatement();
    }

    public void setupDatabase(String sqlFilePath) throws SQLException, FileNotFoundException {
        importSQL(new File(sqlFilePath));
    }

    private void importSQL(File sqlFile) throws SQLException, FileNotFoundException {
        try (Scanner scanner = new Scanner(sqlFile)) {
            scanner.useDelimiter("(;(\r)?\n)|(--\n)");
            try (Statement statement = connection.createStatement()) {
                while (scanner.hasNext()) {
                    String sqlCommand = scanner.next().trim();

                    if (sqlCommand.startsWith("/*!") && sqlCommand.endsWith("*/")) {
                        int index = sqlCommand.indexOf(' ');
                        sqlCommand = sqlCommand.substring(index + 1, sqlCommand.length() - " */".length());
                    }

                    if (!sqlCommand.isEmpty()) {
                        statement.execute(sqlCommand);
                    }
                }
            }
        }
    }

    public Statement getStatement() {
        return statement;
    }
}
