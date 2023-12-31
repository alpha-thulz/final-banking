package za.co.tyaphile.info;

public class Info {
    private static String DATABASE_NAME = "finance_db";
    private static String ROOT;
    private static String PASSWORD;

    public static String getDatabaseName(boolean isMySQL) {
        if (isMySQL) {
            DATABASE_NAME = "finance_db";
        } else {
            DATABASE_NAME = "finance.db";
        }
        return DATABASE_NAME;
    }

    public static String getROOT() {
        return ROOT;
    }

    public static void setROOT(String ROOT) {
        Info.ROOT = ROOT;
    }

    public static String getPASSWORD() {
        return PASSWORD;
    }

    public static void setPASSWORD(String PASSWORD) {
        Info.PASSWORD = PASSWORD;
    }
}
