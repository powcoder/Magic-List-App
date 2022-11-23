https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
#!/usr/bin/env bash
export USER=`whoami`
export PID=`lsof -i :9001 | grep -oE "java\s+[0-9]{2,5}\s+${USER}" | grep -oEi '[0-9]{2,5}' | head -1`
if [[ "${PID}" != "" ]]
then
    echo "Found process ${PID} running on port 9001, killing..."
    kill -9 "${PID}"
fi