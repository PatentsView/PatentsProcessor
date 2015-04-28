package lodi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import com.mysql.jdbc.Statement;

public class Cities {

    public static class Record {
        public final int id;
        public final String city;
        public final String region;
        public final String country;
        public final double latitude;
        public final double longitude;
        public final String stringValue;

        public Record(int id, String city, String region, String country,
                      double latitude, double longitude, String stringValue) 
        {
            this.id = id;
            this.city = city;
            this.region = region;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
            this.stringValue = stringValue;
        }
    }

    /**
     * Load cities table into memory.
     */
    public Cities(Connection conn) 
        throws SQLException
    {
        countryMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        idMap = new TreeMap<>();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "select id, city, region, country, latitude, longitude , string_value " +
                "from all_cities " +
                "where city <> '' or region <> ''");

        while (rs.next()) {
            int id = rs.getInt(1);
            String city = rs.getString(2);
            String region = rs.getString(3);
            String country = rs.getString(4);
            double latitude = rs.getDouble(5);
            double longitude = rs.getDouble(6);
            String stringValue = rs.getString(7);

            Record rec = new Record(id, city, region, country, latitude, longitude, stringValue);

            idMap.put(id, rec);

            if (countryMap.containsKey(country)) {
                countryMap.get(country).add(rec);
            }
            else {
                LinkedList<Record> list = new LinkedList<>();
                list.add(rec);
                countryMap.put(country, list);
            }
        }
    }
    
    public Record get(int id) {
        return idMap.get(id);
    }

    public List<Record> getCountry(String country) {
        return countryMap.get(country);
    }

    public int size() {
        return idMap.size();
    }

    private final TreeMap<String, LinkedList<Record>> countryMap;
    private final TreeMap<Integer, Record> idMap;
}
