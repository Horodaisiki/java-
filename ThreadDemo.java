
public class ThreadDemo extends Thread {
    ThreadDemo(ThreadGroup a,String name) {
        super(a,name);
    }

    public void run() {
        // System.out.println(Thread.currentThread().getName() + " begin to run");
        for (int i = 0; i < 5; i++) {
            compute();
        }
    }

    // public void start() {
    // int[] l=new int[10000];
    // for (int i : l) {
    // i=1;
    // }
    // super.start();
    // }

    public static void main(String[] args) {
        ThreadGroup top=Thread.currentThread().getThreadGroup();
        ThreadDemo t1 = new ThreadDemo(top,"t1");
        ThreadDemo t2 = new ThreadDemo(top,"t2");
        // thread2 t2=new thread2();

        // thread t3=new thread("t3");
        // thread t4=new thread("t4");
        // t1.setPriority(10);
        // t2.setPriority(1);
        t1.start();
        // Thread tt2=new Thread(t2,"t2");
        // tt2.start();
        t2.start();
        // t3.start();
        // t4.start();
        for (int i = 0; i < 5; i++) {
            compute();
        }
    }

    static ThreadLocal<Integer> callOfNumber = new ThreadLocal<>();

    static synchronized void compute() {
        Integer n = (Integer) callOfNumber.get();
        if (n == null)
            n = new Integer(1);
        else
            n = new Integer(n.intValue() + 1);
        callOfNumber.set(n);
        System.out.println(Thread.currentThread().getName() + "  " + Thread.currentThread().getPriority() + ": " + n);
        long j = 0;
        for (long i = 0; i < 1000; i++) {
            j += i;
        }
        
        try {
            System.out.println("================");
            thread.AllThread();
            Thread.sleep(100);
            thread.AllThread();
            System.out.println("================");
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        Thread.yield();

    }

}

class thread3 implements Runnable {

    @Override
    public void run() {
        // TODO Auto-generated method stub
        for (int i = 0; i < 5; i++) {
            ThreadDemo.compute();
        }
	}
    
}