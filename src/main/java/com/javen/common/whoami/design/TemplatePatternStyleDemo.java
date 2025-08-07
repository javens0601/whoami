package com.javen.common.whoami.design;

/**
 * 模版模式
 * 在模板模式（Template Pattern）中，一个抽象类公开定义了执行它的方法的方式/模板。它的子类可以按需要重写方法实现，但调用将以抽象类中定义的方式进行。这种类型的设计模式属于行为型模式。
 *
 * 解决在多个子类中重复实现相同的方法的问题，通过将通用方法抽象到父类中来避免代码重复。
 */
abstract class Game {
    abstract void playInit();
    abstract void playStart();
    abstract void playEnd();

    // 模板方法设置为 final，这样它就不会被重写
    public final void play() {
        playInit();

        playStart();

        playEnd();
    }
}

class FootBall extends Game {
    @Override
    void playInit()
    {
        System.out.println(this.getClass().getSimpleName() + "\tplayInit");
    }

    @Override
    void playStart()
    {
        System.out.println(this.getClass().getSimpleName() + "\tplayStart");
    }

    @Override
    void playEnd()
    {
        System.out.println(this.getClass().getSimpleName() + "\tplayEnd");
    }
}

class Swimming extends Game {
    @Override
    void playInit()
    {
        System.out.println(this.getClass().getSimpleName() + "\tplayInit");
    }

    @Override
    void playStart()
    {
        System.out.println(this.getClass().getSimpleName() + "\tplayStart");
    }

    @Override
    void playEnd()
    {
        System.out.println(this.getClass().getSimpleName() + "\tplayEnd");
    }
}

public class TemplatePatternStyleDemo
{
    public static void main(String[] args)
    {
        FootBall footBall = new FootBall();
        footBall.play();

        Swimming swimming = new Swimming();
        swimming.play();
    }
}
