public class Solution {
    public static void main(String[] args){

    }
}



//import java.util.concurrent.*;
//
//public class Solution {
//    public static void main(String[] args) throws ExecutionException, InterruptedException {
//
//        // 继承Thread类的方式
//        Thread myThread = new MyThread();
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                System.out.println("继承Thread类的方式创建线程！");
//            }
//        };
//        thread.start();
//
//        // runnable方式
//        new Thread(() -> {
//            System.out.println("1、runnable方式创建线程！");
//        }).start();
//
//        MyRunnable myRunnable = new MyRunnable();
//        new Thread(myRunnable).start();
//
//        // callable方式
//        FutureTask<String> futureTask = new FutureTask<>(() -> {
//            return "2、callable方式创建线程！";
//        });
//        new Thread(futureTask).start();
//        System.out.println(futureTask.get());
//
//
//        // 线程池方式
//        ExecutorService pool = Executors.newFixedThreadPool(10);
//        // callable
//        Callable<String> task = () -> {
//            return "3、向线程池提交callable任务";
//        };
//        Future<String> future = pool.submit(task);
//        System.out.println(future.get());
//
//        // runnable
//        Runnable runnable = () -> {
//            System.out.println("4、向线程池提交runnable任务");
//        };
//        pool.submit(runnable);
//        pool.shutdown();
//    }
//}
//
//class MyThread extends Thread {
//    @Override
//    public void run() {
//        System.out.println("0、继承Thread类的方式创建线程！");
//    }
//}
//
//
//class MyRunnable implements Runnable {
//    @Override
//    public void run() {
//        System.out.println("runnable方式创建线程！");
//    }
//}
