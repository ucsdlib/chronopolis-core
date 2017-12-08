#!/bin/sh
#
# ingest-server  This script starts the ingset server process
#
# chkconfig: - 64 36
# description: Start the Chronopolis Ingest Server
# processname: ingest-server.jar
# config: /usr/local/chronopolis/ingest/application.yml
#
### BEGIN INIT INFO
# Provides:      ingest-server
# Required-Start: $network
# Required-Stop: 
# Short-Description: Chronopolis Ingest Server
# Description:   Start the Chronopolis Ingest Server
### END INIT INFO

# Amount of time to attempt to communicate with the server
TIMEOUT=45

# User to execute as
CHRON_USER="chronopolis"

INGEST_DIR="/usr/local/chronopolis/ingest"
INGEST_JAR="ingest-server.jar"


JAVA_BIN=/usr/bin/java
JAVA_CMD="$JAVA_BIN -jar $INGEST_DIR/$INGEST_JAR &"

. /etc/init.d/functions

RETVAL=0

# vars for our use
prog="ingest-server"
pidfile="/var/run/ingest-server.pid"
lockfile=/var/lock/subsys/ingest-server
logdir=/var/log/chronopolis

# env vars for spring
export SPRING_PID_FILE=$pidfile
export SPRING_CONFIG_LOCATION="$INGEST_DIR/"

start(){
    ret=0
    # check if the user exists
    if ! getent passwd "$CHRON_USER" > /dev/null 2>&1; then
        echo "User $CHRON_USER does not exist; unable to start Ingest Server"
        action $"Starting $prog: " /bin/false
        return 2
    fi

    # log directory exists
    if [ ! -d "$logdir" ]; then
        echo "Creating $logdir"
        mkdir "$logdir"
    fi

    # permissions for logging
    uname="$(stat --format '%U' "$logdir")"
    if [ "x${uname}" != "x${CHRON_USER}" ]; then
        echo "Updating permissions for $logdir"
        chown "$CHRON_USER":"$CHRON_USER" "$logdir"
    fi


    # check if we're already running
    RUNNING=0
    if [ -f "$pidfile" ]; then
        PID=`cat "$pidfile" 2>/dev/null`
        if [ -n "$PID" ] && [ -d "/proc/$PID" ]; then
            RUNNING=1
        fi
    else
        # create the pidfile and grant ownership
        touch "$pidfile"
        chown "$CHRON_USER":"$CHRON_USER" "$pidfile"
    fi

    # check for failed/hung server
    # may need to update host endpoint
    RESPONSE=`curl -I -X GET http://localhost:8080 > /dev/null 2>&1`
    if [ $RUNNING = 1 ] && [ $? = 0 ]; then
        action $"Starting $prog: " /bin/true
        return 0
    fi

    # exec
    daemon --user "$CHRON_USER" --pidfile "$pidfile" $JAVA_CMD

    # wait for timeout
    while [ $TIMEOUT -gt 0 ]; do
        # Try to connect to the server
        curl -I -X GET http://localhost:8080 > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            break
        fi
        sleep 1
        let TIMEOUT=${TIMEOUT}-1
    done

    if [ $? -eq 0 ]; then
        action $"Starting $prog: " /bin/true
        touch $lockfile
    else
        action $"Starting $prog: " /bin/false
        ret=3
    fi

    return $ret
}

stop(){
    ret=0

    # No pidfile = not running
    if [ ! -f "$pidfile" ]; then
        action $"Stopping $prog: " /bin/true
        return 0
    fi

    # Get the pid and attempt to kill
    PID=`cat "$pidfile" 2>/dev/null`
    if [ -n "$PID" ]; then
        /bin/kill "$PID" > /dev/null 2>&1 || break
        if [ $? -eq 0 ]; then
            action $"Stopping $prog: " /bin/true
            rm -f $lockfile
            rm -f "$pidfile"
        else
            action $"Stopping $prog: " /bin/false
            ret=4
        fi
    else
        action $"Stopping $prog: " /bin/false
        ret=4
    fi

    return $ret
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
    reload)
        exit 5
        ;;
    status)
        status -p "$pidfile" ingest-server
        ;;
esac

exit $?
