public class Test01 {
    public static void main(String[] args) {
        Thread thread = new Thread("myThread01") {
            public void run() {
                System.out.println(Thread.currentThread().getName() + ": " + "第一种创建线程的方式！");
            }
        };
        thread.start();
    }
}
