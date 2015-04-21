package lodi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManualReplacements {

    public static class Builder {
        public Builder() {
            this.patterns = new LinkedList<>();
            this.replacements = new LinkedList<>();
        }

        public Builder add(String pattern, String replacement) {
            this.patterns.add(pattern);
            this.replacements.add(replacement);

            return this;
        }

        public ManualReplacements build() {
            return new ManualReplacements(patterns, replacements);
        }

        private final LinkedList<String> patterns;
        private final LinkedList<String> replacements;
    }

    public static ManualReplacements loadFromFile(String filename) 
        throws java.io.IOException
    {
        Builder b = new Builder();
        BufferedReader in = new BufferedReader(new FileReader(filename));

        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();

            // skip comments and empty lines
            if (line.startsWith("#") || line.isEmpty())
                continue;

            String[] lineSplit = line.split("|");
            b.add(lineSplit[0], lineSplit[1]);
        }

        in.close();

        return b.build();
    }

    public ManualReplacements(LinkedList<String> patterns, LinkedList<String> replacements) {
        if (patterns.size() != replacements.size())
            throw new IllegalArgumentException("`patterns` and `size` should have the same length");

        this.replacements = new HashMap<>();

        while (!patterns.isEmpty()) {
            String p = patterns.removeFirst();
            String r = replacements.removeFirst();
            this.replacements.put(Pattern.compile(Pattern.quote(p)), r);
        }
    }

    public String apply(String in) {
        for (Map.Entry<Pattern, String> entry: replacements.entrySet()) {
            Matcher m = entry.getKey().matcher(in);
            in = m.replaceAll(entry.getValue());
        }

        return in;    
    }

    private final HashMap<Pattern, String> replacements;
}
