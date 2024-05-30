import java.util.Stack;

public class Solution {
    /**
     * 代码中的类名、方法名、参数名已经指定，请勿修改，直接返回方法规定的值即可
     *
     *
     * @param pushV int整型一维数组
     * @param popV int整型一维数组
     * @return bool布尔型
     */
    public static boolean IsPopOrder (int[] pushV, int[] popV) {

        Stack<Integer> stack = new Stack<>();
        int i = 0;
        for (int p : pushV) {
            stack.add(p);
            while (!stack.isEmpty() && p == popV[i]){
                stack.pop();
                i++;
            }
        }
        return stack.isEmpty();
    }
}