import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class Test03 {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        FutureTask<Integer> task = new FutureTask<>(() -> {
            System.out.println("第三种创建线程的方式");
            Thread.sleep(5000);
            return 0;
        });
        Thread thread = new Thread(task,"myThread03");
        thread.start();
        System.out.println("会阻塞吗");
        System.out.println(task.get());
        System.out.println("会先执行吗");
    }
}
