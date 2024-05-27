
public class Solution {
    public boolean VerifySquenceOfBST(int [] sequence) {
        return recur(sequence,0, sequence.length -1);
    }

    boolean  recur(int[] postorder, int i, int j) {
        if(i >= j) return true;
        int p = i;
        while (postorder[p] < postorder[j]) p++;
        int m = p;
        while (postorder[p] > postorder[j]) p++;
        return p == j && recur(postorder,i, m - 1) && recur(postorder, m + 1, j);
    }
}