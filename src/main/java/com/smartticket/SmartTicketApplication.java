package com.smartticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类。
 * 直接右键 Run 这个类就能启动整个项目。
 */
@SpringBootApplication
// 告诉 MyBatis 去哪里找 Mapper 接口，这样就不用每个接口都写 @Mapper 注解了
@MapperScan("com.smartticket.mapper")
public class SmartTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTicketApplication.class, args);
        System.out.println("""

                ====================================================
                  智能客服工单系统启动成功！
                  接口地址: http://localhost:8080
                ====================================================
                """);
    }
}
