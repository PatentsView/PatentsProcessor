package lodi;

import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;

public class Disambiguator {

    public static class RawParsedLocation {
        public final String city;
        public final String state;
        public final String country;

        public RawParsedLocation(final String city, final String state, final String country) {
            this.city = city;
            this.state = city;
            this.country = city;
        }
    }

    public static HashSet<String> validInputAddresses(final Connection conn) 
        throws SQLException
    {
        HashSet<String> set = new HashSet<>();
        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery(
                "select input_address " +
                "from raw_google " +
                "where confidence > 0.1 " +
                "and (city <> '' or region <> '')");

        while (rs.next())
            set.add(rs.getString("input_address"));

        return set;
    }

    public static LinkedList<RawParsedLocation> rawParsedLocations(
            final Connection conn,
            int limit,
            int offset,
            double minimum_match_value) 
        throws SQLException
    {
        LinkedList<RawParsedLocation> list = new LinkedList<>();
        Statement stmt = conn.createStatement();
        
        ResultSet rs = stmt.executeQuery("select city, state, country from rawlocation");

        while (rs.next()) {
            String city = rs.getString("city");
            String state = rs.getString("state");
            String country = rs.getString("country");
            list.add(new RawParsedLocation(city, state, country));
        }

        return list;
    }

}
