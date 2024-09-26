import java.util.HashSet;

public class Solution {
    public static int climbStairs(int n) {
        if(n < 3) return n;
        return climbStairs(n - 1) + climbStairs(n - 2);
    }

    public static void main(String[] args) {
        int i = climbStairs(3);
        System.out.println(i);
    }

}