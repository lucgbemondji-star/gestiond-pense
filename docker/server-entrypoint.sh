#!/bin/sh
set -eu

port="${PORT:-8080}"
case "$port" in
  ''|*[!0-9]*)
    echo "La variable PORT doit contenir un port valide." >&2
    exit 1
    ;;
esac
if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
  echo "La variable PORT doit être comprise entre 1 et 65535." >&2
  exit 1
fi

sed "s/port=\"__PORT__\"/port=\"$port\"/" /usr/local/tomcat/conf/server.xml.template > /tmp/server.xml
mv /tmp/server.xml /usr/local/tomcat/conf/server.xml

exec catalina.sh run
