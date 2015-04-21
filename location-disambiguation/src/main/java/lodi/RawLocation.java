package lodi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class RawLocation {

    public final static Pattern patEOL = Pattern.compile("[\r\n]");
    public final static Pattern patSeparator = Pattern.compile("\\|", Pattern.UNICODE_CHARACTER_CLASS);

    public final static PatternReplacements manualReplacements = initManualReplacements();

    public static String concatenateLocation(final String city, final String country, final String state) {
        StringJoiner j = new StringJoiner(", ");

        if (city != null && !city.isEmpty())
            j.add(city);

        if (state != null && !state.isEmpty())
            j.add(state);

        if (country != null && !country.isEmpty())
            j.add(country);

        return j.toString();
    }

    public static String cleanRawLocation(String text) {
        text = patEOL.matcher(text).replaceAll("");
        text = patSeparator.matcher(text).replaceAll(", ");
        return text;
    }

    private static PatternReplacements initManualReplacements() {
        return initPatternReplacements("manual_replacements_library.txt");
    }

    private static PatternReplacements initPatternReplacements(String resource) {
        URL url = RawLocation.class.getResource(resource);

        PatternReplacements mr = null;  

        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream())))
        {
            mr = PatternReplacements.loadFromFile(in);
        }
        catch(java.io.IOException e) {
            System.out.println("There was a problem reading '" + resource + "'");
        }

        return mr;
    }
    
    // fields taken from the database

    public final String city;
    public final String state;
    public final String country;

    // the cleaned field used for matching

    public final String cleanedLocation;

    public RawLocation(final String city, final String state, final String country) {
        this.city = city;
        this.state = city;
        this.country = city;

        String s = concatenateLocation(city, state, country);
        s = cleanRawLocation(s);

        this.cleanedLocation = s;
    }

}
