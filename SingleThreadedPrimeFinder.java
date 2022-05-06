import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;

class SingleThreadedPrimeFinder {
    final static int CANDIDATE_UPPER_BOUND = (int)1e8;
    final static int NUM_THREADS = 8;
    static FileWriter fileWriter;
    static ArrayList<Integer> top10Primes = new ArrayList<>();
    // static long sum = 0, count = 0;
    static AtomicLong sum = new AtomicLong(0);
    static AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) throws IOException, InterruptedException {
        fileWriter = new FileWriter(new File("output.txt"));

        long startTime = 0, endTime = 0;


        int implementation = 4;

        switch (implementation) {
        case 1:
            Thread t = new Thread(new PrimeFinderThread());
            startTime = Calendar.getInstance().getTimeInMillis();
            t.run(); t.join();
            endTime = Calendar.getInstance().getTimeInMillis();
            break;
        case 2:
            startTime = Calendar.getInstance().getTimeInMillis();
            PrimeFinderThread2.execute();
            endTime = Calendar.getInstance().getTimeInMillis();
            break;
        case 3:
            startTime = Calendar.getInstance().getTimeInMillis();
            PrimeFinder.init();
            (new PrimeFinder.TaskDistributer()).execute();
            endTime = Calendar.getInstance().getTimeInMillis();
            break;
        case 4:
            startTime = Calendar.getInstance().getTimeInMillis();
            var finder = new SegmentedPrimeFinder();
            finder.execute();
            endTime = Calendar.getInstance().getTimeInMillis();
        }
        
        

        try {
            fileWriter.append("Time taken: " + (endTime - startTime) + " ms\n");
            fileWriter.append("Upper bound for prime search: " + CANDIDATE_UPPER_BOUND + "\n");
            fileWriter.append("Number of Primes: " + count + "\n");
            fileWriter.append("Sum of Primes: " + sum + "\n");
            fileWriter.append("Top 10 Primes: \n");
            Collections.reverse(top10Primes);
            for (var prime : top10Primes)
                fileWriter.append(prime + "\n");
        } catch (Exception e) {
            System.out.println("error writing file");
        }

        fileWriter.close();
    }

    static class PrimeFinderThread implements Runnable {
        boolean[] isPrime = new boolean[CANDIDATE_UPPER_BOUND+1];
        int i = 2; // first prime

        @Override
        public void run() {
            // initialize the array
            for (int j = 2; j <= CANDIDATE_UPPER_BOUND; j++)
                isPrime[j] = true;
            
            for (; i <= CANDIDATE_UPPER_BOUND; i++) {
                if (isPrime[i]) {
                    for (int j = 2*i; j <= CANDIDATE_UPPER_BOUND; j+=i)
                        isPrime[j] = false;
                    count.incrementAndGet();
                    sum.addAndGet(i);
                }
            }

            // calculate top 10
            i = CANDIDATE_UPPER_BOUND;
            while (top10Primes.size() < 10) {
                if (isPrime[i])
                    top10Primes.add(i);
                i--;
            }

            Collections.reverse(top10Primes);
        }
    }

    static class PrimeFinderThread2 implements Runnable {
        static boolean[] isPrime = new boolean[CANDIDATE_UPPER_BOUND+1], processed = new boolean[CANDIDATE_UPPER_BOUND+1];
        // upper and lower denote the range of numbers about which we can be certain of the primality
        static AtomicInteger i = new AtomicInteger(2), lower = new AtomicInteger(2), upper = new AtomicInteger(3);
        int id;

        PrimeFinderThread2(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            int candidate = i.getAndIncrement();
            while (candidate <= CANDIDATE_UPPER_BOUND) {
                // System.out.println("start iteration, thread " + id);
                while (candidate > upper.get());
                if (isPrime[candidate]) {
                    for (int j = 2 * candidate; j <= CANDIDATE_UPPER_BOUND; j+=candidate)
                        isPrime[j] = false;
                    sum.addAndGet(candidate);
                    count.incrementAndGet();
                    // System.out.println(sum.get());
                }
                processed[candidate] = true;

                // recalculate the window of numbers
                int low = lower.get();
                if (low <= Math.sqrt(CANDIDATE_UPPER_BOUND) && processed[low]) {
                    synchronized (PrimeFinderThread2.class) {
                        while (low < CANDIDATE_UPPER_BOUND && processed[low])
                            low = lower.incrementAndGet();
                        upper.set((low-1)*(low-1));
                    }
                }
                // System.out.println("finished " + candidate);

                candidate = i.getAndIncrement();
            }
        }

        public static void execute() throws InterruptedException {
            for (int i = 2; i <= CANDIDATE_UPPER_BOUND; i++) {
                isPrime[i] = true;
                processed[i] = false;
            }

            Thread[] threads = new Thread[NUM_THREADS];

            for (int i = 0; i < NUM_THREADS; i++) threads[i] = new Thread(new PrimeFinderThread2(i));
            for (int i = 0; i < NUM_THREADS; i++) threads[i].start();
            for (int i = 0; i < NUM_THREADS; i++) threads[i].join();
                            
            // calculate top 10
            int j = CANDIDATE_UPPER_BOUND;
            while (top10Primes.size() < 10) {
                if (isPrime[j])
                    top10Primes.add(j);
                j--;
            }
        }
    }

    static class PrimeFinder {
        static int i = 2;
        static boolean[] isPrime = new boolean[CANDIDATE_UPPER_BOUND+1], processed = new boolean[CANDIDATE_UPPER_BOUND+1];
        static ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        static AtomicInteger lastProcessed = new AtomicInteger(2);

        static class TaskDistributer {
            public void execute() {
                for (; i <= CANDIDATE_UPPER_BOUND; i++) {
                    if (isPrime[i])
                        executor.submit(new Worker(i));
                }

                executor.shutdown();
                

                for (i = 2; i <= CANDIDATE_UPPER_BOUND; i++)
                    if (isPrime[i]) {
                        count.incrementAndGet();
                        sum.addAndGet(i);
                    }
                
                // calculate top 10
                i = CANDIDATE_UPPER_BOUND;
                while (top10Primes.size() < 10) {
                    if (isPrime[i])
                        top10Primes.add(i);
                    i--;
                }
            }
        }

        static class Worker implements Runnable {
            int prime;

            public Worker(int prime) {
                this.prime = prime;
            }

            @Override
            public void run() {
                if (isPrime[prime])
                    for (int j = 2*prime; j <= CANDIDATE_UPPER_BOUND; j+=prime)
                        isPrime[j] = false;
            }
        }

        public static void init() {
            for (int i = 2; i <= CANDIDATE_UPPER_BOUND; i++) {
                isPrime[i] = true;
                processed[i] = false;
            }
        }
    }

    static class SegmentedPrimeFinder {
        int i;
        Vector<Integer> primeList;
        int SECTOR_SIZE = CANDIDATE_UPPER_BOUND / NUM_THREADS;

        void execute() throws InterruptedException {
            i = 2;
            primeList = new Vector<>();
            Thread [] threads = new Thread[NUM_THREADS];
            Worker [] workers = new Worker[NUM_THREADS];
            for (int id = 0; id < NUM_THREADS; id++) {
                workers[id] = new Worker(id);
                threads[id] = new Thread(workers[id]);
            }

            for (int id = 0; id < NUM_THREADS; id++)
                threads[id].start();

            for (int id = 0; id < NUM_THREADS; id++)
                threads[id].join();
            
            for (int num = CANDIDATE_UPPER_BOUND-1; num >= 2 && top10Primes.size() < 10; num--)
                if (workers[num/SECTOR_SIZE].isPrime[num%SECTOR_SIZE])
                    top10Primes.add(num);
            
            Collections.reverse(top10Primes);
            
        }

        class Worker implements Runnable {
            boolean [] isPrime = new boolean[SECTOR_SIZE];
            int offset;
            int primesReceived = 0;

            public Worker(int id) {
                this.offset = id * SECTOR_SIZE;
                for (int i = 0; i < SECTOR_SIZE; i++)
                    isPrime[i] = true;
            }

            @Override
            public void run() {
                // System.out.println("primesReceived: ", primesReceived, " primelist len == " , primeList.size);
                while (i < offset || primesReceived < primeList.size()) {
                    if (primesReceived < primeList.size()) {
                        int prime = primeList.get(primesReceived++);
                        for (int j = ((offset+prime-1) / prime) * prime; j < offset + SECTOR_SIZE; j+=prime)
                            isPrime[j-offset] = false;
                    }
                }

                for (; i < offset + SECTOR_SIZE; i++)
                    if (isPrime[i-offset]) {
                        primeList.add(i);
                        for (int j = 2*i; j < offset + SECTOR_SIZE; j += i)
                            isPrime[j-offset] = false;
                            count.incrementAndGet();
                            sum.addAndGet(i);
                    }
            }
        }
    }
}