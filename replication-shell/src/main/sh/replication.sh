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
REPL_JAR="replication.jar"
REPL_PID_FILE="/var/run/replication.pid"

# Set the location which holds our application.properties
# SPRING_CONFIG_NAME="settings.conf"
SPRING_CONFIG_LOCATION="$REPL_DIR/"

JAVA_BIN=/usr/bin/java
JAVA_CMD="$JAVA_BIN -jar $REPL_DIR/$REPL_JAR"
PARAMS="--spring.config.location=$SPRING_CONFIG_LOCATION &"

. /etc/init.d/functions

RETVAL=0
COUNTDOWN=1

case "$1" in
    start)
    if ! getent passwd $CHRON_USER 2>&1; then
        echo "User $CHRON_USER does not exist; unable to start replication service"
        action "Started Replication Service " /bin/false
        exit 2
    fi

    echo "Starting the replication service"
    daemon --user "$CHRON_USER" --pidfile "$REPL_PID_FILE" $JAVA_CMD $PARAMS > /dev/null 2>&1
    RETVAL=$?

    echo "Waiting for startup to complete..."
    while [ $COUNTDOWN -gt 0 ]; do
        sleep 8 # This seems to be the minimum amount of time to get consistent results
        let COUNTDOWN=0
    done

    # This from the jenkins init script... slightly modified for our use
    if [ $RETVAL -eq 0 ]; then
        # Create a pipe to read from so we can still alter $RUNNING
        if [ ! -p check_pipe ]; then
            mkfifo check_pipe
        fi

        /bin/ps hww -u "$CHRON_USER" -o sess,ppid,pid,cmd > check_pipe &
        while read sess ppid pid cmd; do
            [ $ppid -eq 1 ] || continue
            echo "$cmd" | grep $REPL_JAR > /dev/null
            [ $? -eq 0 ] || continue
            echo $pid > $REPL_PID_FILE
            let RUNNING=0
            echo $RUNNING
        done < check_pipe
    fi

    if [ $RUNNING -eq 0 ]; then
        success
    else
        failure
    fi

    RETVAL=$RUNNING
    ;;
    stop)
    echo "Stopping the replication service"
    killproc replication
    ;;
    reload)
    $0 stop
    $0 start
    ;;
    status)
        status replication
        RETVAL=$?
    ;;
esac

exit $RETVAL
