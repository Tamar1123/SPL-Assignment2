package scheduling;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        workers = new TiredThread[numThreads];
        Random rand = new Random();
        for (int i = 0; i < numThreads; i++) {
            double fatigue = rand.nextDouble() + 0.5;
            workers[i] = new TiredThread(i, fatigue);
            idleMinHeap.add(workers[i]);
        }
        for (TiredThread worker : workers) {
            worker.start();
        }
    }
    
    public void submit(Runnable task) {
        TiredThread worker = null;
        while (worker == null) {
            try {
                worker = idleMinHeap.take(); // blocks until a worker is available
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        TiredThread finalWorker = worker;

        Runnable wrappedTask = () -> {
            try {
                long startTime = System.nanoTime();
                task.run();
                long endTime = System.nanoTime();
                finalWorker.increaseTimeUsed(endTime - startTime);
            } finally {
                synchronized (inFlight) {
                    inFlight.decrementAndGet();
                    idleMinHeap.add(finalWorker);
                    inFlight.notifyAll();
                }
                
            }
        };
        inFlight.incrementAndGet();
        try {
            finalWorker.newTask(wrappedTask);
        } catch (IllegalStateException e) {
            inFlight.decrementAndGet();
            idleMinHeap.add(finalWorker);
            throw new RuntimeException("Failed to submit task to worker", e);
        }
    }


    public void submitAll(Iterable<Runnable> tasks) {
        for (Runnable task : tasks) {
            try {
                submit(task);
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to submit task: " + task);
            }
        }

        //Wait for all tasks to be completed
        synchronized (inFlight) {
            while (inFlight.get() > 0) {
                try {
                    inFlight.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } 
            }
        }
        
    }


    public void shutdown() throws InterruptedException {
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
    }

    // public synchronized String getWorkerReport() {
    //     String report = "Worker Report:\n";
    //     double totalFatigue = 0;
        
    //     for (TiredThread worker : workers) {
    //         if (!worker.isBusy())
    //             worker.setIdleTime();
    //             totalFatigue += worker.getFatigue();

    //         report += String.format("Worker %d - Current status: %s, Fatigue: %.2f, Time Used: %.2f s, Time Idle: %.2f s\n",
    //                 worker.getWorkerId(),
    //                 worker.isBusy() ? "Busy" : "Idle",  
    //                 worker.getFatigue() / 1_000_000_000.0,
    //                 worker.getTimeUsed() / 1_000_000_000.0, 
    //                 worker.getTimeIdle() / 1_000_000_000.0);
    //     }
        
    //     if (workers.length > 0) {
    //     double avgFatigue = totalFatigue / workers.length;
    //     double sumSquaredDeviations = 0;
    //     for (TiredThread worker : workers) {
    //         double deviation = (worker.getFatigue() / 1_000_000_000.0) - avgFatigue;
    //         sumSquaredDeviations += Math.pow(deviation, 2);
    //     }

    //     report += (String.format("Fairness Score (Sum of Squared Deviations): %.4f\n", sumSquaredDeviations));
    // }
    //     return report;
    // }


    public synchronized String getWorkerReport() {
        String report = "Worker Report:\n";
        double totalFatigue = 0;
        
        for (TiredThread worker : workers) {
            if (!worker.isBusy())
                worker.setIdleTime();
            
            // FIX: Normalize fatigue to seconds BEFORE adding to total
            double currentFatigue = worker.getFatigue() / 1_000_000_000.0;
            totalFatigue += currentFatigue;

            report += String.format("Worker %d - Current status: %s, Fatigue: %.2f, Time Used: %.2f s, Time Idle: %.2f s\n",
                    worker.getWorkerId(),
                    worker.isBusy() ? "Busy" : "Idle",  
                    currentFatigue,
                    worker.getTimeUsed() / 1_000_000_000.0, 
                    worker.getTimeIdle() / 1_000_000_000.0);
        }
        
        if (workers.length > 0) { // Note: use .size() if workers is a List
            double avgFatigue = totalFatigue / workers.length;
            double sumSquaredDeviations = 0;
            for (TiredThread worker : workers) {
                // Calculation now uses consistent 'seconds' units
                double deviation = (worker.getFatigue() / 1_000_000_000.0) - avgFatigue;
                sumSquaredDeviations += Math.pow(deviation, 2);
            }

            report += (String.format("Fairness Score: %.4f\n", sumSquaredDeviations));
        }
        return report;
    }
    int getInFlightCount() {
        return inFlight.get();
    }

}
