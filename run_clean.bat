#!/bin/bash

echo 'Running assignee disambiguation'
python lib/assignee_disambiguation.py

# TODO: fixup lawyer disambiguation
echo 'Running lawyer disambiguation'
python lib/lawyer_disambiguation.py grant

echo 'Running geo disambiguation'
python lib/geoalchemy.py
