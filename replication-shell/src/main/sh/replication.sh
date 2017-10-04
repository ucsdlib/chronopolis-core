#!/bin/sh
#
# replicationd - This script starts the replication service
# 
# chkconfig: - 64 36
# description: Start the Chronopolis Replication Service 
# processname: replication.jar
# config: /usr/local/chronopolis/ingest/application.yml
#
### BEGIN INIT INFO
# Provides:      replication
# Required-Start: $network
# Required-Stop:
# Short-Description: Chronopolis Replication Service
# Description:   Start the Chronopolis replication service
### END INIT INFO

# User to execute as
CHRON_USER="chronopolis"

REPL_DIR="/usr/local/chronopolis/replication"
REPL_JAR="replicationd.jar"

JAVA_BIN=/usr/bin/java
JAVA_CMD="$JAVA_BIN -jar $REPL_DIR/$REPL_JAR &"

. /etc/init.d/functions

# our vars
prog="replicationd"
pidfile="/var/run/replicationd.pid"
lockfile=/var/lock/subsys/replicationd

# env vars for spring
export SPRING_PID_FILE="$pidfile"
export SPRING_CONFIG_LOCATION="$REPL_DIR/"

start(){
    # check user exists
    if ! getent passwd $CHRON_USER > /dev/null 2>&1; then
        echo "User $CHRON_USER does not exist; unable to start replication service"
        action $"Starting $prog: " /bin/false
        return 2
    fi

    # check already running
    RUNNING=0
    if [ -f "$pidfile" ]; then
        PID=`cat "$pidfile" 2>/dev/null`
        # PID + Directory exists == still running
        if [ -n "$PID" ] && [ -d "/proc/$PID" ]; then
            RUNNING=1
        fi
    else
        touch "$pidfile"
        chown "$CHRON_USER":"$CHRON_USER" "$pidfile"
    fi

    # If we're running skip and return early
    if [ $RUNNING = 1 ]; then
        action $"Starting $prog: " /bin/true
        return 0
    fi

    daemon --user "$CHRON_USER" --pidfile "$REPL_PID_FILE" $JAVA_CMD
    RC=$?

    # We just sleep arbitrary amount then hope for the best
    sleep 12

    # Duplicate of the above... should be the easiest way to check if the service is running
    if [ -f "$pidfile" ]; then
        PID=`cat "$pidfile" 2>/dev/null`
        # PID + Directory exists == still running
        if [ -n "$PID" ] && [ -d "/proc/$PID" ]; then
            RC=0
            action $"Starting $prog: " /bin/true
        else
            action $"Starting $prog: " /bin/false
        fi
    else
        # Sanity check
        action $"Starting $prog: " /bin/false
    fi

    return $RC
}

stop(){
    RC=0

    # check if the pidfile exists
    if [ ! -f "$pidfile" ]; then
        action $"Stopping $prog: " /bin/true
        return $RC
    fi

    # find the pid and attempt to kill
    PID=`cat "$pidfile" 2>/dev/null`
    if [ -n "$PID" ]; then
        /bin/kill "$PID" > /dev/null 2>&1 || break
        if [ $? -eq 0 ]; then
            action $"Stopping $prog: " /bin/true
            rm -f $lockfile
            rm -f "$pidfile"
        else
            action $"Stopping $prog: " /bin/false
            RC=4
        fi
    else 
        action $"Stopping $prog: " /bin/false
        RC=4
    fi

    return $RC
}


case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        $0 stop
        $0 start
        ;;
    status)
        status -p "$pidfile" replicationd
        ;;
    reload)
        return 4
        ;;
esac

exit $?
