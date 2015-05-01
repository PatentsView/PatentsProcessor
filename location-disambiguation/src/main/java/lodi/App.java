package lodi;

import lodi.Disambiguator;

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
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
        throws ClassNotFoundException, java.sql.SQLException
    {
        if (args.length < 1) {
            System.out.println("Usage: java -jar lodi.jar CONFIG_FILE");
            System.exit(1);
        }

        Properties config = new Properties();
        try {
            config.load(new FileReader(args[0]));
        } catch (IOException e) {
            System.out.println("There was a problem loading the configuration file '" + args[0] + "'");
            System.out.println(e);
        }

        Class.forName("com.mysql.jdbc.Driver");
        Class.forName("org.sqlite.JDBC");

        String locationDatabase = config.getProperty("location.database");
        String locationPath = config.getProperty("location.path");
        String connectionString = String.format("jdbc:sqlite:%s/%s", locationPath, locationDatabase);
        System.out.println("Connecting to geolocation database: " + connectionString);
        Connection conn = DriverManager.getConnection(connectionString);
        conn.setAutoCommit(false);

        double confidenceThreshold = 
            Double.parseDouble(config.getProperty("location.raw_google.confidence_threshold"));

        double matchThreshold = Double.parseDouble(config.getProperty("location.match_threshold"));
        
        String host = config.getProperty("mysql.host", "localhost");
        String port = config.getProperty("mysql.port", "3306");
        String database = config.getProperty("mysql.grant.database");
        String url = String.format("jdbc:mysql://%s:%s/%s", host, port, database);
        String user = config.getProperty("mysql.user");
        String password = config.getProperty("mysql.password");

        DriverManager.setLoginTimeout(10);
        Connection pdb = DriverManager.getConnection(url, user, password);

        int n = 100000;
        System.out.format("Requesting %d records... ", n);
        List<RawLocation.Record> rawLocations = RawLocation.load(pdb, n, 0);
        System.out.format("(got %d)\n", rawLocations.size());

        StopWatch watch = new StopWatch();
        watch.start();

        Disambiguator.disambiguate(conn, rawLocations, confidenceThreshold, matchThreshold);
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
              "city, state, country, cleaned_location, cleaned_country, city_id) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

                if (loc.linkedCity != null)
                    pstmt.setInt(11, loc.linkedCity.id);
                else
                    pstmt.setNull(11, java.sql.Types.INTEGER);

                pstmt.execute();
            }

            geodb.commit();
        }
    }
}
