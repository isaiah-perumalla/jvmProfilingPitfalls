import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Scanner;

public class CpuBound {

    private final int limit;

    private ArrayList<Integer> primes = new ArrayList<>();
    private Object primesLock = new Object();
    private volatile int total = 1000 ;
    private volatile String str = "";

    public CpuBound(int limit) {

        this.limit = limit;

    }

    static {
        //do not cache  dns lookups
        Security.setProperty("newtorkaddress.cache.ttl", "0");
    }

    public int count() {
        return primes.size();
    }

    public void execute() throws Exception {
        Thread[] threads = new Thread[2];

            threads[0] = new Thread(() -> doWork(limit), "do-work-thread");
            threads[1] = new Thread(() -> ioAndCpu(10_000), "io-and-cpu-thread");

            for(Thread t : threads) {
                t.start();
            }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private void doWork(int limit) {
        for (int i = 0; i <= limit; ++i) {
            {

                sumNumbersInefficient(total);
                sumNumbers(total);
                addToCollection(i);
            }
        }
    }

    private void sumNumbersInefficient(int total) {
        int limit = total * 2;
        int sum = 0;
        for (int i = 0; i < limit; i++) {
            sum += i;
        }
        this.total = sum;
    }

    private void sumNumbers(int max) {
        int sum = 0;
        for(int i = 0; i < max; i++) {
            sum += i;
        }
        total = sum;
    }

    private void ioAndCpu(int max) {
        int sum = 0;
        for (int i =0; i< max; i++) {
            sum += i;
            if(sum % 100_000 == 0) {
                InputStream istream = null;
                try {
                    istream = new URL("http://wwww.google.com").openStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                str = new Scanner(istream).useDelimiter("\\A").next();
            }
        }
    }

    private void addToCollection(int i) {
        synchronized (primesLock) {
            primes.add(i);
        }
    }

    public static void main(String[] args) throws Exception {
        int limit = Integer.parseInt(args[0]);

        CpuBound cpuBound = new CpuBound(limit);
        System.out.println("Press RETURN to start.");
        System.in.read();
        long start = System.nanoTime();
        cpuBound.execute();
        long finish = System.nanoTime();
        System.out.println("execution-time ms: " + (finish - start)/1000_000);
    }
}

