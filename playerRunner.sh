#!/bin/bash
#This script is a slightly nicer way to run the PlayerRunner app than via Gradle.
#Sample usage: ./playerRunner.sh 9147 RandomGamer
#For a GUI-based player runner, try: ./gradlew player

#To change the JVM arguments for a player run from this script (including the
#maximum heap size), modify the playerJvmArgs list in build.gradle.

./gradlew playerRunner -Pport=$1 -Pgamer=$2
