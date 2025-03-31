import java.util.concurrent.locks.ReentrantLock;

public class HelloWorld {

    // 普通 ThreadLocal
    public static final ThreadLocal<String> normalThreadLocal = new ThreadLocal<>();

    // InheritableThreadLocal
    public static final InheritableThreadLocal<String> inheritableThreadLocal = new InheritableThreadLocal<>();

    public static final ReentrantLock lock = new ReentrantLock();

    public static volatile Integer num = 0;

    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(() -> {
            while (num == 0) {
            }
            System.out.println("--");

        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            num++;
        });

        t1.start();
        t2.start();

    }
}
