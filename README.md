# ThreadDemo.java那些事

#### 一、代码与结果

> 测试平台及工具
>
> - OS：
>   - intel 64 family 6 model 85 steping 10 genuineintel 2500MHz
>   - Aliyun Linux release 2.1903 LTS (Hunting Beagle)
> - Java
>   - java version "1.8.0_231"
>   - Java(TM) SE Runtime Environment (build 1.8.0_231-b11)
>   - Java HotSpot(TM) 64-Bit Server VM (build 25.231-b11, mixed mode)
> - 编辑器：Visual Studio Code + Remote SSH

###### ThreadDemo.java

```java
public class thread extends Thread {
    thread(ThreadGroup a,String name) {
        super(a,name);
    }

    public void run() {
        for (int i = 0; i < 5; i++) {
            compute();
        }
    }

    public static void main(String[] args) {
        ThreadGroup top=Thread.currentThread().getThreadGroup();
        thread t1 = new thread(top,"t1");
        thread t2 = new thread(top,"t2");
        t1.start();
        t2.start();
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
        System.out.println("Current Thread is "+Thread.currentThread().getName()+" :"+n);
        long j = 0;
        for (long i = 0; i < 1000; i++) {
            j += i;
        }
        try {
        	Thread.sleep(100);
        } catch (Exception e) {
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
```

###### 输出结果

会发现线程总是在执行结束一个后在进入下一个。

```
Current Thread is main :1
Current Thread is main :2
Current Thread is main :3
Current Thread is main :4
Current Thread is main :5
Current Thread is t2 :1
Current Thread is t2 :2
Current Thread is t2 :3
Current Thread is t2 :4
Current Thread is t2 :5
Current Thread is t1 :1
Current Thread is t1 :2
Current Thread is t1 :3
Current Thread is t1 :4
Current Thread is t1 :5
```



#### 二、线程调度原理

java中有以下几种线程状态，一个线程在任何一个时刻只允许有一个状态，等待与无限等待记为一种：



![0001](pic/%E6%9C%AA%E5%91%BD%E5%90%8D/0001.jpg)



1. **新建(New)**：创建后尚未启动的线程。
2. **运行(Runnable)**：Runnable包括操作系统线程状态中的Running和Ready，也就是处于此状态的线程有可能正在执行，也有可能等待CPU为它分配执行时间。线程对象创建后，其他线程调用了该对象的start()方法。该状态的线程位于“可运行线程池”中，变得可运行，只等待获取CPU的使用权。
3. **无限期等待(Waiting)**：该状态下线程不会被分配CPU执行时间，要等待被其他线程显式唤醒。如没有设置timeout的object.wait()方法和Thread.join()方法，以及LockSupport.park()方法。
4. **限期等待(Timed Waiting)**：不会被分配CPU执行时间，不过无须等待被其他线程显式唤醒，在一定时间之后会由系统自动唤醒。如Thread.sleep()，设置了timeout的object.wait()和thread.join()，LockSupport.parkNanos()以及LockSupport.parkUntil()方法。
5. **阻塞（Blocked）**:正在阻塞的线程在等待着获取到一个排他锁，或者在被唤醒重新获得这个锁时产生。
6. **结束(Terminated)**：线程执行完了或者因异常退出了run()方法，该线程结束生命周期。

#### 三、ObjectMonitor

Java中的每个对象都有一个ObjectMonitor类，作为其锁机制的实现方式。在Java HotSpot中，其生成函数如下：

```c++
ObjectMonitor() {
    _header       = NULL;
    _count        = 0;
    _waiters      = 0,
    _recursions   = 0;
    _object       = NULL;
    _owner        = NULL;
    _WaitSet      = NULL;
    _WaitSetLock  = 0 ;
    _Responsible  = NULL ;
    _succ         = NULL ;
    _cxq          = NULL ;
    FreeNext      = NULL ;
    _EntryList    = NULL ;
    _SpinFreq     = 0 ;
    _SpinClock    = 0 ;
    OwnerIsThread = 0 ;
    _previous_owner_tid = 0;
  }
```

- WaitSet存放通过wait()进入阻塞的线程
- EntryList存放被唤醒的线程
- cxq存放申请该对象锁失败的线程，在默认情况下是LIFO的双向队列
- owner是当前拥有对象锁的线程
- Responsible是最近一次申请锁失败的线程，一般情况下指向cxq的头部

#### 四、结果详解



1. jvm启动,参数为

   ```
   -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=localhost:port -Dfile.encoding=UTF-8 -cp /root/.vscode-server/data/bin ThreadDemo 
   ```

   

2. 启动main进程，main进入**运行状态**

3. main进程逐条执行语句，在创建两个线程（记为t1和t2）后，两个线程进入**NEW**

4. main进程执行到**start()**语句后，t1和t2进入可运行线程池，即**RUNNING**，等待分配时间片后，t1和t2就运行

5. 在t1和t2进行start()和run()的时候，main也在继续运行，进入了computer()。由于compute()是静态方法，Synchronized的实现是通过**ThreadDemo类**实现的。t1和t2申请这个锁失败，进入**BLOCKED**

6. main进程在compute()中运行到sleep()进入**BLOCKED**，但由于sleep()不会释放锁，因此其他两个线程依然处于BLOCKED

7. main在sleep()结束后进入**就绪状态**，然后进入**运行状态**，执行**Thread.yield()**，让出CPU资源，但不释放锁，最终由main线程获得CPU资源，然后结束compute()

8. 此时由三个线程一起竞争锁，main获得compute()的锁，重新进入代码执行，其他线程依旧在**BLOCKED**中

9. 在main线程执行结束进入**TERMINATED**后，由其他线程通过竞争获得锁，然后执行至最后

10. 最后一个线程结束后进入**TERMINATED**

11. 退出JVM

#### 四、运行结果解释与验证

- 第5步时，如果main在进入compute()前被其他线程抢先，输出就会改变顺序。在start()后增加Thread.sleep(1)，验证如下：

  ```
  Current Thread is t1 :1
  Current Thread is t1 :2
  Current Thread is t1 :3
  Current Thread is t1 :4
  Current Thread is t1 :5
  Current Thread is main :1
  Current Thread is main :2
  Current Thread is main :3
  Current Thread is main :4
  Current Thread is main :5
  Current Thread is t2 :1
  Current Thread is t2 :2
  Current Thread is t2 :3
  Current Thread is t2 :4
  Current Thread is t2 :5
  ```

  在测试的15次中，顺序为 $t1\Longrightarrow main\Longrightarrow t2$ 的有9组，顺序为 $t1\Longrightarrow main\Longrightarrow t2$ 的有6组。


- 第6步，线程不会释放锁，可以在compute()的sleep()前后打印所有线程状态，代码如下：

  ```java
  public synchronized static void AllThread() {
          ThreadGroup currG=Thread.currentThread().getThreadGroup();
          Thread[] ts=new Thread[currG.activeCount()];
          currG.enumerate(ts);
          for (Thread thread : ts) {
              System.out.println(thread.getName()+" --States----> "+thread.getState());
          }
          System.out.println();
      }
  ```

  验证如下：

  ```
  Current Thread is t1 :4
  ==========================
  main --States----> BLOCKED
  t1 --States----> RUNNABLE
  t2 --States----> BLOCKED
  
  main --States----> BLOCKED
  t1 --States----> RUNNABLE
  t2 --States----> BLOCKED
  ```
  
  测试的10组中，在sleep()前后其他线程的**BLOCKED**状态不会改变。
  
- 第8步中，main线程退出Monitor后，Monitor会去cxq队列中寻找头部元素。如果在Monitor寻找之前，main线程重新申请了锁，就会更新Responsible，Monitor再去寻找时，就会把锁交给main线程。

  如果在ThreadDemo.java文件中每一次compute()之后休息1微秒，输出结果如下：
  
  ```
  Current Thread is main :1
  Current Thread is t2 :1
  Current Thread is t1 :1
  Current Thread is t2 :2
  Current Thread is main :2
  Current Thread is t2 :3
  Current Thread is t1 :2
  Current Thread is t2 :4
  Current Thread is main :3
  Current Thread is t2 :5
  Current Thread is t1 :3
  Current Thread is main :4
  Current Thread is t1 :4
  Current Thread is main :5
  Current Thread is t1 :5
  ```
  
  可以发现没有一个线程可以连续获得compute()的锁。
  
- 在compute()函数起始添加System.nanoTime()，观察输出：

  ```
  At 1295416633661386    Current Thread is t1 :1
  At 1295416736579857    Current Thread is t1 :2
  At 1295416836980811    Current Thread is t1 :3
  At 1295416937331809    Current Thread is t1 :4
  At 1295417037702434    Current Thread is t1 :5
  At 1295417138115853    Current Thread is t2 :1
  At 1295417238593940    Current Thread is t2 :2
  At 1295417338957621    Current Thread is t2 :3
  At 1295417439322622    Current Thread is t2 :4
  At 1295417539728712    Current Thread is t2 :5
  At 1295417640141299    Current Thread is main :1
  At 1295417740569277    Current Thread is main :2
  At 1295417840979466    Current Thread is main :3
  At 1295417941350934    Current Thread is main :4
  At 1295418041759788    Current Thread is main :5
  ```

  可以看出，在除去compute()中sleep()等待的100微秒外，执行其他代码所需时间不超过半

#### 五、参考文献

1. 计算机操作系统，第四版，汤小丹、梁红兵等
2. Thinking in Java，Fourth Edition，Bruce Eckel
3. 让你彻底理解Synchronized，你听__，https://www.jianshu.com/p/d53bf830fa09
4. 深入理解Java内存模型（二）——重排序，程晓明，https://www.infoq.cn/article/java-memory-model-2/
5. OpenJDK，ObjectMonitor.cpp，http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/file/ee4d5e999653/src/share/vm/runtime/objectMonitor.cpp

#### 六、作者

关凯宁，南开大学2017级本科生，个人邮箱：1710028@mail.nankai.edu.cn
