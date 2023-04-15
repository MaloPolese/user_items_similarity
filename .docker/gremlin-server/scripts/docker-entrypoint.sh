#!/bin/bash
set -e

CONF_FILE=$1
CONF_FILE_INTERNAL="$2-custom-gremlin-config.yaml"

IP=$(ip -o -4 addr list eth0 | perl -n -e 'if (m{inet\s([\d\.]+)\/\d+\s}xms) { print $1 }')

cp $CONF_FILE $CONF_FILE_INTERNAL

sed -i "s|^host:.*|host: $IP|" $CONF_FILE_INTERNAL

exec /opt/gremlin-server/bin/gremlin-server.sh $CONF_FILE_INTERNAL