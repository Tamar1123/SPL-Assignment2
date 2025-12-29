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
    

    //deterministic constructor for testing.
    public TiredExecutor(int numThreads, double[] fatigueFactors) {
        workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            double fatigue = fatigueFactors[i];
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
                inFlight.decrementAndGet();
                idleMinHeap.add(finalWorker);
                
            }
        };
        inFlight.incrementAndGet();
        try {
            finalWorker.newTask(wrappedTask);
        } catch (IllegalStateException e) {
            synchronized (inFlight) {
                inFlight.decrementAndGet();
                idleMinHeap.add(finalWorker);
                inFlight.notifyAll();
            }
            
        }
        
    }


    public void submitAll(Iterable<Runnable> tasks) {
        for (Runnable task : tasks) {
            submit(task);
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
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Executor shutdown interrupted");
            }
        }
        for (TiredThread worker : workers) {
            worker.join();
        }
    }

    public synchronized String getWorkerReport() {
        String report = "Worker Report:\n";
        
        for (TiredThread worker : workers) {
            if (!worker.isBusy())
                worker.setIdleTime();
            report += String.format("Worker %d - Current status: %s, Fatigue: %.2f, Time Used: %.2f s, Time Idle: %.2f s\n",
                    worker.getWorkerId(),
                    worker.isBusy() ? "Busy" : "Idle",  
                    worker.getFatigue() / 1_000_000_000.0,
                    worker.getTimeUsed() / 1_000_000_000.0, 
                    worker.getTimeIdle() / 1_000_000_000.0);
        }
        return report;
    }

}
