#!/bin/sh
# @(#) Run a Gatling load-test with optional knob overrides
#

usage() {
	cat <<EOT >&2
usage: $0 <load test name> [knob1=value knob2=value ...]

Where "load test name" is the simple class name defining the load test
and optional test parameters ("knobs") are expressed as property
assignments.

For example:

$0 StorageFacilityLoadTest \\
	service.endpoint="http://localhost:9080" \\
	simulation.delay=5 \\
	simulation.burstUsers=100 \\
	simulation.constantUsers=30 \\
	simulation.constantUsersWindow=300

EOT

	exit 1
}

########################################################################
# MAIN
########################################################################

if [[ $# == 0 ]]
then
	usage
fi

PROJECT=$(cd $(dirname $0)/..; pwd)
TEST_NAME=$1
KNOBS=""

shift

for param in "$@"
do
	KNOBS="$KNOBS -D$param"
done

cd $PROJECT || exit 2
sbt $KNOBS "gatling / GatlingIt / testOnly *$TEST_NAME"

