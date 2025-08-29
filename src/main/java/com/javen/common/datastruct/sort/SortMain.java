package com.javen.common.datastruct.sort;

import java.util.Arrays;

/**
 * @Description
 * @Author: Javen
 * @CreateTime: 2025/8/28 17:35
 */
public class SortMain {

    public static void main(String[] args) {
        int[] nums = {2,6,1,3,9,5,2,6,8,7};

        //selectionSort(nums); // 选择排序
        //bubbleSort(nums); // 冒泡排序
        //bubbleSortWithFlag(nums); // 冒泡排序 优化
        //insertionSort(nums); // 插入排序
        //quickSort(nums, 0, nums.length-1); // 快速排序
        quickSort2(nums, 0, nums.length-1); // 快速排序

        Arrays.stream(nums).forEach(System.out::print);

    }

    // 选择排序 不稳定排序
    private static void selectionSort(int[] nums) {
        //选择每一轮的最小元素与第一个元素进行交换
        for (int i = 0; i < nums.length; i++) {
            int minIdx = i;
            for (int j = i+1; j < nums.length; j++) {
                if (nums[j] < nums[minIdx]) {
                    minIdx = j;
                }
            }
            int tmp = nums[i];
            nums[i] = nums[minIdx];
            nums[minIdx] = tmp;
        }
    }

    // 冒泡排序 稳定排序
    private static void bubbleSort(int nums[]) {
        // 和相邻的元素进行比较并交换，每一次循环之后，将最大的元素会放在末尾
        for (int i = nums.length - 1; i >= 0; i--) {
            for (int j = 0; j < i; j++) {
                if (nums[j] > nums[j + 1]) {
                    int tmp = nums[j];
                    nums[j] = nums[j+1];
                    nums[j+1] = tmp;
                }
            }
        }
    }

    // 冒泡排序优化， 增加是否交换的标志，如果没有发生交换，说明已经是有序的，直接返回
    private static void bubbleSortWithFlag(int nums[]) {
        // 和相邻的元素进行比较并交换，每一次循环之后，将最大的元素会放在末尾
        for (int i = nums.length - 1; i >= 0; i--) {
            boolean flag = false;
            for (int j = 0; j < i; j++) {
                if (nums[j] > nums[j + 1]) {
                    int tmp = nums[j];
                    nums[j] = nums[j+1];
                    nums[j+1] = tmp;
                    flag = true;
                }
            }

            if (!flag) {
                break; // 此轮“冒泡”未交换任何元素，直接跳出
            }
        }
    }

    // 插入排序 在未排序的区间选择一个基准元素，将该元素与其左侧已排序区间的元素逐一比较大小，并将该元素插入正确的位置
    private static void insertionSort(int nums[]) {
        // 外循环：已排序区间为 [0, i-1]
        for (int i = 1; i < nums.length; i++) {
            int base = nums[i];
            int j = i-1;

            // 内循环：将 base 插入到已排序区间 [0, i-1] 中的正确位置
            while (j >= 0 && nums[j] > base) {
                // 将 nums[j] 向右移动一位
                nums[j+1]  = nums[j];
                j--;
            }

            // 将 base 赋值到正确位置
            nums[j+1] = base;
        }
    }

    // 快速排序 递归
    // 是一种基于分治策略的排序算法，核心操作是 哨兵划分
    // 选择数组中的某个元素作为“基准数”，将所有小于基准的数的元素移到左侧，将大于基准的数的元素移到右侧
    private static void quickSort(int[] nums, int left, int right) {
        if (left >= right) {
            return;
        }
        int partition = partition(nums, left, right);
        quickSort(nums, left, partition - 1);
        quickSort(nums,partition + 1, right);
    }

    // 递归深度优化 仅对较短的子数组进行递归
    // 尾递归优化
    private static void quickSort2(int[] nums, int left, int right) {
        // 子数组长度为 1 时终止
        while (left < right) {
            int partition = partition(nums, left, right);
            // 对两个子数组中较短的数组执行快速排序
            if (partition - left < right - partition) {
                quickSort2(nums, left, partition - 1);
                left = partition + 1;
            }
            else {
                quickSort2(nums, partition + 1, right);
                right = partition - 1;
            }
        }
    }

    private static int partition(int[] nums, int left, int right) {
        // 以 nums[left] 为基准数
        int i = left, j = right;
        while (i < j) {
            // 从右向左找首个小于基准数的元素
            while (i < j && nums[left] <= nums[j]) {
                j--;
            }
            // 从左向右找首个大于基准数的元素
            while (i< j && nums[left] >= nums[i]) {
                i++;
            }
            // 交换
            swap(nums, i, j);
        }
        // 将基准数交换至两子数组的分界线
        swap(nums, i, left);
        // 返回基准数的索引
        return i;
    }

    private static void swap(int[] nums, int i, int j) {
        int tmp = nums[i];
        nums[i] = nums[j];
        nums[j] = tmp;
    }


    // 归并排序
    // 一种基于分治策略的排序算法
    // 划分阶段：通过递归不断地将数组从中点处分开，将长数组的排序问题转换成短数组的排序问题
    // 合并阶段：当子数组的长度为1时终止划分，开始合并，持续地将左右2个较短的有序数组合并为一个较长的有序数组，直至结束

    // 链表排序：对于链表，归并排序相较于其他排序算法具有显著优势，可以将链表排序任务的空间复杂度优化至O(1)
    //
    //划分阶段：可以使用“迭代”替代“递归”来实现链表划分工作，从而省去递归使用的栈帧空间。
    //合并阶段：在链表中，节点增删操作仅需改变引用（指针）即可实现，因此合并阶段（将两个短有序链表合并为一个长有序链表）无须创建额外链表
}
