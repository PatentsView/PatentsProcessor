package lodi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

public class AllCities {

    /**
     * Return list of all cities in the given country.
     */
    public static LinkedList<City> load(Connection conn, String country) 
        throws SQLException
    {
        LinkedList<City> list = new LinkedList<>();

        PreparedStatement pstmt = conn.prepareStatement(
                "select city, region, country, latitude, longitude " +
                "from all_cities " +
                "where country = ?");
        pstmt.setString(1, country);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            String city = rs.getString(1);
            String region = rs.getString(2);
            // String country = rs.getString(3);
            double latitude = rs.getDouble(4);
            double longitude = rs.getDouble(5);

            City rec = new City(city, region, country, latitude, longitude);
            list.add(rec);
        }

        return list;
    }
}
