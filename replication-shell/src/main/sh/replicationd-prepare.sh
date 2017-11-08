#!/usr/bin/env bash

logdir="/var/log/chronopolis/"
user=`systemctl show -p User replicationd | sed 's/User=//'`
group=`systemctl show -p Group replicationd | sed 's/Group=//'`

# log directory exists
if [ ! -d "$logdir" ]; then
    echo "Creating $logdir"
    mkdir "$logdir"
fi

# permissions for logging
uname="$(stat --format '%U' "$logdir")"
if [ "x${uname}" != "x${CHRON_USER}" ]; then
    echo "Updating permissions for $logdir"
    chown "$user":"$group" "$logdir"
fi

exit 0
