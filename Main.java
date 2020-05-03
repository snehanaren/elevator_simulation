import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.*;


import javax.print.attribute.standard.PrinterLocation;

import java.util.*; 
import javafx.util.Pair; 


public class Main {
    public static Building myBuilding = new Building();
    public static ScheduledFuture<?> t;
    int runs = 0;

    public static void main(String [] args) {
        String filename;
        if (args.length > 0)  filename = args[0];
        else  filename = null;
        ExecutorService service = Executors.newFixedThreadPool(3);
        myBuilding.initElevators();
        service.submit(myBuilding.elevators[0]);
        service.submit(myBuilding.elevators[1]);
        service.submit(myBuilding.elevators[2]);
        // schedule an executer to run the task every x seconds
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        t = executor.scheduleAtFixedRate(new Main().new RunnableImpl(), 0, 30, TimeUnit.SECONDS);
        while (true) {
            if (t.isCancelled()) {
                //call building to stop
                myBuilding.stop();
                while (myBuilding.isStopped) {
                    if (myBuilding.checkStopped()) {
                        //then we need to stop all services
                        service.shutdown();
                        executor.shutdown();
                        try {
                            if (!service.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES)) {
                                service.shutdownNow();
                            }
                        } catch (InterruptedException ex) {
                            service.shutdownNow();
                        }
                        try {
                            if (filename != null) {
                                PrintWriter myWriter = new PrintWriter(filename);
                                myWriter.print(printElevatorTimeSeries());
                                myWriter.close();
                            }
                            else printElevatorTimeSeries();
                        }
                        catch (IOException e) {} 
                        return;
                    }
                }
            }
            // keep calling elevator pickup
            else myBuilding.pickup();
        }
    }

    public static void printElevatorCall(Pair<Integer, PriorityQueue<Person>> myCall) {
        System.out.print("---------NEW ELEVATOR CALL--------- \n");
        System.out.print("PICK UP FLOOR IS: " + Integer.toString(myCall.getKey()) + "\n");
        System.out.print("DESTINATION FLOORS ARE: ");
        for (Person p : myCall.getValue()) {
            System.out.print(Integer.toString(p.destFloor) + " ");
        }
        System.out.print("\n\n");
    }

    public static String printElevatorTimeSeries() {
        StringBuilder sb = new StringBuilder();
        for (Elevator e : myBuilding.elevators) {
            for (List<String> myList : e.elevatorStatus) {
                    for (String s : myList) {
                        sb.append(s);
                    }
                    sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private class RunnableImpl implements Runnable { 
  
        public void run() 
        { 
                Pair<Integer, PriorityQueue<Person>> myPair = myBuilding.elevatorCall();
                printElevatorCall(myPair);
                runs++;
                //add call to queue
                myBuilding.elevatorCallQueue.add(myPair);
                if (runs >= 3) {
                    //cancel next future thread
                    t.cancel(true);
                    return;
                }
            }
        } 
}