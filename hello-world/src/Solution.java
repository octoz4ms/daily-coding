

public class Solution {
    /**
     * 代码中的类名、方法名、参数名已经指定，请勿修改，直接返回方法规定的值即可
     *
     *
     * @param n int整型
     * @return int整型
     */
    public static int findNthDigit (int n) {
        // write code here
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= n; i++) {
            builder.append(i);
        }
        char[] charArray = builder.toString().toCharArray();
        return Character.getNumericValue(charArray[n]);
    }

    public static void main(String[] args) {
        System.out.println(findNthDigit(3));
    }
}