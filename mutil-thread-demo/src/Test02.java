public class Test02 {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + ": " + "第二种创建线程的方式");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },"myThread02");
        thread.start();
        System.out.println("会阻塞吗？");
    }
}
