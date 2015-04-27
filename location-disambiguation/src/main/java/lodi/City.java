package lodi;

import java.util.regex.Pattern;

/**
 * A struct to represent a record from the raw_google or all_cities database.
 */
public class City {

    public final String city;
    public final String region;
    public final String country;
    public final double latitude;
    public final double longitude;
    public final String groupingID;
    public final String location;

    protected static Pattern patCapital = Pattern.compile("[A-Z]");

    public City(String city, String region, String country,
                double latitude, double longitude) 
    {
        this.city = city;
        this.region = region;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.groupingID = String.format("%s|%s", latitude, longitude);

        String state = null;
        if (patCapital.matcher(region).matches())
            state = region;

        this.location = RawLocation.concatenateLocation(city, state, country);
    }


}
