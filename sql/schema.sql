-- ============================================================
-- 智能客服工单系统 建表脚本
-- 使用方法：先启动 MySQL，然后执行
--   mysql -uroot -p < schema.sql
-- 或者在 Navicat / IDEA 的 Database 面板里直接运行本文件
-- ============================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS smart_ticket
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE smart_ticket;

-- 2. 工单表
DROP TABLE IF EXISTS ticket;
CREATE TABLE ticket (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no       VARCHAR(32)  NOT NULL                COMMENT '工单编号',
    title           VARCHAR(200) NOT NULL                COMMENT '标题',
    content         TEXT                                 COMMENT '问题描述',
    category        VARCHAR(50)                          COMMENT '分类（由 AI 自动归类）',
    priority        TINYINT      NOT NULL DEFAULT 2      COMMENT '优先级 1低 2中 3高',
    status          TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0待处理 1处理中 2已解决 3已关闭',
    customer_name   VARCHAR(50)                          COMMENT '客户姓名',
    customer_phone  VARCHAR(20)                          COMMENT '客户电话',
    ai_suggestion   TEXT                                 COMMENT 'AI 生成的回复建议',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客服工单表';

-- 3. 用户表（登录用）
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL                COMMENT '登录名',
    password    VARCHAR(100) NOT NULL                COMMENT '密码（MD5 加密后存储）',
    nickname    VARCHAR(50)                          COMMENT '昵称',
    role        VARCHAR(20)  NOT NULL DEFAULT 'AGENT' COMMENT '角色 ADMIN/AGENT',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- 4. 初始化一个管理员账号
--    用户名 admin，密码 123456
--    下面的字符串是 123456 经过 MD5 后的结果（e10adc3949ba59abbe56e057f20f883e）
INSERT INTO sys_user (username, password, nickname, role)
VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 'ADMIN');

-- 5. 插几条测试工单，方便页面一打开就有数据
INSERT INTO ticket (ticket_no, title, content, category, priority, status, customer_name, customer_phone)
VALUES
('TK20260910001', '登录不上系统', '输入账号密码后点登录没反应，换浏览器也不行。', '账号问题', 3, 0, '张三', '13800000001'),
('TK20260910002', '订单一直显示待发货', '昨天下午付的款，到现在还是待发货状态。', '订单问题', 2, 1, '李四', '13800000002'),
('TK20260910003', '发票怎么开', '公司需要报销，请问电子发票在哪里下载？', '发票问题', 1, 2, '王五', '13800000003');
