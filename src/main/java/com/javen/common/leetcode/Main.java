package com.javen.common.leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author: Javen
 * @CreateTime: 2025/9/1 11:12
 */
public class Main {

    public static void main(String[] args) {

        String str = "abcabcdeeeacdefg";
        int l = lengthOfLongestSubstring(str);
        System.out.println(l);
    }

    // 两数之和
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i< nums.length; i++) {
            if (map.containsKey(target - nums[i])) {
                return new int[]{i, map.get(target - nums[i])};
            }
            map.put(nums[i], i);
        }
        return null;
    }

    // 字母异位词组合
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> map = new HashMap<>();
        for (String str : strs) {
            char[] charArray = str.toCharArray();
            Arrays.sort(charArray);
            String strSort = new String(charArray);
            List<String> strList = map.getOrDefault(strSort, new ArrayList());
            strList.add(str);
            map.put(strSort, strList);
        }
        return new ArrayList<>(map.values());
    }

    // 给定一个未排序的整数数组 nums ，找出数字连续的最长序列（不要求序列元素在原数组中连续）的长度。
    // 请你设计并实现时间复杂度为 O(n) 的算法解决此问题。
    public static int longestConsecutive(int[] nums) {
        if (nums.length == 0) {
            return 0;
        }
        Arrays.sort(nums);
        int span = 1;
        int maxSpan = 0;
        for (int i = 0; i < nums.length - 1; i++) {
            if (nums[i] == nums[i+1]) {
                continue;
            }
            if (nums[i] + 1  == nums[i+1]) {
                span++;
            } else {
                maxSpan = Math.max(maxSpan, span);
                span = 1;
            }
        }
        maxSpan = Math.max(maxSpan, span);
        return maxSpan;
    }

    // 移动0 双重循环 从后往前遍历
    public static void moveZeroes(int[] nums) {
        int length = nums.length;
        for (int i = length-1; i>=0; i--) {
            if (nums[i] == 0) {
                for (int j = i; j < length - 1; j++) {
                    nums[j] = nums[j+1];
                }
                nums[length-1] = 0;
            }
        }
    }

    // 双指针 交换
    public static void moveZeroes2(int[] nums) {
        int i=0,j=0;
        while (j < nums.length) {
            if (nums[j] != 0) {
                int tmp = nums[j];
                nums[j] = nums[i];
                nums[i] = tmp;

                i++;
            }
            j++;
        }
    }

    // 最大面积 =  长 x 宽
    public static int maxArea(int[] height) {
        int s = 0;
        int e = height.length;
        int area = 0;
        int maxArea = 0;

        while (s < e) {
            area = (e-s) * Math.min(height[e], height[s]);
            maxArea = Math.max(area, maxArea);
            if (height[e] < height[s]) {
                --e;
            } else {
                ++s;
            }
        }
        return maxArea;
    }

    // 三数之和
    public static List<List<Integer>> threeSum(int[] nums) {

        return null;
    }

    // 无重复字符的最长子串
    // 给定一个字符串 s ，请你找出其中不含有重复字符的 最长 子串 的长度。
    public static int lengthOfLongestSubstring(String s) {
        char[] charArray = s.toCharArray();
        int p1=0,p2=0;
        int maxSpan = 0;

        while (p2 <= charArray.length) {
            if (p2 == charArray.length) {
                maxSpan = Math.max(maxSpan, p2-p1);
            } else
            if (p1!=p2 && charArray[p1] == charArray[p2])  {
                maxSpan = Math.max(maxSpan, p2-p1);
                p1++;
            }
            p2++;
        }
        return maxSpan;
    }
}
