package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
            workers[i].start();
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
                
                task.run();
                
            } finally {
                inFlight.decrementAndGet();
                idleMinHeap.add(finalWorker);
                
            }
        };
        inFlight.incrementAndGet();
        finalWorker.newTask(wrappedTask);
    }


    public void submitAll(Iterable<Runnable> tasks) {
        for (Runnable task : tasks) {
            submit(task);
        } 
        
    }

    public void shutdown() throws InterruptedException {
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
        for (TiredThread worker : workers) {
            worker.join();
        }
    }

    public synchronized String getWorkerReport() {
        String report = "Worker Report:\n";
        for (TiredThread worker : workers) {
            report += String.format("Worker %d - Fatigue: %.2f, Time Used: %.2f s, Time Idle: %.2f s\n",
                    worker.getWorkerId(),
                    worker.getFatigue(),
                    worker.getTimeUsed() / 1_000_000_000.0,
                    worker.getTimeIdle() / 1_000_000_000.0);
        }
        return report;
    }

}
