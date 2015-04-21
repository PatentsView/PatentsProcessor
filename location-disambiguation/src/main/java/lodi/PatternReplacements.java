package lodi;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternReplacements {

    public static PatternReplacements loadFromFile(BufferedReader in) 
        throws java.io.IOException
    {
        PatternReplacements mr = new PatternReplacements();

        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();

            // skip comments and empty lines
            if (line.startsWith("#") || line.isEmpty())
                continue;

            String[] lineSplit = line.split("|");


            if (lineSplit.length == 1) {
                mr.add(lineSplit[0]);
            } 
            else {
                String pattern = lineSplit[0];
                String replacement = lineSplit[1];
                int flags = 0;

                if (lineSplit.length > 2) {
                    String f = lineSplit[2];
                    for (int i = 0; i < f.length(); i++) {
                        switch(f.charAt(i)) {
                            case 'u': flags |= Pattern.UNICODE_CHARACTER_CLASS;
                                      break;
                            case 'm': flags |= Pattern.MULTILINE;
                                      break;
                        }
                    }
                }

                mr.add(pattern, replacement, flags);
            }
        }

        return mr;
    }

    public PatternReplacements() {
        this.replacements = new HashMap<>();
    }

    public PatternReplacements add(String pattern, String replacement, int flags) {
        replacements.put(Pattern.compile(pattern, flags), replacement);
        return this;
    }

    public PatternReplacements add(String pattern, String replacement) {
        return this.add(pattern, replacement, 0);
    }

    public PatternReplacements add(String pattern, int flags) {
        return this.add(pattern, "", flags);
    }

    public PatternReplacements add(String pattern) {
        return this.add(pattern, "", 0);
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
