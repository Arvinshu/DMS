package org.ls;

import org.mybatis.spring.annotation.MapperScan; // 引入 MapperScan
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 主应用程序启动类
 * 文件路径: src/main/java/org/ls/DepartmentManagementApplication.java
 */
@SpringBootApplication // 组合了 @Configuration, @EnableAutoConfiguration, @ComponentScan
@MapperScan("org.ls.mapper") // 指定 MyBatis Mapper 接口所在的包
public class DepartmentManagementApplication {

    /**
     * 主方法，应用程序入口点
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DepartmentManagementApplication.class, args);
        System.out.println("(^_^) 部门管理系统启动成功 (^_^) ");
        System.out.println("Swagger UI (if enabled): http://localhost:8080/swagger-ui/index.html"); // 假设端口是 8080
        System.out.println("Application URL: http://localhost:8080/");
    }

}
