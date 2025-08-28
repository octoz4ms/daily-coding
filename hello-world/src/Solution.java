public class Solution {
    public static void main(String[] args) {
        ListNode listNode = swapPairs(ListNode.createListNode(new int[]{1, 2, 3, 4}));
//        ListNode.printListNode(listNode);

    }

    /**
     * 交换链表中相邻的节点
     *
     * @param head 链表的头节点
     * @return 交换后的链表的头节点
     */
    public static ListNode swapPairs(ListNode head) {
        // 创建一个虚拟节点，方便处理头节点的交换
        ListNode prev = new ListNode(-1);
        prev.next = head;
        // 初始化当前节点和下一个节点
        ListNode curr = head;
        ListNode next = (head != null) ? head.next : null;

        // 遍历链表，直到没有下一个节点
        while (next != null) {
            // 打印当前链表状态（可根据实际需要保留或移除此行）
            ListNode.printListNode(head);

            // 交换当前节点和下一个节点
            curr.next = next.next;
            next.next = curr;
            prev.next = next;

            // 移动prev、curr、next指针，准备下一轮交换
            prev = prev.next.next;
            curr = prev.next;
            next = (curr != null) ? curr.next : null;
        }
        // 返回交换后的链表头节点
        return head;
    }
}