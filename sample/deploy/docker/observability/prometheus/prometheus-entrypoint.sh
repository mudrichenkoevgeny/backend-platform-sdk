#!/bin/sh

: ${OTEL_TARGETS:="otel-collector:8888"}
: ${KTOR_MANAGEMENT_PORT:="9090"}

sed -e "s/\${OTEL_TARGETS}/$OTEL_TARGETS/g" \
    -e "s/\${KTOR_MANAGEMENT_PORT}/$KTOR_MANAGEMENT_PORT/g" \
    /etc/prometheus/prometheus.yml > /tmp/prometheus.yml

exec /bin/prometheus --config.file=/tmp/prometheus.yml --web.listen-address=":$KTOR_MANAGEMENT_PORT" "$@"