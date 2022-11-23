https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
#!/usr/bin/env bash
# DO NOT MOVE THIS FILE - IT USES RELATIVE FILE PATHS
# $1 --> STOP or stop

echo "P1 --> ${1}";
echo "P2 --> ${2}";
echo "P2 --> ${3}";

if [[ "$1" != "PORT" ]] && [[ "$1" != "port" ]] ; then
    ./kill-server.sh
fi


export SBT_OPTS="-Xmx2G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xss2M"
export DATABASE_URL="postgres://wqqqaxtrxnplzz:db88805bf33aef6539a1a1a3ff9a3dc5a8ff2c986d6922077a8957254cca42fe@ec2-184-73-181-132.compute-1.amazonaws.com:5432/dehil381d8q4da?sslmode=require"
export DROP_BOX_ACCESS_TOKEN="CuAcfeP4n8AAAAAAAAAAF5NA0ojI5DOLRp1K8"
export MEMCACHIER_SERVERS="mc2.dev.ec2.memcachier.com:11211"
export MEMCACHIER_USERNAME="1D5EE7"
export MEMCACHIER_PASSWORD="1873B59927F68FFDF27A96958032CBDE"
export REALEMAIL_CLIENT_ID="2104"
export REALEMAIL_API_KEY="E334J591D8ACI6L9YN04RXBGOWV21QK8ZH02T57SUMF6P7"
export SEND_GRID_API_KEY="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"
export SERVER_URL="http://localhost:9000"
export STRIPE_API_KEY_PUBLIC="pk_test_dtcWUG3lJeMJVrYzpONkxXKS"
export STRIPE_API_KEY_SECRET="sk_test_xXTcLIZFv4Y8m835ciqtar1g"
export STRIPE_KEY_ON_SUBSCRIPTION_CHANGE="whsec_vtS7wqDTBWDmHuZFz7V9hD34hb0Cl6Gp"
export IS_BETA="false"

if [[ "$1" == "PRODUCTION" ]] || [[ "$1" == "production" ]] ; then
    echo "Running server with production configuration..."
    sbt ";stage;exit";

    export SERVER_ENVIRONMENT="PRODUCTION";
    export APPLICATION_SECRET="BetterThanChangeMe";

    chmod 777 ./target/universal/stage/bin/magic_list_maker-server;
    ./target/universal/stage/bin/magic_list_maker-server -Dconfig.resource=production.conf -Dlogger.resource=prod-logback.xml -Dlogback.debug=true -Dhttp.port=9000
else
    sbt ";run 9000";
fi