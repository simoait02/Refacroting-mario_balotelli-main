import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private Statement statement;

    private DatabaseManager() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        initializeConnection();
    }

    public static DatabaseManager getInstance() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    private void initializeConnection() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        Properties properties = new Properties();

        // Load properties from the classpath
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("belote.properties")) {
            if (input == null) {
                throw new IOException("Unable to find belote.properties");
            }
            properties.load(input);
        }

        // Load JDBC driver
        Class.forName("org.hsqldb.jdbcDriver").newInstance();

        // Get database connection details from properties
        String dbUrl = properties.getProperty("db.url");
        String dbUsername = properties.getProperty("db.username");
        String dbPassword = properties.getProperty("db.password");

        connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
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
