package com.smartticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * 工单实体类，对应数据库的 ticket 表。
 * 字段名用驼峰（customerName），数据库用下划线（customer_name），
 * MyBatis-Plus 会自动转换，不用手写映射。
 */
@TableName("ticket")
public class Ticket {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号，由后端自动生成，不用前端传 */
    private String ticketNo;

    /** 标题，必填 */
    @NotBlank(message = "工单标题不能为空")
    private String title;

    /** 问题描述 */
    private String content;

    /** 分类，这个字段将来由 Python AI 服务自动填写 */
    private String category;

    /** 优先级：1 低，2 中，3 高 */
    private Integer priority;

    /** 状态：0 待处理，1 处理中，2 已解决，3 已关闭 */
    private Integer status;

    /** 客户姓名 */
    private String customerName;

    /** 客户电话 */
    private String customerPhone;

    /** AI 生成的回复建议 */
    private String aiSuggestion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0 未删除，1 已删除。加了 @TableLogic 后 deleteById 会变成 UPDATE */
    @TableLogic
    private Integer deleted;

    // ==================== getter / setter ====================
    // 没用 Lombok，是为了让你能直接看到每个字段怎么读怎么写，面试问起来心里有底

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getAiSuggestion() {
        return aiSuggestion;
    }

    public void setAiSuggestion(String aiSuggestion) {
        this.aiSuggestion = aiSuggestion;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
