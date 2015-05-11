package lodi;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.time.StopWatch;

/**
 * The disambiguation application. 
 */
public class App 
{

    public static class Configuration {
        public String locationDatabase;
        public String patentDatabase;
        public String patentDatabaseUser;
        public String patentDatabasePass;

        double googleConfidenceThreshold;
        double fuzzyMatchThreshold;

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("{\n");
            b.append("  locationDatabase = ").append(locationDatabase).append("\n");
            b.append("  patentDatabase = ").append(patentDatabase).append("\n");
            b.append("  patentDatabaseUser = ").append(patentDatabaseUser).append("\n");
            b.append("  patentDatabasePass = ").append(patentDatabasePass).append("\n");
            b.append("  googleConfidenceThreshold = ").append(googleConfidenceThreshold).append("\n");
            b.append("  fuzzyMatchThreshold = ").append(fuzzyMatchThreshold).append("\n");
            b.append("}");

            return b.toString();
        }
    }

    public static Configuration loadConfiguration(String file) {
        Properties prop = new Properties();
        try {
            prop.load(new FileReader(file));
        }
        catch(IOException e) {
            System.out.println("There was a problem loading the configuration file '" + file + "'");
            System.out.println(e);
            System.exit(1);
        }

        Configuration config = new Configuration();

        // build connection string for geolocation database

        String locationDatabase = prop.getProperty("location.database");
        String locationPath = prop.getProperty("location.path");
        String connectionString = String.format("jdbc:sqlite:%s/%s", locationPath, locationDatabase);
        config.locationDatabase = connectionString;

        // get threshold values

        config.googleConfidenceThreshold =
            Double.parseDouble(prop.getProperty("location.raw_google.confidence_threshold"));

        config.fuzzyMatchThreshold = Double.parseDouble(prop.getProperty("location.match_threshold"));

        // build connection string for patent database

        String host = prop.getProperty("mysql.host", "localhost");
        String port = prop.getProperty("mysql.port", "3306");
        String database = prop.getProperty("mysql.grant.database");
        config.patentDatabase = String.format("jdbc:mysql://%s:%s/%s", host, port, database);
        config.patentDatabaseUser = prop.getProperty("mysql.user");
        config.patentDatabasePass = prop.getProperty("mysql.password");

        return config;
    }

    public static void main( String[] args )
        throws ClassNotFoundException, java.sql.SQLException
    {
        if (args.length < 1) {
            System.out.println("Usage: java -jar lodi.jar CONFIG_FILE");
            System.exit(1);
        }

        Configuration config = loadConfiguration(args[0]);
        System.out.println("Using configuration:");
        System.out.println(config);

        Class.forName("com.mysql.jdbc.Driver");
        Class.forName("org.sqlite.JDBC");

        Connection conn = DriverManager.getConnection(config.locationDatabase);
        conn.setAutoCommit(false);

        DriverManager.setLoginTimeout(10);
        Connection pdb = DriverManager.getConnection(
                config.patentDatabase, config.patentDatabaseUser, config.patentDatabasePass);

        int n = 100000;
        System.out.format("Requesting %d records... ", n);
        List<RawLocation.Record> rawLocations = RawLocation.load(pdb, n, 0);
        System.out.format("(got %d)\n", rawLocations.size());

        StopWatch watch = new StopWatch();
        watch.start();

        Disambiguator.disambiguate(
                conn, 
                rawLocations, 
                config.googleConfidenceThreshold, 
                config.fuzzyMatchThreshold);

        watch.stop();
        System.out.println("Elapsed time: " + watch);

        System.out.print("Saving results to database... ");
        prepareResultTable(conn);
        saveResults(conn, rawLocations);
        System.out.println("DONE");

        System.exit(0);
    }

    public static void prepareResultTable(Connection geodb) 
        throws java.sql.SQLException
    {
        try (Statement stmt = geodb.createStatement()) {
            stmt.execute("drop table if exists coded_locations");
            stmt.execute(
                    "create table coded_locations (" +
                    "location_id text not null, " +
                    "inventor_id text, " +
                    "raw_city text, " +
                    "raw_state text, " +
                    "raw_country text, " +
                    "city text, " +
                    "state text, " +
                    "country text, " +
                    "cleaned_location text, " +
                    "cleaned_country text, " +
                    "link_code integer, " +
                    "city_id integer)");

            stmt.execute("create index coded_city_ix on coded_locations (city_id)");
            geodb.commit();
        }
    }

    public static void saveResults(Connection geodb, List<RawLocation.Record> rawLocations)
        throws java.sql.SQLException
    {
        String sql =
            "insert into coded_locations " +
            "(location_id, inventor_id, raw_city, raw_state, raw_country, " +
              "city, state, country, cleaned_location, cleaned_country, link_code, city_id) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = geodb.prepareStatement(sql)) {
            for (RawLocation.Record loc: rawLocations) {
                pstmt.setString(1, loc.locationId);
                pstmt.setString(2, loc.inventorId);
                pstmt.setString(3, loc.rawCity);
                pstmt.setString(4, loc.rawState);
                pstmt.setString(5, loc.rawCountry);
                pstmt.setString(6, loc.city);
                pstmt.setString(7, loc.state);
                pstmt.setString(8, loc.country);
                pstmt.setString(9, loc.cleanedLocation);
                pstmt.setString(10, loc.cleanedCountry);
                pstmt.setInt(11, loc.linkCode);

                if (loc.linkedCity != null)
                    pstmt.setInt(12, loc.linkedCity.id);
                else
                    pstmt.setNull(12, java.sql.Types.INTEGER);

                pstmt.execute();
            }

            geodb.commit();
        }
    }
}
