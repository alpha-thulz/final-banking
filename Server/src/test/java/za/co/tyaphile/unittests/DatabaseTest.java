package za.co.tyaphile.unittests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.co.tyaphile.database.DatabaseManager;

import java.io.File;
import java.util.Map;

public class DatabaseTest {
    private static DatabaseManager dbm = new DatabaseManager();
    @BeforeAll
    static void showFiles() {
        DatabaseManager.createTables();
    }

    @AfterAll
    static void cleanUp() {
        dbm = null;
        DatabaseManager.closeConnection();
        File file = new File("finance_db");
        System.out.println("Clean up successful: " + file.delete());
    }
}