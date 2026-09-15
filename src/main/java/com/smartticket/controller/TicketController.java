package com.smartticket.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartticket.common.Result;
import com.smartticket.entity.Ticket;
import com.smartticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工单接口层。
 * 这一层只做三件事：接参数、调 Service、包返回值。业务逻辑一律不写在这里。
 * 面试问"Controller 和 Service 怎么分工"就答这个。
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    private final TicketService ticketService;

    // 构造器注入（比 @Autowired 字段注入更好，面试加分点：方便单元测试、能发现循环依赖）
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** 分页查询工单列表 */
    @GetMapping("/page")
    public Result<IPage<Ticket>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        System.out.println("        ********** [Controller] page() 方法执行了 **********");
        return Result.success(ticketService.pageQuery(pageNum, pageSize, status, keyword));
    }

    /** 查询工单详情 */
    @GetMapping("/{id}")
    public Result<Ticket> detail(@PathVariable Long id) {
        return Result.success(ticketService.getDetail(id));
    }

    /** 新建工单 */
    @PostMapping
    public Result<Ticket> create(@Valid @RequestBody Ticket ticket) {
        return Result.success(ticketService.createTicket(ticket));
    }

    /** 修改工单状态 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        ticketService.updateStatus(id, status);
        return Result.success();
    }

    /** 删除工单（逻辑删除，数据库里只是把 deleted 改成 1） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return Result.success();
    }

    /** 首页统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(ticketService.stats());
    }
}
