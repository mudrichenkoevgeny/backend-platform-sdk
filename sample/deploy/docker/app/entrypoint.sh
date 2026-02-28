#!/usr/bin/env sh
set -e

echo "=== DEBUG: START entrypoint.sh ==="
echo "DEBUG: Current directory: $(pwd)"

# ----------------------------------------------------------------------
# Get database parameters
# ----------------------------------------------------------------------
echo "DEBUG: DB_URL=${DB_URL:-not set}"
if [ -n "${DB_URL:-}" ]; then
    if echo "$DB_URL" | grep -q "jdbc:postgresql://[^:/]\+\(:[0-9]\+\)\?/[^/?]\+"; then
        DB_HOST=$(echo "$DB_URL" | sed -n 's#jdbc:postgresql://\([^:/]\+\).*#\1#p')
        DB_PORT=$(echo "$DB_URL" | sed -n 's#jdbc:postgresql://[^:/]\+:\([0-9]\+\).*#\1#p')
        DB_NAME=$(echo "$DB_URL" | sed -n 's#jdbc:postgresql://[^/]\+/\([^/?]\+\).*#\1#p')
        DB_PORT="${DB_PORT:-5432}"
        echo "DEBUG: Parsed - host=$DB_HOST, port=$DB_PORT, db=$DB_NAME"
    else
        echo "ERROR: Cannot parse DB_URL: $DB_URL"
        exit 1
    fi
fi

# ----------------------------------------------------------------------
# Set default variables
# ----------------------------------------------------------------------
: "${DB_HOST:=db}"
: "${DB_PORT:=5432}"
: "${DB_NAME:?Database name must be set via DB_NAME or DB_URL}"
: "${SECRETS_DIR:=/app/sample/deploy/secrets}"

echo "DEBUG: Final values - DB_HOST=$DB_HOST, DB_PORT=$DB_PORT, DB_NAME=$DB_NAME"
echo "DEBUG: SECRETS_DIR=$SECRETS_DIR"

DB_USER_FILE="${SECRET_DB_USER_FILE:-${DB_USER_FILE:-$SECRETS_DIR/DB_USER}}"
DB_PASSWORD_FILE="${SECRET_DB_PASSWORD_FILE:-${DB_PASSWORD_FILE:-$SECRETS_DIR/DB_PASSWORD}}"
: "${DB_WAIT_TIMEOUT:=90}"

echo "DEBUG: DB_USER_FILE=$DB_USER_FILE"
echo "DEBUG: DB_PASSWORD_FILE=$DB_PASSWORD_FILE"
echo "DEBUG: DB_WAIT_TIMEOUT=$DB_WAIT_TIMEOUT"

# ----------------------------------------------------------------------
# Check required files
# ----------------------------------------------------------------------
if [ ! -f "$DB_USER_FILE" ]; then
    echo "ERROR: DB_USER file not found: $DB_USER_FILE"
    ls -la "$SECRETS_DIR" 2>/dev/null || echo "Cannot list $SECRETS_DIR"
    exit 1
fi

if [ ! -f "$DB_PASSWORD_FILE" ]; then
    echo "ERROR: DB_PASSWORD file not found: $DB_PASSWORD_FILE"
    ls -la "$SECRETS_DIR" 2>/dev/null || echo "Cannot list $SECRETS_DIR"
    exit 1
fi

# ----------------------------------------------------------------------
# Read secrets
# ----------------------------------------------------------------------
USER=$(cat "$DB_USER_FILE" | tr -d '\r')
PASS=$(cat "$DB_PASSWORD_FILE" | tr -d '\r')

echo "DEBUG: User from file: '$USER' (len: $(echo -n "$USER" | wc -c))"
echo "DEBUG: Password len: $(echo -n "$PASS" | wc -c) chars"

# ----------------------------------------------------------------------
# Waiting for database connection
# ----------------------------------------------------------------------
echo "Waiting for database connection..."
echo "  Database: $DB_NAME"
echo "  Host:     $DB_HOST:$DB_PORT"
echo "  User:     $USER"
echo "  Timeout:  ${DB_WAIT_TIMEOUT}s"

TIMEOUT=$DB_WAIT_TIMEOUT
i=0
while [ $i -lt "$TIMEOUT" ]; do
    echo "DEBUG: Attempt $((i+1))/$TIMEOUT..."

    echo "DEBUG: Running: pg_isready -h $DB_HOST -p $DB_PORT -U $USER -d $DB_NAME"
    if PGPASSWORD="$PASS" pg_isready \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$USER" \
        -d "$DB_NAME"; then

        echo "✓ Database is reachable"
        echo "DEBUG: pg_isready succeeded"

        echo "DEBUG: Testing actual connection with SELECT 1..."
        if PGPASSWORD="$PASS" psql \
            -h "$DB_HOST" \
            -p "$DB_PORT" \
            -U "$USER" \
            -d "$DB_NAME" \
            -c "SELECT 1;"; then

            echo "✓ Database connection test successful"
            break
        else
            echo "⚠ Database reachable but connection test failed"
            echo "DEBUG: psql error:"
            PGPASSWORD="$PASS" psql \
                -h "$DB_HOST" \
                -p "$DB_PORT" \
                -U "$USER" \
                -d "$DB_NAME" \
                -c "SELECT 1;"
        fi
    else
        echo "DEBUG: pg_isready failed"
        PGPASSWORD="$PASS" pg_isready \
            -h "$DB_HOST" \
            -p "$DB_PORT" \
            -U "$USER" \
            -d "$DB_NAME"
    fi

    i=$((i + 1))
    sleep 1
done

if [ $i -eq "$TIMEOUT" ]; then
    echo "✗ ERROR: Database not ready after $TIMEOUT seconds"
    echo "DEBUG: Final attempt - running command manually:"
    echo "DEBUG: PGPASSWORD='***' pg_isready -h $DB_HOST -p $DB_PORT -U $USER -d $DB_NAME"
    PGPASSWORD="$PASS" pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$USER" -d "$DB_NAME" || true
    exit 1
fi

# ----------------------------------------------------------------------
# Launch app
# ----------------------------------------------------------------------
echo "=== DEBUG: Database ready, starting app ==="
MAIN_CLASS="io.github.mudrichenkoevgeny.backend.sample.MainKt"
echo "DEBUG: Command: exec java -cp \"app.jar:lib/*\" $MAIN_CLASS"
exec java -cp "app.jar:lib/*" "$MAIN_CLASS"