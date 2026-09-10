package com.smartticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.smartticket.common.BizException;
import com.smartticket.entity.Ticket;
import com.smartticket.mapper.TicketMapper;
import com.smartticket.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 工单业务实现。
 * 继承 ServiceImpl 后，baseMapper 就是 TicketMapper，可以直接用。
 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    /** 统计数据的缓存 key */
    private static final String STATS_CACHE_KEY = "ticket:stats";
    /** 缓存有效期 60 秒 */
    private static final long STATS_CACHE_SECONDS = 60;

    private final RedisTemplate<String, Object> redisTemplate;

    public TicketServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public IPage<Ticket> pageQuery(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        // 参数兜底，防止前端不传或传 0
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 构造查询条件
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Ticket::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            // 标题或者客户姓名包含关键字，两个条件用 or 连起来
            wrapper.and(w -> w.like(Ticket::getTitle, keyword)
                    .or().like(Ticket::getCustomerName, keyword));
        }
        // 新工单排前面
        wrapper.orderByDesc(Ticket::getCreateTime);

        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Ticket createTicket(Ticket ticket) {
        // 1. 生成工单编号：TK + 年月日 + 3 位随机数，例如 TK20260910001
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(100, 1000);
        ticket.setTicketNo("TK" + datePart + randomPart);

        // 2. 补默认值
        if (ticket.getStatus() == null) {
            ticket.setStatus(0);          // 默认待处理
        }
        if (ticket.getPriority() == null) {
            ticket.setPriority(2);        // 默认中优先级
        }
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        ticket.setDeleted(0);

        // 3. 存库。save() 是 IService 自带的方法
        this.save(ticket);

        // 4. 数据变了，统计缓存必须清掉，否则前端看到的是旧数字
        clearStatsCache();
        return ticket;
    }

    @Override
    public Ticket getDetail(Long id) {
        Ticket ticket = this.getById(id);
        if (ticket == null) {
            throw new BizException("工单不存在，id = " + id);
        }
        return ticket;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 3) {
            throw new BizException("状态值不合法，只能是 0/1/2/3");
        }
        Ticket ticket = this.getDetail(id);   // 顺便校验工单存在
        ticket.setStatus(status);
        ticket.setUpdateTime(LocalDateTime.now());
        this.updateById(ticket);

        clearStatsCache();
    }

    @Override
    public void deleteTicket(Long id) {
        this.getDetail(id);
        this.removeById(id);   // 因为有 @TableLogic，实际执行的是 UPDATE ... SET deleted = 1
        clearStatsCache();
    }

    @Override
    public Map<String, Object> stats() {
        // ---------- 1. 先查 Redis 缓存 ----------
        try {
            Object cached = redisTemplate.opsForValue().get(STATS_CACHE_KEY);
            if (cached != null) {
                log.debug("统计数据命中缓存");
                @SuppressWarnings("unchecked")
                Map<String, Object> cacheMap = (Map<String, Object>) cached;
                return cacheMap;
            }
        } catch (Exception e) {
            // Redis 挂了不能让整个接口挂掉，降级去查数据库就行
            log.warn("Redis 读取失败，降级查数据库：{}", e.getMessage());
        }

        // ---------- 2. 缓存没有，查数据库 ----------
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", this.count());
        map.put("pending", this.count(new LambdaQueryWrapper<Ticket>().eq(Ticket::getStatus, 0)));
        map.put("processing", this.count(new LambdaQueryWrapper<Ticket>().eq(Ticket::getStatus, 1)));
        map.put("resolved", this.count(new LambdaQueryWrapper<Ticket>().eq(Ticket::getStatus, 2)));
        map.put("closed", this.count(new LambdaQueryWrapper<Ticket>().eq(Ticket::getStatus, 3)));

        // ---------- 3. 写回缓存 ----------
        try {
            redisTemplate.opsForValue().set(STATS_CACHE_KEY, map, STATS_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 写入失败：{}", e.getMessage());
        }

        return map;
    }

    /** 删除统计缓存。数据一变动就调它，这是"缓存一致性"最朴素的解法（Cache Aside 模式） */
    private void clearStatsCache() {
        try {
            redisTemplate.delete(STATS_CACHE_KEY);
        } catch (Exception e) {
            log.warn("Redis 删除缓存失败：{}", e.getMessage());
        }
    }
}
