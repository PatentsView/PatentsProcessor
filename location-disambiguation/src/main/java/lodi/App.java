package lodi;

import lodi.Disambiguator;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Properties;

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
        System.out.println(String.format("jdbc:sqlite:%s", locationDatabase));
        Connection conn = DriverManager.getConnection(
                String.format("jdbc:sqlite:%s/%s", locationPath, locationDatabase));

        HashSet<String> validInputAddresses = Disambiguator.validInputAddresses(conn);
        System.out.format(
                "Constructed list of valid Google input addresses (%d items)\n",
                validInputAddresses.size());

        System.exit(0);
    }
}
