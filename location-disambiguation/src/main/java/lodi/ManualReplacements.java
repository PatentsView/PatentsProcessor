package lodi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringJoiner;
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
        StringBuilder b1 = new StringBuilder();
        StringBuilder b2 = new StringBuilder();
        StringBuilder temp;


    }

    private final HashMap<Pattern, String> replacements;
}
