package lodi;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Disambiguator {

    public static void disambiguate(List<RawLocation.Record> rawLocations, RawGoogle goog) {

        ConcurrentMap<Boolean, List<RawLocation.Record>> splitLocations =
            rawLocations
            .parallelStream()
            .collect(Collectors.groupingByConcurrent(loc -> goog.containsKey(loc.cleanedLocation)));

        List<RawLocation.Record> identifiedLocations = splitLocations.get(true);
        List<RawLocation.Record> unidentifiedLocations = splitLocations.get(false);

        int identifiedCount = (identifiedLocations == null) ? 0 : identifiedLocations.size();
        System.out.println("Count of identified locations: " + identifiedCount);

        // first handle unmatched locations
        
        ConcurrentMap<String, List<RawLocation.Record>> unidentifiedGroupedLocations =
            unidentifiedLocations
            .parallelStream()
            .collect(Collectors.groupingByConcurrent(loc -> loc.cleanedCountry));
    }
}
