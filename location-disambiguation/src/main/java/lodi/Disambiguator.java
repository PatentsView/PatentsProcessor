package lodi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class Disambiguator {

    /**
     * Disambiguate the list of raw locations.
     *
     * @param conn A connection to the geolocations database.
     * @param rawLocations A list of raw location records to disambiguate
     * @param googleConfidenceThreshold The confidence threshold to use when loading the google table
     * @param matchThreshold The required match level when doing fuzzy-matching using Jaro-Winkler.
     */
    public static void disambiguate(
            Connection conn, 
            List<RawLocation.Record> rawLocations,
            double googleConfidenceThreshold,
            double matchThreshold) 
        throws SQLException
    {

        System.out.print("Loading cities table... ");
        Cities cities = new Cities(conn);
        System.out.format("got %d records\n", cities.size());

        System.out.print("Loading google_cities table... ");
        GoogleCities goog = new GoogleCities(conn, googleConfidenceThreshold, cities);
        System.out.format("got %d records\n", goog.size());

        ConcurrentMap<Boolean, List<RawLocation.Record>> splitLocations =
            rawLocations
            .parallelStream()
            .collect(Collectors.groupingByConcurrent(loc -> goog.containsKey(loc.cleanedLocation)));

        List<RawLocation.Record> identifiedLocations = splitLocations.get(true);
        List<RawLocation.Record> unidentifiedLocations = splitLocations.get(false);

        // should be able to do this in the initial group-by, but the implemenation is ugly
        // and I'm not sure it's enough of a performance gain to be worth it.

        identifiedLocations.parallelStream()
            .forEach(loc -> loc.linkedCity = goog.get(loc.cleanedLocation).city);

        int identifiedCount = (identifiedLocations == null) ? 0 : identifiedLocations.size();
        System.out.println("Count of identified locations: " + identifiedCount);

        // handle unmatched locations
        
        // group unidentified locations by country

        ConcurrentMap<String, List<RawLocation.Record>> unidentifiedGroupedLocations =
            unidentifiedLocations
            .parallelStream()
            .collect(Collectors.groupingByConcurrent(Disambiguator::countryGroup));

        for  (final Map.Entry<String, List<RawLocation.Record>> entry: unidentifiedGroupedLocations.entrySet()) {

            List<RawLocation.Record> countryLocations = entry.getValue();

            // group locations by unique cleanedLocation

            Map<String, List<RawLocation.Record>> rawCities =
                countryLocations
                .stream()
                .collect(Collectors.groupingBy(loc -> loc.cleanedLocation));

            // get database cities from this country

            String country = entry.getKey();
            List<Cities.Record> cityList;

            if (country.startsWith("::")) {
                // this "country" encodes a US state (see #countryGroup)
                String state = country.substring(2);
                cityList = cities.getState(state);
            } else {
                cityList = cities.getCountry(country);
            }

            int cityCount = (cityList == null) ? 0 : cityList.size();

            System.out.println(
                    String.format("Missing for: %s (%d locations, %d unique, %d known cities)", 
                                  country, countryLocations.size(), rawCities.size(), cityCount));

            // for each unmatched location in this country, find the best matching city

            rawCities.keySet()
                .parallelStream()
                .forEach(rawString -> {
                    CityScore cscore = bestScore(rawString, cityList);

                    if (cscore.score > matchThreshold && cscore.city.stringValue != country)
                        rawCities.get(rawString).stream().forEach(loc -> loc.linkedCity = cscore.city);
                });
        }

        List<RawLocation.Record> finalLinked =
            rawLocations
            .parallelStream()
            .filter(loc -> loc.linkedCity != null)
            .collect(Collectors.toList());

        System.out.println("Count of identified locations (2nd pass): " + finalLinked.size());
    }

    /**
     * Create a special "country" code for US states. This is used when splitting up the
     * raw locations by country. This is a little hacky but works.
     */
    protected static String countryGroup(RawLocation.Record loc) {
        if (loc.cleanedCountry.equalsIgnoreCase("US")) {
            return String.format("::%s", loc.state);
        }
        else {
            return loc.cleanedCountry;
        }
    }

    protected static class CityScore {
        public final Cities.Record city;
        public final double score;

        public CityScore(Cities.Record city, double score) {
            this.city = city;
            this.score = score;
        }

        public static CityScore max(CityScore a, CityScore b) {
            return (a.score >= b.score) ? a : b;
        }
    }

    protected static CityScore score(String rawString, Cities.Record city) {
        double score = StringUtils.getJaroWinklerDistance(rawString, city.stringValue);
        return new CityScore(city, score);
    }

    protected static CityScore bestScore(String rawString, List<Cities.Record> cities) {
        if (cities == null)
            return new CityScore(null, -1);

        Optional<CityScore> maxScore = cities.stream()
            .map(c -> score(rawString, c))
            .reduce(CityScore::max);

        if (maxScore.isPresent())
            return maxScore.get();
        else
            return new CityScore(null, -1);
    }

}