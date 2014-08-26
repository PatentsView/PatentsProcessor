#!/bin/bash

rm disambiguator.csv
echo 'Running consolidation for disambiguator'
python consolidate.py $1
