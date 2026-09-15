import java.lang.reflect.Proxy;

// ===================================================================
//  代理模式演示 —— 重点回答一个问题：
//      "真正的业务逻辑，到底写在哪个对象里？"
//
//  运行：java ProxyDemo.java
// ===================================================================

interface UserService {
    void save(String name);
}

// ===================================================================
//  真实对象：业务逻辑【在这里】
// ===================================================================
class UserServiceImpl implements UserService {
    @Override
    public void save(String name) {
        // ★★★ 这就是"真正的逻辑"：保存用户 ★★★
        System.out.println("      >>> 保存用户: " + name + " （写数据库、发通知……）");
    }
}

// ===================================================================
//  静态代理：这里【没有】任何业务逻辑，只有"日志 + 转发"
// ===================================================================
class UserServiceStaticProxy implements UserService {

    private UserService target;                       // 它"抱住"了真实对象

    UserServiceStaticProxy(UserService target) {
        this.target = target;
    }

    @Override
    public void save(String name) {
        System.out.println("      [代理] 开始调用");
        target.save(name);                            // ← 转发！业务在 target 里面
        System.out.println("      [代理] 调用结束");
        // ⚠️ 注意：这个方法里【没有】"保存用户"这个业务，一行都没有
    }
}

// ===================================================================
public class ProxyDemo {

    public static void main(String[] args) {

        UserService real = new UserServiceImpl();

        // ============================================================
        System.out.println("========== ① 直接调【真实对象】==========");
        // ============================================================
        real.save("张三");

        // ============================================================
        System.out.println();
        System.out.println("========== ② 调【代理对象】（代理正常转发）==========");
        // ============================================================
        UserService proxy = new UserServiceStaticProxy(real);
        proxy.save("张三");

        // ============================================================
        System.out.println();
        System.out.println("========== ③ 一个【忘记转发】的代理 ==========");
        System.out.println("      （把 target.save(...) 那行去掉，模拟『代理自己实现逻辑』）");
        // ============================================================
        UserService brokenProxy = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class<?>[]{ UserService.class },
                (p, method, margs) -> {
                    System.out.println("      [代理] 开始调用");
                    // ★ 这里故意【不转发】★
                    System.out.println("      [代理] 调用结束");
                    return null;
                }
        );
        brokenProxy.save("张三");
        System.out.println();
        System.out.println("      ↑↑↑ 看到了吗？『保存用户: 张三』消失了！");
        System.out.println("          说明业务逻辑【不在代理里】，代理不转发就什么都不会发生。");

        // ============================================================
        System.out.println();
        System.out.println("========== ④ 换成 JDK 动态代理（同样是转发）==========");
        // ============================================================
        UserService dynamicProxy = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class<?>[]{ UserService.class },
                (p, method, margs) -> {
                    System.out.println("      [代理] 开始调用");
                    Object r = method.invoke(real, margs);        // ← 转发（反射版）
                    System.out.println("      [代理] 调用结束");
                    return r;
                }
        );
        dynamicProxy.save("李四");
        System.out.println();
        System.out.println("      动态代理生成的类名: " + dynamicProxy.getClass().getName());
        System.out.println("      （磁盘上不存在这个类，是运行时在内存里造的）");
    }
}
