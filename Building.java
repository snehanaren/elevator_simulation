import java.lang.*;
import java.util.*; 
import javafx.util.Pair; 
import java.util.Random;

public class Building {
    
    Elevator[] elevators;
    ArrayList<Pair<Integer, PriorityQueue<Person>>> elevatorCallQueue;
    boolean isStopped = false;

    public Building() {
        elevators = new Elevator[3];
        elevators[0] = new Elevator("A");
        elevators[1] = new Elevator("B");
        elevators[2] = new Elevator("C");
        elevatorCallQueue = new ArrayList<>();
    }

    public void initElevators() {
        elevators[0].start();
        elevators[1].start();
        elevators[2].start();
    }

    public int [] getElevatorFloors () {
        return new int [] {elevators[0].current_floor, elevators[1].current_floor, elevators[2].current_floor};
    }

    public void stop() {
        isStopped = true;
        while (elevatorCallQueue.size() > 0) {
            callElevator();
        }
        for (Elevator e : elevators) {
           e.isStopped = true;
        }
    }

    public boolean checkStopped() {
        for (Elevator e : elevators) {
            if (e.thread.getState()!=Thread.State.NEW) {
                return false;
            }
        }
        return true;
    }

    public PriorityQueue<Person> generatePeople() {
        Random rand = new Random();
        int numPeople = rand.nextInt(5) + 1;
        PriorityQueue<Person> pq = new PriorityQueue<>((a,b) -> a.destFloor - b.destFloor);
        // generate random people
        for (int i = 0; i<numPeople; i++) {
            pq.add(new Person(generateFloor())); //generate a person with a random destination floor 
        }
        return pq;
     }

    //  method to return the first closed elevator, if none exist, return -1
     public int nextValid() {
         for (int i = 0; i<elevators.length; i++) {
             if (elevators[i].isClosed) return i;
         }
         return -1;
     }

     public int generateFloor() {
        //give a 10% chance of picking lobby
        Random rand = new Random();
        int isLobby = rand.nextInt(10);
        if (isLobby == 0) {
            return 0;
        }
        else return rand.nextInt(98) + 1; //floors are 0-10 for right now, for testing purposes
     }

    //  method to generate an elevator being called - randomly generates pickup floor, people and destination floor
     public Pair<Integer, PriorityQueue<Person>> elevatorCall() {
         int floor = generateFloor();
         PriorityQueue<Person> myQueue = generatePeople();
         return new Pair<Integer, PriorityQueue<Person>> (floor, myQueue);
     }

     public boolean validFloor(int destFloor, int currFloor, Elevator.Direction dir) {
         if (dir == Elevator.Direction.DOWN) {
             return (destFloor <= currFloor);
         }
         else {
             //elevator is going up
             return destFloor > currFloor;
         } 
     }

     public boolean pickElevator(Pair<Integer, PriorityQueue<Person>> elevatorCall, int elevator) {
        int pickup_floor = elevatorCall.getKey();
        PriorityQueue<Person> passengers = elevatorCall.getValue();
            if (passengers.size() == 0) return true; //if pq is empty, exit out of method
            // destination of pickup floor w.r.t elevator floor
            Elevator currElevator = elevators[elevator];
            //gets the direction of the elevator
            Elevator.Direction dir = pickup_floor - currElevator.current_floor < 0 ? Elevator.Direction.DOWN : Elevator.Direction.UP;
            boolean directionChanged = false;
            if (dir == currElevator.direction || directionChanged) {
                if (currElevator.floors.size() == 0) {
                    dir = pickup_floor - passengers.peek().destFloor > 0 ? Elevator.Direction.DOWN : Elevator.Direction.UP;
                    if (dir != currElevator.direction) directionChanged = true;
                }
                int peopleAdded = 0;
                //if elevator is going in the same direction, then figure out how many people it can pick up 
                while (passengers.size() > 0 && (currElevator.floors.size() == 0 || validFloor(passengers.peek().destFloor, pickup_floor, dir) && currElevator.numPeople + ++peopleAdded <= 10)) {
                    if (currElevator.isClosed) {
                        currElevator.isClosed = false;
                    }
                    //if current elevator has the destination floor in floors, then simply add this floor to destination
                    if (currElevator.floors.containsKey(pickup_floor)) {
                        Pair<Integer, List<Integer>> curr_pair = currElevator.floors.get(pickup_floor);
                        List<Integer> toAdd = curr_pair.getValue();
                        toAdd.add(passengers.poll().destFloor);
                        currElevator.floors.put(pickup_floor, new Pair<Integer, List<Integer>> (curr_pair.getKey()+1, toAdd));
                    }
                    else {
                        ArrayList<Integer> toAdd = new ArrayList<Integer>();
                        toAdd.add(passengers.poll().destFloor);
                        currElevator.floors.put(pickup_floor, new Pair<Integer, List<Integer>>(1, toAdd));
                    }
                }
                if (passengers.size() > 0) return false;
                return true; 
            }
            return false;
        }
    
    public void callElevator() {
            Pair<Integer, PriorityQueue<Person>> elevatorCall = elevatorCallQueue.get(0);
            boolean isDone = false;
            int nextValid = -1;
            while (!isDone) {
                if ((nextValid = nextValid()) > -1) {
                    isDone = pickElevator(elevatorCall, nextValid);
                }
                else {
                    for (int i = 0; i<elevators.length; i++) {
                        isDone = pickElevator(elevatorCall, i);
                        if (isDone) break;
                    }
                }
            }
            if (elevatorCall.getValue().size() == 0) elevatorCallQueue.remove(0);
    }
     //  method to pick which elevators to use
    public void pickup () {
        if (elevatorCallQueue.size() > 0) {
            callElevator();
        }
    }
}