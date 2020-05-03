#!/bin/bash
echo "---------Compiling Files---------"
javac Main.java
touch output.txt
echo "---------Running Elevator Simulation---------"
java Main "output.txt"
echo "Elevator Simulation Over"
echo "Your output can be viewed in \"output.txt\""