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
        TiredThread worker = idleMinHeap.poll();        
        while (worker == null) {
            synchronized (this) {
                try {
                this.wait();
                } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                }
                worker = idleMinHeap.poll();
            }
        }
        worker.newTask(task);
        inFlight.incrementAndGet();
    }


    public void submitAll(Iterable<Runnable> tasks) {
        for (Runnable task : tasks) {
            submit(task);
            } 
        
    }

    public void shutdown() throws InterruptedException {
        for (int i = 0; i < workers.length; i++) {
            workers[i].shutdown();
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("This thread was interrupted while shutdown");
            }
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        return null;
    }

    // public synchronized void notifyIdle(TiredThread worker) {
    //     long now = System.nanoTime();
    //     for (TiredThread t : idleMinHeap) {
    //         if (t.isBusy() == false) {
                
    //         }
    //     }
    //     idleMinHeap.add(worker);
    //     this.notify();
    // }



}
