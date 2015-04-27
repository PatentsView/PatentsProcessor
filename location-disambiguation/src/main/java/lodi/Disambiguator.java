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

        RawGoogle goog = new RawGoogle(conn, googleConfidenceThreshold);
        System.out.format(
                "Constructed list of valid Google input addresses (%d items)\n",
                goog.size());

        ConcurrentMap<Boolean, List<RawLocation.Record>> splitLocations =
            rawLocations
            .parallelStream()
            .collect(Collectors.groupingByConcurrent(loc -> goog.containsKey(loc.cleanedLocation)));

        List<RawLocation.Record> identifiedLocations = splitLocations.get(true);
        List<RawLocation.Record> unidentifiedLocations = splitLocations.get(false);

        // should be able to do this in the initial group-by, but the implemenation is ugly
        // and I'm not sure it's enough of a performance gain to be worth it.

        identifiedLocations.parallelStream()
            .forEach(loc -> loc.linkedCity = goog.get(loc.cleanedLocation));

        int identifiedCount = (identifiedLocations == null) ? 0 : identifiedLocations.size();
        System.out.println("Count of identified locations: " + identifiedCount);

        // first handle unmatched locations
        
        ConcurrentMap<String, List<RawLocation.Record>> unidentifiedGroupedLocations =
            unidentifiedLocations
            .parallelStream()
            .collect(Collectors.groupingByConcurrent(loc -> loc.cleanedCountry));

        for  (Map.Entry<String, List<RawLocation.Record>> entry: unidentifiedGroupedLocations.entrySet()) {
            List<RawLocation.Record> rawCities = entry.getValue();
            List<City> cities = AllCities.load(conn, entry.getKey());

            System.out.println(
                    String.format("Missing for: %s (%d locations, %d known cities)", 
                                  entry.getKey(), rawCities.size(), cities.size()));

            // for each unmatched location in this country, find the best matching city

            rawCities.parallelStream()
                .forEach(loc -> {
                    CityScore cscore = bestScore(loc, cities);

                    if (cscore.score > matchThreshold && cscore.city.location != loc.country)
                        loc.linkedCity = cscore.city;
                });
        }

        ConcurrentMap<Boolean, List<RawLocation.Record>> splitLocations2 =
            rawLocations
            .parallelStream()
            .collect(Collectors.groupingByConcurrent(loc -> loc.city != null));

        List<RawLocation.Record> identifiedLocations2 = splitLocations2.get(true);
        // List<RawLocation.Record> unidentifiedLocations2 = splitLocations2.get(false);

        int identifiedCount2 = (identifiedLocations2 == null) ? 0 : identifiedLocations2.size();
        System.out.println("Count of identified locations (2nd pass): " + identifiedCount2);
    }

    protected static class CityScore {
        public final City city;
        public final double score;

        public CityScore(City city, double score) {
            this.city = city;
            this.score = score;
        }

        public static CityScore max(CityScore a, CityScore b) {
            return (a.score >= b.score) ? a : b;
        }
    }

    protected static CityScore score(RawLocation.Record raw, City city) {
        double score = StringUtils.getJaroWinklerDistance(raw.cleanedLocation, city.location);
        return new CityScore(city, score);
    }

    protected static CityScore bestScore(RawLocation.Record raw, List<City> cities) {
        Optional<CityScore> maxScore = cities.stream()
            .map(c -> score(raw, c))
            .reduce(CityScore::max);

        if (maxScore.isPresent())
            return maxScore.get();
        else
            return new CityScore(null, -1);
    }

    // protected Scored<City> bestMatch(RawLocation.Record raw, List<AllCities.Record> cities) {
    // }
}
