#!/bin/bash
# 一键把 rate_monitor.py 加入系统 cron（开机自启 + 每 6 小时检查）
# 用法: bash setup_cron.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PYTHON="$(which python3)"
LOG="$SCRIPT_DIR/rate_monitor_cron.log"

if [ -z "$PYTHON" ]; then
    echo "[ERROR] 找不到 python3，请先安装: sudo apt install python3"
    exit 1
fi

# 每 6 小时执行一次（0点、6点、12点、18点）
CRON_JOB="0 0,6,12,18 * * * $PYTHON $SCRIPT_DIR/rate_monitor.py >> $LOG 2>&1"

# 避免重复添加
( crontab -l 2>/dev/null | grep -v "rate_monitor.py" ; echo "$CRON_JOB" ) | crontab -

echo "Cron 任务已设置:"
echo "  $CRON_JOB"
echo ""
echo "查看当前 cron: crontab -l"
echo "查看日志:      tail -f $LOG"
echo ""
echo "如果想立即测试脚本，运行:"
echo "  python3 $SCRIPT_DIR/rate_monitor.py"
