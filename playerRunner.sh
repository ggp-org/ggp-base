#!/bin/bash
#This script is a slightly nicer way to run the PlayerRunner app than via Gradle.
#Sample usage: ./playerRunner.sh 9147 RandomGamer
#For a GUI-based player runner, try: ./gradlew player

./gradlew playerRunner -Pport=$1 -Pgamer=$2

