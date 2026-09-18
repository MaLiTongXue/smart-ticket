/**
 * static 和 非static 的区别演示
 * 运行：java StaticDemo.java
 */
public class StaticDemo {

    // ============ 有 static：全类共用一份 ============
    static int staticCount = 0;
    static final String CONSTANT = "我是常量（static final）";

    // ============ 没有 static：每个对象一份 ============
    int instanceCount = 0;
    final String label = "我只是 final（每个对象一份）";

    public StaticDemo() {
        staticCount++;      // 往"全类共用"的那个上 +1
        instanceCount++;    // 往"自己这个对象"的上 +1
    }

    public static void main(String[] args) {
        System.out.println("创建对象之前：");
        System.out.println("  staticCount = " + staticCount);
        System.out.println();

        StaticDemo a = new StaticDemo();
        StaticDemo b = new StaticDemo();
        StaticDemo c = new StaticDemo();

        System.out.println("创建了 3 个对象之后：");
        System.out.println();

        System.out.println("【有 static 的字段】：");
        System.out.println("  staticCount = " + staticCount);
        System.out.println("  ↑ 3 个对象都往【同一个】计数器上加，所以是 3");
        System.out.println();

        System.out.println("【没有 static 的字段】：");
        System.out.println("  a.instanceCount = " + a.instanceCount);
        System.out.println("  b.instanceCount = " + b.instanceCount);
        System.out.println("  c.instanceCount = " + c.instanceCount);
        System.out.println("  ↑ 每个对象有自己的那一份，各算各的，所以都是 1");
        System.out.println();

        System.out.println("【访问方式也不一样】：");
        System.out.println("  StaticDemo.CONSTANT  = " + StaticDemo.CONSTANT);
        System.out.println("  ↑ static 字段直接用【类名】访问，不用 new 对象");
        System.out.println();
        System.out.println("  a.label = " + a.label);
        System.out.println("  ↑ 非 static 字段必须通过【对象】访问");
    }
}
