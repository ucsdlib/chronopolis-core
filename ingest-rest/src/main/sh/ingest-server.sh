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

# Amount of time to attempt to communicate with the serve
TIMEOUT=15

# User to execute as
CHRON_USER="chronopolis"

REPL_JAR="/usr/lib/chronopolis/ingest-server.jar"
REPL_PID_FILE="/var/run/ingest-server.pid"

# Set the location which holds our application.properties
# SPRING_CONFIG_NAME="settings.conf"
SPRING_CONFIG_LOCATION="/etc/chronopolis/"

JAVA_BIN=/usr/bin/java
JAVA_CMD="$JAVA_BIN -jar $REPL_JAR"
PARAMS="--spring.config.location=$SPRING_CONFIG_LOCATION &"

. /etc/init.d/functions

RETVAL=0

case "$1" in
    start)
    echo "Starting the ingest server"
    daemon --user "$CHRON_USER" --pidfile "$REPL_PID_FILE" $JAVA_CMD $PARAMS > /dev/null 2>&1

    echo "Attempting to connect to server..."
    while [ $TIMEOUT -gt 0 ]; do
        # Try to connect to the server
        curl -I -X GET http://localhost:8080 > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            break
        fi
        sleep 1
        let TIMEOUT=${TIMEOUT}-1
    done

    # We could also use the value of the timeout
    RETVAL=$?

    # This bit is from the jenkins init script, I'm not sure if we'll need it though
    if [ $RETVAL -eq 0 ]; then
        success
        /bin/ps hww -u "$CHRON_USER" -o sess,ppid,pid,cmd | \
        while read sess ppid pid cmd; do
        [ $ppid -eq 1 ] || continue
        echo "$cmd" | grep $REPL_JAR > /dev/null
        [ $? -eq 0 ] || continue
        echo $pid > $REPL_PID_FILE
        done
    else
        failure
    fi
    echo
    RETVAL=$?
    ;;
    stop)
    echo "Stopping the ingest server"
    killproc ingest-server
    echo
    ;;
    reload)
    $0 stop
    $0 start
    ;;
    status)
        status ingest-server
        RETVAL=$?
    ;;
esac

exit $RETVAL
