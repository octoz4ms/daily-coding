
public class Solution {
    /**
     * 代码中的类名、方法名、参数名已经指定，请勿修改，直接返回方法规定的值即可
     *
     *
     * @param nums int整型一维数组
     * @return int整型
     */
    public static int minNumberInRotateArray (int[] nums) {
        // write code here
        int left = 0;
        int right = nums.length - 1;
        while (left <= right) {
            int mid = (left + right) / 2;
            if(nums[mid] == nums[right]) {
                right --;
            }else if(nums[mid] > nums[right]) {
                left = mid + 1;
            }else {
                right = mid - 1;
            }
        }
        return nums[left];

    }

    public static void main(String[] args) {
        int i = minNumberInRotateArray(new int[]{1, 0, 1, 1, 1});
        System.out.println(i);
    }
}