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
 *
 * ⚠️ 下面 5 个方法（pageQuery 到 deleteTicket）的方法体是空的，你自己写。
 *    stats() 和 clearStatsCache() 已经写好了（今天的重点是那 5 个）。
 *
 * 写作方法：中文先行法
 *   第 1 步  看 TicketController，确认方法签名（已经给你了）
 *   第 2 步  用中文写出步骤（写在注释里）
 *   第 3 步  逐句翻译成代码
 *   第 4 步  跑测试验证
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

    // ================================================================
    //  ↓↓↓ 下面 5 个方法，自己写 ↓↓↓
    // ================================================================

    /**
     * 分页查询。
     *
     * 中文步骤：
     *   1. 参数兜底：pageNum 为 null 或 < 1 → 改成 1
     *                pageSize 为 null 或 < 1 或 > 100 → 改成 10
     *   2. 建一个空的条件构造器
     *   3. status 不为 null → 加"状态等于"条件
     *   4. keyword 有内容   → 加"标题或客户名包含"条件（记得加括号）
     *   5. 按 createTime 倒序
     *   6. 交给分页方法，返回结果
     *
     * 工具：
     *   new LambdaQueryWrapper<Ticket>()
     *   wrapper.eq(Ticket::getStatus, status)
     *   wrapper.and(w -> w.like(Ticket::getTitle, keyword)
     *                   .or().like(Ticket::getCustomerName, keyword))
     *   wrapper.orderByDesc(Ticket::getCreateTime())
     *   this.page(new Page<>(pageNum, pageSize), wrapper)
     */
    @Override
    public IPage<Ticket> pageQuery(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        // TODO 你自己写
        if(pageNum==null||pageNum<1){
            pageNum=1;
        }
        if(pageSize==null||pageSize>100||pageSize<1){
            pageSize=10;
        }
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        if(status!= null){
            wrapper.eq(Ticket::getStatus,status);
        }
        if(StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Ticket::getTitle, keyword).or().like(Ticket::getCustomerName, keyword));
        }
        wrapper.orderByDesc(Ticket::getCreateTime);



        return this.page(new Page<>(pageNum,pageSize),wrapper);
    }

    /**
     * 新建工单。
     *
     * 中文步骤：
     *   1. 生成工单编号（"TK" + yyyyMMdd + 3 位随机数）
     *   2. 补默认值：status=0、priority=2、createTime、updateTime、deleted=0
     *   3. 存库
     *   4. 清掉统计缓存
     *   5. 返回存好的对象
     *
     * 工具：
     *   LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
     *   ThreadLocalRandom.current().nextInt(100, 1000)
     *   this.save(ticket)
     *   clearStatsCache()
     */
    @Override
    public Ticket createTicket(Ticket ticket) {
        // TODO 你自己写
        // 看了提示写的很多
        String depart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randPart = ThreadLocalRandom.current().nextInt(100, 1000);
        ticket.setTicketNo("TK"+depart+randPart);
        if(ticket.getStatus()==null){
            ticket.setStatus(0);
        }
        if(ticket.getPriority()==null){
            ticket.setPriority(2);
        }
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        ticket.setDeleted(0);
        this.save(ticket);
        this.clearStatsCache();
        return ticket;
    }

    /**
     * 查询详情。
     *
     * 中文步骤：
     *   1. 根据 id 查工单
     *   2. 查不到 → 抛 BizException("工单不存在，id = " + id)
     *   3. 返回
     *
     * 工具：
     *   this.getById(id)
     */
    @Override
    public Ticket getDetail(Long id) {
        // TODO 你自己写
        Ticket ticket = this.getById(id);
        if(ticket == null){
            throw new BizException("工单不存在，id = "+ id);
        }


        return ticket;
    }

    /**
     * 修改状态。
     *
     * 中文步骤：
     *   1. 校验 status 只能是 0/1/2/3，否则抛异常
     *   2. 确认工单存在（不存在会自己抛异常）
     *   3. 改状态
     *   4. 改更新时间
     *   5. 保存
     *   6. 清掉统计缓存
     *
     * 工具：
     *   this.getDetail(id)
     *   LocalDateTime.now()
     *   this.updateById(ticket)
     *   clearStatsCache()
     */
    @Override
    public void updateStatus(Long id, Integer status) {
        // TODO 你自己写
        if(status == null || status < 0 || status >3){//用了更复杂的判定
            throw new BizException("状态值不合理，只能是0,1,2,3");
        }
        Ticket ticket = this.getDetail(id);//用的GETBYID
        ticket.setStatus(status);//忘记要创建对象，然后调用方法了
        ticket.setUpdateTime(LocalDateTime.now());
        this.updateById(ticket);//不知道为什么这么写

        
        this.clearStatsCache();
    }

    /**
     * 删除工单（逻辑删除）。
     *
     * 中文步骤：
     *   1. 确认工单存在（不存在会自己抛异常）
     *   2. 删除
     *   3. 清掉统计缓存
     *
     * 工具：
     *   this.getDetail(id)
     *   this.removeById(id)      ← 因为有 @TableLogic，实际是 UPDATE deleted=1
     *   clearStatsCache()
     */
    @Override
    public void deleteTicket(Long id) {
        // TODO 你自己写
        this.getDetail(id);
        this.removeById(id);
        clearStatsCache();
    }

    // ================================================================
    //  ↓↓↓ 下面两个已经写好了（Day 11 会重写） ↓↓↓
    // ================================================================

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

    /** 删除统计缓存。数据一变动就调它（Cache Aside 模式） */
    private void clearStatsCache() {
        try {
            redisTemplate.delete(STATS_CACHE_KEY);
        } catch (Exception e) {
            log.warn("Redis 删除缓存失败：{}", e.getMessage());
        }
    }
}
