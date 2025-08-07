package com.javen.common.whoami.design;

import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * <h3>享元模式</h3>
 * 享元模式（Flyweight Pattern）主要用于减少创建对象的数量，以减少内存占用和提高性能。这种类型的设计模式属于结构型模式，它提供了减少对象数量从而改善应用所需的对象结构的方式。
 * 享元模式尝试重用现有的同类对象，如果未找到匹配的对象，则创建新对象
 * <p><b>主要解决问题:</b></p>
 * <p>
 *     避免因创建大量对象而导致的内存溢出问题。
 *     通过共享对象，提高内存使用效率
 * </p>
 *
 * 在创建大量相似对象时考虑使用享元模式。
 *
 * 享元工厂（Flyweight Factory）:
 * 负责创建和管理享元对象，通常包含一个池（缓存）用于存储和复用已经创建的享元对象。
 *
 * 具体享元（Concrete Flyweight）:
 * 实现了抽象享元接口，包含了内部状态和外部状态。内部状态是可以被共享的，而外部状态则由客户端传递。
 *
 * 抽象享元（Flyweight）:
 * 定义了具体享元和非共享享元的接口，通常包含了设置外部状态的方法。
 *
 * 客户端（Client）:
 * 使用享元工厂获取享元对象，并通过设置外部状态来操作享元对象。客户端通常不需要关心享元对象的具体实现。
 */

interface Shape {
    void draw();
}

class Circle implements Shape {
    private String color;

    @Setter
    private int x;
    @Setter
    private int y;
    @Setter
    private int radius;

    public Circle(String color) {
        this.color = color;
    }

    @Override
    public String toString()
    {
        return "Circle{" +
                "color='" + color + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", radius=" + radius +
                '}';
    }

    @Override
    public void draw()
    {
        /*System.out.println("Circle{" +
                "color='" + color + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", radius=" + radius +
                '}');*/
        System.out.println(this);
    }
}

class ShapeFactory {
    public static final Map<String, Shape> circleMap = new HashMap<>();

    public static Shape getCircle(String color) {
        Circle circle = (Circle) circleMap.get(color);
        if (circle == null) {
            circle = new Circle(color);
            circleMap.put(color, circle);
            System.out.println("create circle of color: " + color);
        }
        return circle;
    }

}
public class FlyweightPatternStyleDemo
{
    private static final String colors[] =
            { "Red", "Green", "Blue", "White", "Black" };
    public static void main(String[] args)
    {
        for(int i=0; i < 20; ++i) {
            Circle circle =
                    (Circle)ShapeFactory.getCircle(getRandomColor());
            circle.setX(getRandomX());
            circle.setY(getRandomY());
            circle.setRadius(100);
            circle.draw();
        }

        System.out.println(ShapeFactory.circleMap);
    }

    private static String getRandomColor() {
        return colors[(int)(Math.random()*colors.length)];
    }
    private static int getRandomX() {
        return (int)(Math.random()*100 );
    }
    private static int getRandomY() {
        return (int)(Math.random()*100);
    }
}
