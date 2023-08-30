import java.util.Comparator;
import java.util.LinkedList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Creating the threads
        Queue q = new Queue();
        final int N = 10;
        final int M = 5;
        int generated = 0, processed = 0;
        GeneratorThread[] generators = new GeneratorThread[N];
        ProcessorThread[] processors = new ProcessorThread[M];
        Notifier.start();
        for (int i = 0; i < generators.length; i++) {
            generators[i] = new GeneratorThread(q);
            generators[i].start();
        }
        for (int i = 0; i < processors.length; i++) {
            processors[i] = new ProcessorThread(q);
            processors[i].start();
        }
        // wait for 30 seconds while Generators and Processors are running
        Thread.sleep(1000 * 30);
        Notifier.stop();
        // waiting for threads to finish
        for (GeneratorThread generator : generators) {
            generator.interrupt();
        }
        for (ProcessorThread processor : processors) {
            processor.join();
        }

        // printing the results
        System.out.print("Queue [");
        for (var e :
                q.getQueue()) {
            System.out.print(e.id + " ");
        }
        System.out.println("]");
        for (int i = 0; i < generators.length; i++) {
            generated += generators[i].getGenerated();
            System.out.println("Gen" + i + " ha generato " + generators[i].getGenerated() + " messaggi");
        }
        for (int i = 0; i < processors.length; i++) {
            processed += processors[i].getProcessed();
            System.out.println("Pro" + i + " ha processato " + processors[i].getProcessed() + " messaggi");
        }
        System.out.println("Generati = " + generated);
        System.out.println("Processati = " + processed);
    }
}

class Queue {
    private final LinkedList<Msg> queue = new LinkedList<>();
    private static final int L = 25;
    private int id = 0;

    public synchronized void add(Msg m) throws InterruptedException {
        // wait if queue is full
        while (queue.size() >= L) wait();
        queue.add(m);
        // sorting the list based on the id (progressive number)
        queue.sort(Comparator.comparingInt(a -> a.id)); // same as queue.sort((a, b) -> (a.id - b.id));
        // awakening all the threads
        notifyAll();
    }

    public synchronized Msg[] get() throws InterruptedException {
        // wait if queue is empty and doesn't contain progressive messages
        // since the queue is sorted it is only necessary to check the first two elements
        while (queue.size() < 2 || (queue.get(0).id != id || queue.get(1).id != (id + 1))) {
            if (!Notifier.isRunning() && queue.size() < 2) {
                // No more elements in queue
                return null;
            }
            wait();
        }
        // getting the messages
        Msg[] res = new Msg[]{queue.get(0), queue.get(1)};
        queue.removeFirst();
        queue.removeFirst();
        id += 2;
        notifyAll();
        return res;
    }

    public boolean hasNewMessage() {
        return queue.size() >= 2;
    }

    public LinkedList<Msg> getQueue() {
        return queue;
    }
}

class SharedCounter {
    private static int counter = 0;

    public synchronized static int get() {
        return counter++;
    }
}

class GeneratorThread extends Thread {
    private final Queue q;
    private int generated;

    public GeneratorThread(Queue q) {
        this.q = q;
    }

    @Override
    public void run() {
        while (Notifier.isRunning()) {
            int id = SharedCounter.get();
            generated++;
            try {
                sleep((long) (Math.random() * 6) * 1000);
            } catch (InterruptedException ignored) {
            }
            Msg m = new Msg(id, (int) (Math.random() * 11));
            try {
                q.add(m);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public int getGenerated() {
        return generated;
    }
}

class ProcessorThread extends Thread {
    private final Queue q;
    private int processed = 0;

    public ProcessorThread(Queue q) {
        this.q = q;
    }

    @Override
    public void run() {
        while (Notifier.isRunning() || q.hasNewMessage()) {
            try {
                Msg[] res = q.get();
                if (res == null) {
                    // since the response is null the processor has no other messages to handle
                    // terminating the thread
                    return;
                }
                System.out.println(getName() + " ha acquisito (" + res[0].id + ", " + res[1].id + ")");
                processed += 2;
            } catch (InterruptedException ignored) {
            }
        }
    }

    public int getProcessed() {
        return processed;
    }
}

class Msg {
    int id;
    int value;

    public Msg(int id, int value) {
        this.id = id;
        this.value = value;
    }
}

final class Notifier {
    private static boolean running = false;

    public static void start() {
        running = true;
    }

    public static void stop() {
        running = false;
    }

    public static boolean isRunning() {
        return running;
    }
}