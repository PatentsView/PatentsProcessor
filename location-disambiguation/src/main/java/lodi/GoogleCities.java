package lodi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

/**
 * This class loads the entire raw_google database into memory for efficient lookups.
 */
public class GoogleCities {

    public static class Record {
        public final String inputString;
        public final double confidence;
        public final Cities.Record city;

        public Record(String inputString, double confidence, Cities.Record city)
        {
            this.inputString = inputString;
            this.confidence = confidence;
            this.city = city;
        }
    }

    public GoogleCities(Connection conn, double confidenceThreshold, Cities cities)
        throws SQLException
    {
        map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        PreparedStatement pstmt = conn.prepareStatement(
                "select input_string, city_id, confidence " +
                "from google_cities " +
                "join cities on cities.id = city_id " +
                "where confidence > ? " +
                "and (city <> '' or region <> '')");

        pstmt.setDouble(1, confidenceThreshold);
        ResultSet rs = pstmt.executeQuery();
                 
        while (rs.next()) {
            String inputString = rs.getString(1);
            int cityId = rs.getInt(2);
            double confidence = rs.getDouble(3);

            Cities.Record city = cities.get(cityId);

            Record rec = new Record(inputString, confidence, city);
            map.put(rec.inputString, rec);
        }
    }

    public boolean containsKey(String cleanedLocation) {
        return map.containsKey(cleanedLocation);
    }

    public Record get(String cleanedLocation) {
        return map.get(cleanedLocation);
    }

    public int size() {
        return map.size();
    }

    private final TreeMap<String, Record> map;
}
