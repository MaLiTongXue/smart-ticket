import java.util.*;
import java.util.stream.*;

/**
 * 方法引用（Method Reference）演示
 * 运行：java MethodRefDemo.java
 */
public class MethodRefDemo {

    // ============ 一个普通的实体类 ============
    static class Ticket {
        private Long id;
        private String title;
        private Integer status;

        Ticket(Long id, String title, Integer status) {
            this.id = id;
            this.title = title;
            this.status = status;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public Integer getStatus() { return status; }

        @Override
        public String toString() {
            return "Ticket{id=" + id + ", title='" + title + "', status=" + status + "}";
        }
    }

    public static void main(String[] args) {
        List<Ticket> list = new ArrayList<>(Arrays.asList(
                new Ticket(1L, "登录不上", 2),
                new Ticket(2L, "订单没发货", 1),
                new Ticket(3L, "发票问题", 3)
        ));

        // ============================================================
        System.out.println("========== ① 用方法引用提取 title ==========");
        // ============================================================
        List<String> titles = list.stream()
                .map(Ticket::getTitle)          // ← 方法引用
                .collect(Collectors.toList());
        System.out.println("  结果: " + titles);
        System.out.println("  解释: Ticket::getTitle 等价于 t -> t.getTitle()");

        // ============================================================
        System.out.println();
        System.out.println("========== ② 等价的 lambda 写法 ==========");
        // ============================================================
        List<String> titles2 = list.stream()
                .map(t -> t.getTitle())         // ← lambda，做同一件事
                .collect(Collectors.toList());
        System.out.println("  结果: " + titles2);
        System.out.println("  两种写法结果完全一样: " + titles.equals(titles2));

        // ============================================================
        System.out.println();
        System.out.println("========== ③ 用方法引用按 status 排序 ==========");
        // ============================================================
        list.sort(Comparator.comparing(Ticket::getStatus));
        list.forEach(t -> System.out.println("  " + t));
        System.out.println("  解释: Comparator.comparing(Ticket::getStatus)");
        System.out.println("        等价于 (a, b) -> a.getStatus().compareTo(b.getStatus())");

        // ============================================================
        System.out.println();
        System.out.println("========== ④ 四种方法引用形式 ==========");
        // ============================================================
        System.out.println("  静态方法  :  Integer::parseInt     等价于 s -> Integer.parseInt(s)");
        System.out.println("  特定对象  :  System.out::println   等价于 x -> System.out.println(x)");
        System.out.println("  任意对象  :  Ticket::getStatus     等价于 t -> t.getStatus()   ← 你问的这个");
        System.out.println("  构造器    :  Ticket::new           等价于 (a,b,c) -> new Ticket(a,b,c)");

        // ============================================================
        System.out.println();
        System.out.println("========== ⑤ 关键：方法引用不是【调用】，是【传递】 ==========");
        // ============================================================
        System.out.println("  Ticket::getStatus   →  没有调用 getStatus，只是把【这个方法本身】当参数传");
        System.out.println("  ticket.getStatus()  →  这才是真的【调用】");
    }
}
