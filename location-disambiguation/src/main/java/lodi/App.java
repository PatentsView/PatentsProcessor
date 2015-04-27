package lodi;

import lodi.Disambiguator;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
        Connection conn = DriverManager.getConnection(
                String.format("jdbc:sqlite:%s/%s", locationPath, locationDatabase));

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

        List<RawLocation.Record> rawLocations = RawLocation.load(pdb, 1000000, 0);

        StopWatch watch = new StopWatch();
        watch.start();

        Disambiguator.disambiguate(conn, rawLocations, confidenceThreshold, matchThreshold);
        watch.stop();
        System.out.println("Elapsed time: " + watch);

        System.exit(0);
    }
}
