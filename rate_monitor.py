#!/usr/bin/env python3
"""
CNY -> GBP Exchange Rate Monitor
每 6 小时自动检查一次，变动超过 1% 时打印警报并记录到日志文件。

用法:
    python3 rate_monitor.py           # 正常运行（需要外网）
    python3 rate_monitor.py --demo    # 演示模式（无需外网，模拟数据）
    nohup python3 rate_monitor.py &   # 后台运行，关终端也不停
    bash setup_cron.sh                # 加入系统定时任务（永久自动化）
"""

import sys
import time
import os
import random
from datetime import datetime

# ── 配置 ──────────────────────────────────────────────────────────────────────
INTERVAL_HOURS  = 6      # 检查间隔（小时）
ALERT_THRESHOLD = 1.0    # 汇率变动超过此百分比时警报
LOG_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "rate_history.log")
API_URL  = "https://open.er-api.com/v6/latest/CNY"
# ─────────────────────────────────────────────────────────────────────────────

DEMO_MODE = "--demo" in sys.argv


def fetch_rate_live() -> float:
    """从免费 API 获取 1 CNY = ? GBP，失败返回 -1。"""
    import urllib.request, json
    try:
        with urllib.request.urlopen(API_URL, timeout=10) as resp:
            data = json.loads(resp.read().decode())
            return float(data["rates"]["GBP"])
    except Exception as e:
        print(f"[ERROR] 获取汇率失败: {e}")
        return -1.0


def fetch_rate_demo(prev: float) -> float:
    """演示模式：在真实基准值附近随机波动（偶尔触发警报）。"""
    base = prev if prev > 0 else 0.1083          # 近期真实基准
    change = random.uniform(-0.015, 0.015)        # ±1.5% 随机波动
    return round(base * (1 + change), 6)


def fetch_rate(prev: float) -> float:
    return fetch_rate_demo(prev) if DEMO_MODE else fetch_rate_live()


def log(msg: str):
    """同时输出到终端和日志文件。"""
    line = f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {msg}"
    print(line, flush=True)
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(line + "\n")


def run():
    interval_sec = 10 if DEMO_MODE else INTERVAL_HOURS * 3600
    label = "10 秒(演示)" if DEMO_MODE else f"{INTERVAL_HOURS} 小时"

    log("=" * 55)
    log(f"CNY->GBP 汇率监控启动  {'【演示模式】' if DEMO_MODE else '【实时模式】'}")
    log(f"检查间隔: {label}")
    log(f"日志文件: {LOG_FILE}")
    log("=" * 55)

    prev_rate = -1.0

    while True:
        rate = fetch_rate(prev_rate)

        if rate <= 0:
            log("本次获取失败，等待下次检查...")
        else:
            msg = f"1 CNY = {rate:.6f} GBP"

            if prev_rate > 0:
                change_pct = (rate - prev_rate) / prev_rate * 100
                direction  = "↑" if change_pct > 0 else "↓"
                msg += f"  ({direction}{abs(change_pct):.3f}%)"

                if abs(change_pct) >= ALERT_THRESHOLD:
                    log("!" * 50)
                    log(f"*** 警报 *** 汇率变动 {change_pct:+.3f}%")
                    log(f"    上次: {prev_rate:.6f}  ->  本次: {rate:.6f}")
                    log("!" * 50)

            log(msg)
            prev_rate = rate

        nxt = datetime.fromtimestamp(time.time() + interval_sec).strftime("%Y-%m-%d %H:%M:%S")
        log(f"下次检查: {nxt}")
        time.sleep(interval_sec)


if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        log("监控已手动停止。")
