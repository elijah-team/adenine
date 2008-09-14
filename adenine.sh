#!/bin/bash
java -cp adenine.jar:`find subfloor/jars -name "*jar" | paste -sd ':' -` edu.mit.lcs.haystack.adenine.Console
