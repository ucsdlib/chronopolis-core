#!/bin/sh

### BEGIN INIT INFO
# Provides:      replication
# Default-Start: 3 5
# Default-Stop:  0 1 2 6
# Description:   Start the Chronopolis replication service
### END INIT INFO

# User to execute as
CHRON_USER="chron"

REPL_JAR="/usr/lib/chronopolis/replication.jar"
REPL_PID_FILE="/var/run/replication.pid"

# Set the location which holds our application.properties
# SPRING_CONFIG_NAME="settings.conf"
SPRING_CONFIG_LOCATION="/etc/chronopolis/"

JAVA_BIN=/usr/bin/java
JAVA_CMD="$JAVA_BIN -jar $REPL_JAR"
PARAMS="--spring.config.location=$SPRING_CONFIG_LOCATION"

RETVAL=0

case "$1" in
    start)
    echo "Starting the replication service"
    daemon --user "$CHRON_USER" --pidfile "$REPL_PID_FILE" $JAVA_CMD > /dev/null 2>&1 &
    RETVAL=$?

    # This bit is from the jenkins init script, I'm not sure if we'll need it though
    if [ $RETVAL = 0 ]; then
        success
        echo > "$REPL_PID_FILE"
        /bin/ps hww -u "$CHRON_USER" -o sess,ppid,pid,cmd | \
        while read sess ppid pid cmd; do
        [ $ppid = 1 ] || continue
        echo "$cmd" | grep $REPL_JAR > /dev/null
        [ $? = 0 ] || continue
        echo $pid > $REPL_PID_FILE
        done
    else
        failure
    fi
    RETVAL=$?
    ;;
    stop)
    echo "Stopping the replication service"
    killproc replication
    ;;
    restart)
    $0 stop
    $0 start
    ;;
    status)
        status replication
        RETVAL=$?
    ;;
esac

exit $RETVAL
