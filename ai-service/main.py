# -*- coding: utf-8 -*-
"""
智能工单 AI 服务
------------------------------------------------------------
作用：给 Java 后端提供两个 AI 能力
    1. /ai/classify   工单自动分类（把"登录不上"归类成"账号问题"）
    2. /ai/reply      自动生成客服回复草稿

两种模式（自动切换，不用改代码）：
    - 配置了环境变量 DEEPSEEK_API_KEY  -> 调用 DeepSeek 大模型（真实 AI）
    - 没配置                          -> 走关键词规则（也能跑，方便先跑通流程）

启动方式：
    pip install -r requirements.txt
    python -m uvicorn main:app --reload --port 8000
    然后打开 http://localhost:8000/docs 就能在网页上直接测试
"""

import os
import requests
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# ============================================================
# 1. 初始化
# ============================================================

app = FastAPI(title="智能工单 AI 服务", version="1.0.0")

# 允许跨域，否则前端页面调这个接口会被浏览器拦
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 从环境变量读 API Key。没设置就为空字符串，自动降级到规则模式
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"

# 工单分类，和 Java 端的 category 字段对应
CATEGORIES = ["账号问题", "订单问题", "发票问题", "退款问题", "物流问题", "技术支持", "其他"]

# 规则模式用的关键词表
KEYWORDS = {
    "账号问题": ["登录", "密码", "账号", "注册", "验证码", "锁定"],
    "订单问题": ["订单", "下单", "发货", "取消订单", "待发货"],
    "发票问题": ["发票", "开票", "报销", "税号"],
    "退款问题": ["退款", "退货", "退钱", "退单"],
    "物流问题": ["物流", "快递", "运费", "到货", "签收"],
    "技术支持": ["报错", "崩溃", "打不开", "卡顿", "闪退", "接口"],
}


# ============================================================
# 2. 请求 / 响应 的数据结构
# ============================================================

class TicketIn(BaseModel):
    """Java 端传过来的工单内容"""
    title: str
    content: str = ""


# ============================================================
# 3. 规则模式：不花钱、不联网也能跑
# ============================================================

def rule_classify(text: str) -> str:
    """按关键词匹配分类，命中不了就归到"其他""""
    for category, words in KEYWORDS.items():
        for word in words:
            if word in text:
                return category
    return "其他"


def rule_reply(title: str, content: str, category: str) -> str:
    """按分类套模板生成回复草稿"""
    templates = {
        "账号问题": "您好，关于账号问题，请先尝试点击登录页的「忘记密码」重置密码。若仍无法登录，请提供您的注册手机号，我们会尽快为您核查。",
        "订单问题": "您好，已收到您的订单咨询。请提供订单号，我们马上为您查询当前状态并跟进处理。",
        "发票问题": "您好，电子发票可在「个人中心 - 我的订单 - 申请开票」中自行下载，通常 1 个工作日内开具完成。",
        "退款问题": "您好，退款申请提交后一般 1-3 个工作日原路退回。如超时未到账，请提供订单号，我们为您加急核实。",
        "物流问题": "您好，请提供订单号，我们立即为您查询物流轨迹并联系承运方核实。",
        "技术支持": "您好，很抱歉给您带来不便。请提供报错截图和您的操作步骤，我们会尽快定位问题。",
    }
    return templates.get(category, "您好，您的问题我们已收到，客服会尽快与您联系，感谢您的耐心等待。")


# ============================================================
# 4. 大模型模式：调 DeepSeek 接口
# ============================================================

def call_deepseek(prompt: str) -> str:
    """把 prompt 发给 DeepSeek，返回模型回复的文本"""
    headers = {
        "Authorization": "Bearer " + DEEPSEEK_API_KEY,
        "Content-Type": "application/json",
    }
    body = {
        "model": "deepseek-chat",
        "messages": [
            {"role": "system", "content": "你是一个专业的电商客服助手，回答简洁、礼貌、专业。"},
            {"role": "user", "content": prompt},
        ],
        "temperature": 0.3,   # 调低一点，让输出稳定，别太发散
        "max_tokens": 300,
    }

    # timeout 必须设置！否则大模型卡住会把 Java 那边也拖死
    response = requests.post(DEEPSEEK_URL, headers=headers, json=body, timeout=20)
    response.raise_for_status()
    data = response.json()
    return data["choices"][0]["message"]["content"].strip()


# ============================================================
# 5. 接口
# ============================================================

@app.get("/health")
def health():
    """健康检查。Java 端启动时可以先调这个确认 AI 服务活着"""
    return {
        "status": "ok",
        "mode": "deepseek" if DEEPSEEK_API_KEY else "rule",
    }


@app.post("/ai/classify")
def classify(ticket: TicketIn):
    """
    工单自动分类。
    返回：{"category": "账号问题", "confidence": 0.9, "mode": "rule"}
    """
    text = ticket.title + " " + ticket.content

    # 没配 Key 就走规则
    if not DEEPSEEK_API_KEY:
        category = rule_classify(text)
        return {"category": category, "confidence": 0.6, "mode": "rule"}

    # 配了 Key 就走大模型
    try:
        prompt = (
            "请判断下面这条客服工单属于哪个分类。\n"
            "只能从这些分类里选一个：" + "、".join(CATEGORIES) + "\n"
            "只输出分类名称，不要输出任何其它文字。\n\n"
            "工单标题：" + ticket.title + "\n"
            "工单内容：" + ticket.content
        )
        category = call_deepseek(prompt).strip()

        # 防止模型自由发挥，输出不在列表里的词
        if category not in CATEGORIES:
            category = rule_classify(text)

        return {"category": category, "confidence": 0.9, "mode": "deepseek"}
    except Exception as e:
        # 大模型失败不能影响主流程，降级到规则
        print("调用 DeepSeek 失败，降级到规则模式：", e)
        return {"category": rule_classify(text), "confidence": 0.5, "mode": "rule-fallback"}


@app.post("/ai/reply")
def reply(ticket: TicketIn):
    """
    生成客服回复草稿。
    返回：{"suggestion": "您好，……", "mode": "rule"}
    """
    text = ticket.title + " " + ticket.content
    category = rule_classify(text)

    if not DEEPSEEK_API_KEY:
        return {"suggestion": rule_reply(ticket.title, ticket.content, category), "mode": "rule"}

    try:
        prompt = (
            "请为下面这条客服工单写一段回复草稿，语气礼貌专业，控制在 100 字以内。\n\n"
            "工单标题：" + ticket.title + "\n"
            "工单内容：" + ticket.content
        )
        return {"suggestion": call_deepseek(prompt), "mode": "deepseek"}
    except Exception as e:
        print("调用 DeepSeek 失败，降级到规则模式：", e)
        return {"suggestion": rule_reply(ticket.title, ticket.content, category), "mode": "rule-fallback"}


# 直接 python main.py 也能启动（等价于上面的 uvicorn 命令）
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
