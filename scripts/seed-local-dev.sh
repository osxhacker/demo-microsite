#!/bin/sh
# @(#) Driver script for initializing locally running microservices
#

DEMO_SH="$(dirname $0)/demo.sh"
export SCRIPT_HOME="$(cd $(dirname $0); pwd)/demo"
source $SCRIPT_HOME/shell.subr


usage() {
	cat - <<EOT
usage: $0 [-h] [gateway] | direct

Initializes the demo microservices running locally.

If no argument is given, direct commands to the APISIX gateway.  To
interact directly with each microservice, specify 'direct' as the
argument.
EOT

	exit 1
}


########################################################################
# MAIN
########################################################################

[[ $# -lt 2 ]] || usage

case "${1:-gateway}" in
	direct)
		GATEWAY="" ;;

	gateway)
		GATEWAY="gateway" ;;

	*)
		usage
esac

$DEMO_SH $GATEWAY company post -f bus-4-us &&
	$DEMO_SH $GATEWAY company post -f doktor-strange &&
	$DEMO_SH $GATEWAY company post -f yoda-panda &&
	$DEMO_SH $GATEWAY storage-facility bus-4-us put -f miami-facility &&
	$DEMO_SH $GATEWAY storage-facility bus-4-us put -f new-york-facility &&
	$DEMO_SH $GATEWAY storage-facility doktor-strange put -f tacoma-facility &&
	$DEMO_SH $GATEWAY storage-facility yoda-panda put -f atlanta-facility &&
	$DEMO_SH $GATEWAY storage-facility yoda-panda put -f denver-facility

