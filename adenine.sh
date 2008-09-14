#!/bin/bash
#export CLASSPATH=`find subfloor/jars -name "*jar" | paste -sd ':' -`
#echo $CLASSPATH
#java -jar adenine.jar
java -cp adenine.jar:`find subfloor/jars -name "*jar" | paste -sd ':' -` edu.mit.lcs.haystack.adenine.Console
