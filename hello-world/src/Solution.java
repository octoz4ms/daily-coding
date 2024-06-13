import java.util.HashSet;

public class Solution {

    /**
     * 代码中的类名、方法名、参数名已经指定，请勿修改，直接返回方法规定的值即可
     *
     *
     * @param array int整型一维数组
     * @return int整型一维数组
     */
    public static int[] FindGreatestSumOfSubArray (int[] array) {
        // write code here
        int[] dp = new int[array.length];
        dp[0] = array[0];
        int maxSum = dp[0];
        // 滑动区间
        int left = 0, right = 0;
        // 最大值区间
        int resL = 0, resR = 0;
        for (int i = 1; i < array.length; i++) {
            right++;
            dp[i] = Math.max(dp[i - 1] + array[i], array[i]);
            if(dp[i - 1] + array[i] < array[i]) {
                left = right;
            }
            if(dp[i] > maxSum || (dp[i] == maxSum &&((resR - resL) < (right - left)))) {
                maxSum = dp[i];
                resL = left;
                resR = right;
            }
        }
        System.out.println(resL);
        System.out.println(resR);
        int[] res = new int[resR - resL + 1];
        for (int i = resL; i <= resR; i++) {
            res[i - resL] = array[i];
        }
        return res;
    }

    public static void main(String[] args) {
        int[] array = {1};
        int[] ints = FindGreatestSumOfSubArray(array);
        for (int i : ints) {
            System.out.println(i);
        }
    }
}