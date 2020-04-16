# ThreadDemo.java那些事

![这里写图片描述](https://img-blog.csdn.net/20170823170657224?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvbmFsYW5taW5nZGlhbg==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

java中有5种线程状态，任意一个时刻一个线程只允许有一个状态，等待与无线等待记为一种：

1. **新建(New)**：创建后尚未启动的线程。
2. **运行(Runnable)**：Runnable包括操作系统线程状态中的Running和Ready，也就是处于此状态的线程有可能正在执行，也有可能等待CPU为它分配执行时间。线程对象创建后，其他线程调用了该对象的start()方法。该状态的线程位于“可运行线程池”中，变得可运行，只等待获取CPU的使用权。即在就绪状态的进程除CPU之外，其它的运行所需资源都已全部获得。
3. **无限期等待(Waiting)**：该状态下线程不会被分配CPU执行时间，要等待被其他线程显式唤醒。如没有设置timeout的object.wait()方法和Thread.join()方法，以及LockSupport.park()方法。
4. **限期等待(Timed Waiting)**：不会被分配CPU执行时间，不过无须等待被其他线程显式唤醒，在一定时间之后会由系统自动唤醒。如Thread.sleep()，设置了timeout的object.wait()和thread.join()，LockSupport.parkNanos()以及LockSupport.parkUntil()方法。
5. **阻塞（Blocked）**:线程被阻塞了。与等待状态的区别是：阻塞在等待着获取到一个排他锁，这个事件将在另外一个线程放弃这个锁的时候发生；而等待则在等待一段时间，或唤醒动作的发生。在等待进入同步区域时，线程将进入这种状态。
6. **结束(Terminated)**：线程执行完了或者因异常退出了run()方法，该线程结束生命周期。

补充

- 等待阻塞：运行的线程执行wait()方法，该线程会释放占用的所有资源，JVM会把该线程放入“等待池”中。进入这个状态后，是不能自动唤醒的，必须依靠其他线程调用notify()或notifyAll()方法才能被唤醒，即无限期等待。
- **同步阻塞：运行的线程在获取对象的同步锁时，若该同步锁被别的线程占用，则JVM会把该线程放入“锁池”中。**
- 其他阻塞：运行的线程执行sleep()或join()方法，或者发出了I/O请求时，JVM会把该线程置为阻塞状态。当sleep()状态超时、join()等待线程终止或者超时、或者I/O处理完毕时，线程重新转入就绪状态。即限期等待。

#### 结果详解

> 平台：Linux，2核，JDK1.8

![image-20200416100957283](pic/%E6%9C%AA%E5%91%BD%E5%90%8D/image-20200416100957283.png)

1. jvm启动，启动main进程，main进入**运行状态**

2. main进程逐条执行语句，在创建两个线程（记为t1和t2）后，两个线程进入**初始状态**

3. main进程执行到**start()**语句后，t1和t2进入可运行线程池，即**就绪状态**，如果机器是多核的话，此时t1和t2就会按顺序进入**运行状态**

4. 在t1和t2进行start()和run()的时候，main也在继续运行，进入了computer()。由于compute()同步锁的原因，t1和t2进入**锁池状态**

5. main进程在compute()中运行到sleep()把自己放入了**阻塞状态**，但由于sleep()不会释放锁，因此其他两个线程并不会从锁池状态中出来

6. main在sleep()结束后进入**就绪状态**，然后进入**运行状态**，执行**Thread.yield()**，让出CPU资源，不释放锁，并进入**就绪状态**，但其他线程拿不到锁，还是由main线程进入**运行状态**，然后结束compute()，再次返回**就绪状态**

7. 此时由于某种原因（比如就绪状态的线程比在锁池状态的线程更快的拿到compute()的锁），main重新进入**运行状态**，其他线程依旧在**锁池状态**中

8. 在main线程执行结束进入**终止状态**后，由其他线程随机获得锁，然后执行至**终止状态**

9. 最后一个线程结束后进入**终止状态**，JVM退出

#### 验证结果的分析

- 第3步时，如果main在进入compute()前被其他线程抢先，输出就会改变顺序。在start()后增加sleep()，验证如下：

  ![image-20200416111825742](pic/%E6%9C%AA%E5%91%BD%E5%90%8D/image-20200416111825742.png)

> 可以看出main线程不再是第一个。首先由t1和t2抢夺第一个，再由剩下的和main抢夺第二个。

- 第5步，线程不会释放锁，可以在compute()的sleep()后打印所有线程状态，代码如下：

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

  ![image-20200416113129376](pic/%E6%9C%AA%E5%91%BD%E5%90%8D/image-20200416113129376.png)

  ![image-20200416113211734](pic/%E6%9C%AA%E5%91%BD%E5%90%8D/image-20200416113211734.png)

  > 可以看出除去第一个外在sleep()前后除了当前线程外都处于**BLOCKED**
  >
  > 第一组的t2应该是处在run()方法中的第一行，即开始for循环的时候

- 关于第7步的“某种原因”，笔者并没有一个很好的验证方法，如果各位有好的思路，欢迎分享。但是其他平台上的不同结果可以由此来解释。

  如果机器运行快，在前一个线程compute()结束后进入就绪状态又进入运行状态，在其他线程还未获取锁的情况下，运行完for循环进入compute()。

  如果运行慢，前一个线程可能在运行for循环时被其它线程抢先进入compute()，从而输出结果产生了乱序。

- 对与优先度，只是一个参考，并不能保证线程的运行会按照优先度的顺序执行。如果想要让线程按顺序执行，要借助多个锁来完成。

  一个可能是在同个状态下的不同线程之间按优先度顺序执行，不同状态的线程之间没有可比性。

- 释放锁是Object级别的操作，只有wait()和join()会释放锁，wait()是Object的方法，join()是调用wait()实现的。

