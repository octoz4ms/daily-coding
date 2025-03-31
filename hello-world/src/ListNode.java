import java.util.ArrayList;

public class ListNode {
    int val;
    ListNode next = null;

    public ListNode(int val) {
        this.val = val;
    }

    public static ListNode createListNode(int[] array) {
        ListNode head = new ListNode(array[0]);
        ListNode point = head;
        for (int i = 1; i < array.length; i++) {
            point.next = new ListNode(array[i]);
            point = point.next;
        }
        return head;
    }

    public static void printListNode(ListNode head) {
        ArrayList<Integer> list = new ArrayList<>();
        while (head != null) {
            list.add(head.val);
            head = head.next;
        }
        System.out.println(list);
    }

    public void hello() {

    }
}