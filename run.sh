#!/usr/bin/env bash
#
# run.sh — ThoughtWorks Agents 前后端管理
#
# 用法:
#   ./run.sh start    启动前后端，启动后监听后端日志
#   ./run.sh stop     关停前后端
#   ./run.sh restart  重启前后端
#   ./run.sh status   查看运行状态
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
LOG_DIR="$PROJECT_ROOT/logs"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/frontend.pid"

BACKEND_PORT=8090
FRONTEND_PORT=5173

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[ OK ]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()   { echo -e "${RED}[ERR]${NC}  $*"; }

mkdir -p "$LOG_DIR"

# ───────────────────── stop ─────────────────────
do_stop() {
  echo -e "\n${CYAN}🛑 停止服务${NC}\n"

  for entry in "后端:$BACKEND_PID_FILE:$BACKEND_PORT" "前端:$FRONTEND_PID_FILE:$FRONTEND_PORT"; do
    IFS=: read -r name pf port <<< "$entry"

    # 按 PID 文件停止
    if [ -f "$pf" ]; then
      pid=$(cat "$pf")
      if kill -0 "$pid" 2>/dev/null; then
        info "停止 $name (PID $pid) ..."
        kill "$pid" 2>/dev/null || true
        for _ in $(seq 10); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
        kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
      fi
      rm -f "$pf"
    fi

    # 按端口兜底清理
    pids=$(lsof -i ":$port" -sTCP:LISTEN -t 2>/dev/null || true)
    if [ -n "$pids" ]; then
      for p in $pids; do
        warn "清理 $name 残留进程 (PID $p, 端口 $port)"
        kill "$p" 2>/dev/null || true
      done
      sleep 1
      for p in $(lsof -i ":$port" -sTCP:LISTEN -t 2>/dev/null || true); do
        kill -9 "$p" 2>/dev/null || true
      done
    fi

    ok "$name 已停止"
  done
  echo ""
}

# ───────────────────── start ────────────────────
do_start() {
  echo -e "\n${CYAN}🚀 启动服务${NC}\n"

  # 检查端口
  for entry in "后端:$BACKEND_PORT" "前端:$FRONTEND_PORT"; do
    IFS=: read -r name port <<< "$entry"
    if lsof -i ":$port" -sTCP:LISTEN -t &>/dev/null; then
      err "$name 端口 $port 已被占用，请先执行 ./run.sh stop"
      exit 1
    fi
  done

  # ── 后端 ──
  info "编译后端..."
  cd "$PROJECT_ROOT"
  mvn compile -q -DskipTests 2>&1 | tail -3

  info "启动后端 (Spring Boot :$BACKEND_PORT) ..."
  nohup mvn spring-boot:run -DskipTests -Dspring-boot.run.arguments="--server.port=$BACKEND_PORT" \
    > "$BACKEND_LOG" 2>&1 &
  echo $! > "$BACKEND_PID_FILE"
  info "后端 PID $(cat "$BACKEND_PID_FILE")"

  printf "  等待后端就绪"
  for i in $(seq 60); do
    if curl -sf -o /dev/null "http://localhost:$BACKEND_PORT/h2-console" 2>/dev/null; then
      echo ""
      ok "后端已就绪 (${i}s)"
      break
    fi
    [ "$i" -eq 60 ] && { echo ""; warn "后端启动超时，请查看 $BACKEND_LOG"; }
    printf "."
    sleep 1
  done

  # ── 前端 ──
  cd "$FRONTEND_DIR"
  [ ! -d node_modules ] && { info "安装前端依赖..."; npm install --silent 2>&1 | tail -3; }

  info "启动前端 (Vite :$FRONTEND_PORT) ..."
  nohup npx vite --port "$FRONTEND_PORT" --strictPort \
    > "$FRONTEND_LOG" 2>&1 &
  echo $! > "$FRONTEND_PID_FILE"
  info "前端 PID $(cat "$FRONTEND_PID_FILE")"

  printf "  等待前端就绪"
  for i in $(seq 20); do
    if curl -sf -o /dev/null "http://localhost:$FRONTEND_PORT" 2>/dev/null; then
      echo ""
      ok "前端已就绪 (${i}s)"
      break
    fi
    [ "$i" -eq 20 ] && { echo ""; warn "前端启动超时，请查看 $FRONTEND_LOG"; }
    printf "."
    sleep 1
  done

  # ── 摘要 ──
  echo ""
  echo -e "${CYAN}════════════════════════════════════════════════${NC}"
  echo -e "  ${GREEN}ThoughtWorks Agents 已启动${NC}"
  echo -e ""
  echo -e "  前端  ${GREEN}http://localhost:$FRONTEND_PORT${NC}"
  echo -e "  后端  ${GREEN}http://localhost:$BACKEND_PORT${NC}"
  echo -e "  H2    ${YELLOW}http://localhost:$BACKEND_PORT/h2-console${NC}"
  echo -e ""
  echo -e "  停止  ${YELLOW}./run.sh stop${NC}"
  echo -e "  重启  ${YELLOW}./run.sh restart${NC}"
  echo -e "  状态  ${YELLOW}./run.sh status${NC}"
  echo -e "${CYAN}════════════════════════════════════════════════${NC}"
  echo ""

  info "监听后端日志 (Ctrl+C 退出监听，不影响服务) ...\n"
  tail -f "$BACKEND_LOG"
}

# ───────────────────── status ───────────────────
do_status() {
  echo -e "\n${CYAN}════════════════════════════════════════════════${NC}"
  echo -e "  ${CYAN}ThoughtWorks Agents — 服务状态${NC}"
  echo -e "${CYAN}════════════════════════════════════════════════${NC}\n"

  for entry in "后端:$BACKEND_PID_FILE:$BACKEND_PORT" "前端:$FRONTEND_PID_FILE:$FRONTEND_PORT"; do
    IFS=: read -r name pf port <<< "$entry"
    pid="-"
    [ -f "$pf" ] && kill -0 "$(cat "$pf")" 2>/dev/null && pid="$(cat "$pf")"
    if lsof -i ":$port" -sTCP:LISTEN -t &>/dev/null; then
      printf "  %-6s ${GREEN}● 运行中${NC}  PID %-8s 端口 ${GREEN}%s${NC}\n" "$name" "$pid" "$port"
    else
      printf "  %-6s ${RED}● 已停止${NC}\n" "$name"
    fi
  done
  echo -e "\n${CYAN}════════════════════════════════════════════════${NC}\n"
}

# ───────────────────── main ─────────────────────
case "${1:-}" in
  start)   do_start ;;
  stop)    do_stop ;;
  restart) do_stop; sleep 2; do_start ;;
  status)  do_status ;;
  *)
    echo ""
    echo "用法: ./run.sh {start|stop|restart|status}"
    echo ""
    echo "  start    启动前后端，启动后监听后端日志"
    echo "  stop     关停前后端"
    echo "  restart  重启前后端"
    echo "  status   查看运行状态"
    echo ""
    ;;
esac
