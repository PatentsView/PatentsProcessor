package lodi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

import org.jsoup.Jsoup;

/**
 * A raw location that needs to be disambiguated.
 */
public class RawLocation {

    /**
     * A RawLocation record.
     */
    public static class Record {
        // fields taken from the database

        public final String city;
        public final String state;
        public final String country;

        // the cleaned field used for matching

        public final String cleanedLocation;
        public final String cleanedCountry;

        public Record(final String city, final String state, final String country) {
            this.city = city;
            this.state = city;
            this.country = city;

            String s = concatenateLocation(city, state, country);
            s = cleanRawLocation(s);

            this.cleanedLocation = s;

            if (country != null)
                this.cleanedCountry = country;
            else if (state != null)
                this.cleanedCountry = state;
            else
                this.cleanedCountry = city;
        }
    }

    /**
     * Return a list of {@link RawLocation} objects from the database.
     *
     * @todo Check the query for correctness
     *
     * @param conn A database connection where the `rawlocation` table is found
     * @param limit The number of records to return
     * @param offset The offset into the database table to use
     * @return A list of {@link RawLocation} objects from the database
     */
    public static LinkedList<Record> load(
            final Connection conn,
            int limit,
            int offset) 
        throws SQLException
    {
        LinkedList<Record> list = new LinkedList<>();
        
        PreparedStatement pstmt = conn.prepareStatement(
                "select city, state, country_transformed from rawlocation " +
                "where coalesce(city, state, country_transformed, '') <> '' " +
                "limit ?, ?");
        pstmt.setInt(1, offset);
        pstmt.setInt(2, limit);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            String city = rs.getString("city");
            String state = rs.getString("state");
            String country = rs.getString("country_transformed");
            list.add(new Record(city, state, country));
        }

        return list;
    }

    /**
     * Return the canonical String for this locale. The result is used in fuzzy string
     * matching. The arguments to this function may be null or empty strings, in which
     * case they do not appear in the final concatenated string.
     *
     * @param city Locale city
     * @param state Locale state
     * @param country Locale country
     * @return The concatenated representation of this locale
     */
    public static String concatenateLocation(final String city, final String state, final String country) {
        StringJoiner j = new StringJoiner(", ");

        if (city != null && !city.isEmpty())
            j.add(city);

        if (state != null && !state.isEmpty())
            j.add(state);

        if (country != null && !country.isEmpty())
            j.add(country);

        return j.toString();
    }

    /**
     * Apply various filters to clean the text of this raw location to make it suitable
     * for matching.
     *
     * @param text The raw location text
     * @return The cleaned location text
     */
    public static String cleanRawLocation(String text) {
        text = eolPattern.matcher(text).replaceAll("");
        text = separatorPattern.matcher(text).replaceAll(", ");

        text = manualReplacements.apply(text);
        text = quickFix(text);

        // remove XML/HTML tags
        text = Jsoup.parseBodyFragment(text).text();

        // normalize unicode
        text = Normalizer.normalize(text, Normalizer.Form.NFC);

        text = removePatterns.apply(text); 
        text = countryPatterns.apply(text);

        return text;
    }

    protected final static Pattern eolPattern = Pattern.compile("[\r\n]");
    protected final static Pattern separatorPattern = Pattern.compile("\\|", Pattern.UNICODE_CHARACTER_CLASS);
    protected final static Pattern curlyPattern = Pattern.compile("\\{.*\\((.*)\\).*\\}", Pattern.UNICODE_CHARACTER_CLASS);

    protected final static PatternReplacements manualReplacements = initManualReplacements();
    protected final static PatternReplacements removePatterns = initRemovePatterns();
    protected final static PatternReplacements countryPatterns = initCountryPatterns();

    /**
     * Perform additional text fixes that can't be expressed as a simple pattern
     * replacement. Currently, this consists of replacing the contents of a curly-braced
     * expression with the contents of the parenthesized expression that it contains. So
     * '{blah blah (X)}' is replaced by X.
     *
     * @param text In put text to apply fixes to
     * @return Fixed text
     */
    protected static String quickFix(String text) {
        Matcher m = curlyPattern.matcher(text);

        if (m.matches())
            return m.replaceAll(m.group(1));
        else
            return text;
    }

    private static PatternReplacements initManualReplacements() {
        return initPatternReplacements("/manual_replacement_library.txt");
    }

    private static PatternReplacements initRemovePatterns() {
        return initPatternReplacements("/remove_patterns.txt");
    }

    private static PatternReplacements initCountryPatterns() {
        return initPatternReplacements("/country_patterns.txt");
    }

    private static PatternReplacements initPatternReplacements(String resource) {
        URL url = RawLocation.class.getResource(resource);

        PatternReplacements pr = null;  

        try (BufferedReader in = 
                new BufferedReader(new InputStreamReader(url.openStream())))
        {
            pr = PatternReplacements.loadFromFile(in);
        }
        catch(java.io.IOException e) {
            System.out.println("There was a problem reading '" + resource + "'");
        }

        return pr;
    }
}
