package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TiredThread extends Thread implements Comparable<TiredThread> {

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle

    public TiredThread(int id, double fatigueFactor) {
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    // Update idle time.
    public void setIdleTime() {
        long oldVal;
        long newVal;
        do {
            oldVal = timeIdle.get();
            newVal = oldVal + (System.nanoTime() - idleStartTime.get());
        } while (!timeIdle.compareAndSet(oldVal,newVal));
        idleStartTime.set(System.nanoTime());
    }

    public void increaseTimeUsed(long time) {
        long oldVal;
        long newVal;
        do {
            oldVal = timeUsed.get();
            newVal = oldVal + time;
        } while (!timeUsed.compareAndSet(oldVal,newVal));
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */
    public synchronized void newTask(Runnable task) {
       if (handoff.offer(task)) {
            busy.set(true);
        } else { 
            throw new IllegalStateException("Worker is not ready to accept a new task");
        }

    }

    /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */
    public void shutdown() {
        try {
            handoff.put(POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.interrupt();
        }
    }

    @Override
    public void run() {
        while (alive.get()) {
            Runnable task;
            try {
                task = handoff.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                shutdown();
                continue;
            }
            if (task == POISON_PILL) {
                alive.set(false);
                break;
            }
            executeTask(task);
        }   
    }

    private synchronized void executeTask(Runnable task) {
        timeIdle.addAndGet(System.nanoTime() - idleStartTime.get());
        task.run();    
        idleStartTime.set(System.nanoTime());
        
        busy.compareAndSet(true, false);
    }

    @Override
    public int compareTo(TiredThread o) {
        double diff = getFatigue()-o.getFatigue();
        if (diff == 0) 
            return 0;
        if (diff > 0)
            return 1;
        return -1;
    }
}