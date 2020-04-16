
public class thread extends Thread {
    thread(ThreadGroup a,String name) {
        super(a,name);
    }

    public void run() {
        for (int i = 0; i < 5; i++) {
            compute();
        }
    }

    // public void start() {
    //     int[] l=new int[10000];
    //     for (int i : l) {
    //         i=1;
    //     }
    //     super.start();
    // }

    public static void main(String[] args) {
        ThreadGroup top=Thread.currentThread().getThreadGroup();
        System.out.println("Before create");
        AllThread();
        thread t1 = new thread(top,"t1");
        thread t2 = new thread(top,"t2");
        // thread2 t2=new thread2();
        System.out.println("After create");
        AllThread();
        
        // thread t3=new thread("t3");
        // thread t4=new thread("t4");
        // t1.setPriority(10);
        // t2.setPriority(1);
        System.out.println("Before start");
        AllThread();
        t1.start();
        // Thread tt2=new Thread(t2,"t2");
        // tt2.start();
        t2.start();
        // t3.start();
        // t4.start();
        System.out.println("After start");
        AllThread();

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
        // System.out.println(Thread.currentThread().getName() + "  " + Thread.currentThread().getPriority() + ": " + n);
        System.out.println("Current Thread is "+Thread.currentThread().getName()+" :"+n);
        AllThread();
        long j = 0;
        for (long i = 0; i < 1000; i++) {
            j += i;
        }
        try {
        Thread.sleep(100);
        } catch (Exception e) {
        // TODO: handle exception
        e.printStackTrace();
        }
        Thread.yield();

    }

    public synchronized static void AllThread() {
        ThreadGroup currG=Thread.currentThread().getThreadGroup();
        Thread[] ts=new Thread[currG.activeCount()];
        currG.enumerate(ts);
        for (Thread thread : ts) {
            System.out.println(thread.getName()+" --States----> "+thread.getState());
        }
        System.out.println();
    }

}
class thread2 implements Runnable{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		for (int i = 0; i < 5; i++) {
            thread.compute();
        }
	}
    
}