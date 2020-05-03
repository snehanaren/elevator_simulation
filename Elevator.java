import java.util.*;
import java.lang.StringBuilder;
import javafx.util.Pair; 
import java.util.SortedMap; 


public class Elevator implements Runnable{
    enum Direction {
        UP,
        DOWN
    }
    // global variables
    Direction direction;
    int current_floor;
    int numPeople;
    boolean isClosed;
    boolean running;
    Thread thread;
    boolean isStopped;
    ArrayList<List<String>> elevatorStatus;
    String name;
    //sorted map to keep track of all the floors we need to go to
    SortedMap<Integer, Pair<Integer, List<Integer>>> floors;

    // when an elevator is initialized, it starts at the lobby floor and is closed
    public Elevator(String name) {
        this.name = name;
        this.direction = Direction.UP;     
        this.isClosed = true;  
        this.floors = new TreeMap<>((a,b) -> a-b);
        this.elevatorStatus = new ArrayList<List<String>>();
        this.isStopped = false;
        running = true;
    }

    public void goBackDown() {
        if (this.floors.size() > 0) return;
        isClosed = true;
        this.direction = Direction.DOWN;
        // go back to lobby
        while (this.floors.size() == 0 && current_floor != 0) {
            try {
                //pause thread for one second to wait for updates
                thread.sleep(1000);
                current_floor--;
            }
            catch (InterruptedException e){
            }
        }
        atLobby();
    }


    public void atLobby() {
        if (current_floor == 0) {
            this.direction = Direction.UP;
        }
        if (floors.size() == 0) {
            isClosed = true;
        }
    }

    public String getFloorsLeft() {
        StringBuilder sb = new StringBuilder();
        if (floors.size() <= 0) return "N/A";
        for (int i : floors.keySet()) {
            sb.append(i + " ");
        }
        return sb.toString();
    }

    public void goToFloor(int floor, Pair<Integer, List<Integer>> dest_floors) {
        //delete floor from map
        while (current_floor != floor) {
            try {
                if (direction == Direction.UP) {
                    current_floor+=1;
                }
                else current_floor-=1;
                //pause thread for one second to move
                thread.sleep(1000);
            }
            catch (InterruptedException e){
            }
        }
        //to drop off all the people that this is their destination floor, we just change the number in 
        // numPeople of our elevator
        numPeople += dest_floors.getKey();
        //TO PICK UP all the people needed
        List<Integer> destinations = dest_floors.getValue();
        // these are all the people we PICK UP that have new destinations
        for (int i : destinations) {
            if (i == current_floor) {
                //subtract number of people at current floor and subtract
                numPeople--;
                continue;
            }
            // check if current floor is already a destination
            if (floors.containsKey(i)){
                Pair<Integer, List<Integer>> curr_pair = floors.get(i);
                // then we add simply increment the number of people drop off in pair
                floors.put(i, new Pair<Integer, List<Integer>> (curr_pair.getKey() - 1, curr_pair.getValue()));
            }
            //otherwise, we add the current floor
            else {
                floors.put(i, new Pair<Integer, List<Integer>> (-1, new ArrayList<Integer>()));
            }
        }
        // check destination floors, and switch directions if necessary
        if (direction == Direction.UP && (floors.size() > 0 && floors.lastKey() < current_floor)) {
            direction = Direction.DOWN;
        }
        else if (direction == Direction.DOWN && (floors.size() > 0 && floors.firstKey() > current_floor)) {
            direction = Direction.UP;
        }
            //if at lobby, wait for 30 seconds
            int NUMWAIT = current_floor == 0 ? 30000 : 5000;
            //pause thread for 5 seconds to pickup/drop off people
            try {
                updateStatus();
                thread.sleep(NUMWAIT);
            }
            catch (InterruptedException e){}
    }

    // method to move the elevator to pick up/drop off people
    public void moveElevator() {
        //if we're going up, then we want to move the elevator low - high
        if (direction == Direction.UP) {
            int firstKey = floors.firstKey();
            Pair<Integer, List<Integer>> dest_floors = floors.get(firstKey);
            floors.remove(firstKey);
            goToFloor(firstKey, dest_floors);
        }
        else {
            int lastKey = floors.lastKey();
            Pair<Integer, List<Integer>> dest_floors = floors.get(lastKey);
            floors.remove(lastKey);
            goToFloor(lastKey, dest_floors);
        }
    }


    public void updateStatus() {
        List<String> toAdd = new ArrayList<String>();
        toAdd.add("ELEVATOR NAME: " + name + " ");
        toAdd.add("ELEVATOR IS: " + (isClosed ? "CLOSED" : "OPEN") + " ");
        toAdd.add("NUMBER OF PEOPLE: " + numPeople + " ");
        toAdd.add("CURRENT FLOOR: " + current_floor + " ");
        toAdd.add("DESTINATION FLOORS: " + getFloorsLeft());
        System.out.print(toAdd);
        System.out.print("\n");
        elevatorStatus.add(toAdd);
    }

    //method to start the elevator 
    public void start() {
        if (thread == null) {
            thread = new Thread (this);
        }
        isClosed = true;
    }

    public void killRunning() throws InterruptedException {
        if (floors.size() > 0) return; 
        running = false; 
        updateStatus();
        Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        // keep thread running until simulation terminates
        while (!Thread.currentThread().isInterrupted()) {
            try {
                thread.sleep(1);
                if (floors.size() > 0) isClosed = false;
                while (!isClosed) {
                    //move the elevator to either pickup/drop off people
                    if (floors.size() > 0) {
                        moveElevator();
                    }
                    else goBackDown();
                }
                if (floors.size() == 0 && isStopped) {
                    killRunning();
                }
            }
            catch (InterruptedException e){
                updateStatus();
                Thread.currentThread().interrupt();
            }
        }
    }
}