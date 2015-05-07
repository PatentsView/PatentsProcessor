# Location Disambiguation

## Cleaning Raw Locations

Raw locations are taken from the database. For each raw location, the following
transformations are applied to the city, state, and country fields:

* EOL characters are removed
* Pipe characters are removed
* Replacements in manual_replacement_library.txt are performed. These replacements try to
    replace the different ways that non-ASCII characters might be represented with the
    UNICODE equivalents.
* Replace any occurrences of patterns like {dot over (a)} with the character in
    parentheses.
* Remove XML/HTML tags
* Normalize the text to the canonical UNICODE form
* Remove patterns that appear in remove_patterns.txt
* For the country field, the replacements specified in country_patterns.txt are applied.
* If the cleaned country field is "US", then the replacements specified in
    state_abbreviations.txt are applied. These translate state names to their 2-letter
    codes.

## Geolocation database

The geolocation database contains 2 tables: cities, and google_cities. The cities table
contains all unique city-region-country combinations

### The Raw Google data

## Disambiguation

For each input location record, we create a fields by concatenating the cleaned values of
city, state, and country.
