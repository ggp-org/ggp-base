#!/bin/bash
#This script allows the user to run the GameServerRunner app via Gradle.
#Arguments: ./gameServerRunner <results directory> <game key> <start clock> <play clock> <player host 1> <player port 1> <player name 1> <player host 2> <player port 2> <player name 2> etc.
#Sample usage: ./gameServerRunner myTournament ticTacToe 60 15 127.0.0.1 9147 PlayerOne 127.0.0.1 9148 PlayerTwo
#For a GUI-based server, try: ./gradlew server

./gradlew gameServerRunner -Pmyargs="$*"
