package com.javen.common.whoami.service;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/17 08:21
 */
class Ref {
    @Override
    protected void finalize() throws Throwable
    {
        System.out.println("invoke finalize");
    }
}
public class ReferenceService
{
    public static void main(String[] args)
    {
        storageReference();
    }

    // 强引用
    private static void storageReference() {
        Ref ref = new Ref();
        System.out.println(ref);

        ref = null; // 将引用置为null
        System.gc();
        System.out.println(ref);
    }

}
