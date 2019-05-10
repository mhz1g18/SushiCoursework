# COMP1206: Sushi Coursework 2
## Main source tree
The main source tree can be found in src/main/java

## Dependencies:
- You must have a valid pom.xml file which contains all needed dependencies
- Executing mvn install will build /target/sushi-2.jar in the target folder correctly.

## Execution: 
- Executing java -jar target/sushi-2.jar server will start the server
- Executing java -jar sushi-2.jar client will start the client.

## Creating the jar file
    mvn install

## Running
    java -jar target/sushi-2.jar
    java -jar target/sushi-2.jar server
    java -jar target/sushi-2.jar client
    

## In this coursework
    Exception handling
    Multithreading
    Client - Server communication
    GUI
    
## To do
    Improve server - client communication and message parsing (Implement communication parsing via a Queue)
    Optimize drones

