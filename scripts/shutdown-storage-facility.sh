#!/usr/bin/env sh
# @(#) Driver script for shutting down the storage-facility microservice
#

export SCRIPT_HOME="$(cd $(dirname $0); pwd)/demo"

$(dirname $0)/demo.sh storage-facility shutdown put "message='${1:-stopping}'"

