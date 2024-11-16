package com.example.ssd.utils;

//public class RedisDelayQueueConsumer implements Runnable {

//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @Override
//    public void run() {
//        while (true) {
//            Set<Object> delayQueue = redisTemplate.opsForZSet().rangeByScore("delayQueue", 0, System.currentTimeMillis());
//            if (!delayQueue.isEmpty()) {
//                String task = (String) delayQueue.iterator().next();
//                redisTemplate.opsForZSet().remove("delayQueue", task);
//                // 处理任务
//                System.out.println("Processing task: " + task);
//            } else {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }
//}
