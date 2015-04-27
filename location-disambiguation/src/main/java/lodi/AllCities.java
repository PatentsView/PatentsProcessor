package lodi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

public class AllCities {

    public static class Record {
        public final String city;
        public final String region;
        public final String country;
        public final double latitude;
        public final double longitude;
        public final String groupingID;

        public Record(String city, String region, String country, 
                      double latitude, double longitude)
        {
            this.city = city;
            this.region = region;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
            this.groupingID = String.format("%s|%s", latitude, longitude);
        }
    }

    /**
     * Return list of all cities in the given country.
     */
    public static LinkedList<Record> load(Connection conn, String country) 
        throws SQLException
    {
        LinkedList<Record> list = new LinkedList<>();

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

            Record rec = new Record(city, region, country, latitude, longitude);
            list.add(rec);
        }

        return list;
    }
}
