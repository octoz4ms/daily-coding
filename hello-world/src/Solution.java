import java.util.Comparator;
import java.util.PriorityQueue;

public class Solution {
    public static void main(String[] args) {
        Comparator<Integer> comparator = new Comparator<>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        };

        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>(comparator);
        priorityQueue.add(3);
        priorityQueue.add(7);
        priorityQueue.add(10);
        priorityQueue.add(2);
        System.out.println(priorityQueue);

        while (!priorityQueue.isEmpty()){
            System.out.println(priorityQueue.poll());
        }

        System.out.println(priorityQueue);
    }

}