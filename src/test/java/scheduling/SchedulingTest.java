package scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchedulingTest {

    private TiredExecutor executor;

    @BeforeEach
    public void setUp() {
        executor = new TiredExecutor(4);
    }

    @Test
    public void demonstrate() throws InterruptedException {
      System.out.println(executor.getWorkerReport());
      for (int i = 0; i < 10; i++) {
          final int taskId = i;
          executor.submit(() -> {

            System.out.printf("Task %d is running on thread %d%n %s \n \n",
            taskId,
            ((TiredThread)(Thread.currentThread())).getWorkerId(),
            executor.getWorkerReport()); 

              try {
                  Thread.sleep(500);
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
              }
          });
          
      }
        executor.shutdown();
    }

    @Test
    public void testSubmitSingleTask() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        executor.submit(counter::incrementAndGet);
        executor.shutdown();
        assertEquals(1, counter.get());
    }

    @Test
    public void testSubmitMultipleTasks() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            executor.submit(counter::incrementAndGet);
        }
        executor.shutdown();
        assertEquals(10, counter.get());
    }

    @Test
    public void testSubmitAll() throws InterruptedException {
        List<Runnable> tasks = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            tasks.add(counter::incrementAndGet);
        }
        executor.submitAll(tasks);
        executor.shutdown();
        assertEquals(10, counter.get());
    }
 

    @Test
    public void testInFlightCounterIntegrity() throws InterruptedException {
    int numTasks = 10;
    AtomicInteger tasksNotFinished = new AtomicInteger(numTasks);
    List<Runnable> tasks = new ArrayList<>();
    for (int i = 0; i < numTasks; i++) {
        tasks.add(() -> {
            try {
                Thread.sleep(10); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                tasksNotFinished.decrementAndGet();
            }
        });
    }
    // Submit all tasks
    executor.submitAll(tasks);
    assertEquals(tasksNotFinished.get(), 0, 
        "All tasks should have completed their work");

    assertEquals(0, executor.getInFlightCount(), 
        "In-flight counter should return to 0 after all tasks complete");
    
    executor.shutdown();
    }


    @Test
    public void testSubmitEmptyTaskList() {
        // executor should not block or throw an error on empty list of tasks
        List<Runnable> emptyTasks = new ArrayList<>();
        executor.submitAll(emptyTasks);
     
        assertEquals(0, executor.getInFlightCount());
        
        try {
            executor.shutdown();
        } catch (InterruptedException e) {
        }
    }
}