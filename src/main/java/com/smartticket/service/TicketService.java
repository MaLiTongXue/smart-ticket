package com.smartticket.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.smartticket.entity.Ticket;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 工单业务接口。
 *
 * 继承 IService<Ticket> 之后，就有了这些方法（不用自己写）：
 *   getById / save / updateById / removeById / page / count / list ...
 */
@Service
public interface TicketService extends IService<Ticket> {

    /** 分页查询，支持按状态筛选 + 按标题/客户名模糊搜索 */
    IPage<Ticket> pageQuery(Integer pageNum, Integer pageSize, Integer status, String keyword);

    /** 新建工单：自动生成工单编号、补默认值 */
    Ticket createTicket(Ticket ticket);

    /** 查询详情，查不到就抛业务异常 */
    Ticket getDetail(Long id);

    /** 修改工单状态 */
    void updateStatus(Long id, Integer status);

    /** 删除工单（逻辑删除） */
    void deleteTicket(Long id);

    /** 首页统计（Day 11 再重写缓存部分，今天先照抄原版） */
    Map<String, Object> stats();
}
