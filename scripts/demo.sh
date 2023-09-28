#!/usr/bin/env sh
# @(#) Driver script for executing arbitrary microservice commands
#
# This is a driver program for initiating requests sent to deployed
# microservices.  It uses a technique of stacking execution of
# subcommands which constitute a DSL to "build up" an ENDPOINT URL.
#

export SCRIPT_HOME="$(cd $(dirname $0); pwd)/demo"

source "$SCRIPT_HOME/shell.subr" || {
	echo "unable to load subroutines" >/dev/stderr
	exit 255
}


# usage
#
#	Display help text for how to successfully invoke the program.
#
usage () {
	cat - >/dev/stderr <<EOT
usage:

$0 -h

or

$0 [-tv] [gateway] <microservice> <DSL> <command> [command args]


Flags:

	-h	Show this help information
	-t	Print $CURL timing (implies verbosity)
	-v	Enable verbosity


Where microservice is one of:

	company
	inventory		TBD
	purchase-order		TBD
	storage-facility


'DSL' is a combination of modifiers determining how a resource
endpoint is interacted with.  The supported commands are:

$(
	cd $SCRIPT_HOME/commands
	ls -1 |
		grep -v -e '^company$' \
			-e '^gateway$' \
			-e '^storage-facility$' |
		xargs what |
		paste -d ' ' - - |
		column --table \
			--table-columns-limit 2 \
			--table-columns COMMAND,DESCRIPTION |
		sed -e 's/^/\t/'
)


Example invocations:

	$0 company get
	$0 company post -f yoda-panda
	$0 company item '<uuid>' activate post 'version=<current>'
	$0 storage-facility heartbeat get
	$0 storage-facility yoda-panda get
	$0 storage-facility yoda-panda item '<uuid>' get
	$0 storage-facility yoda-panda item '<uuid>' delete
	$0 storage-facility shutdown put 'message="Stopping instance"'

To route traffic through the APISIX gateway, prefix service selection
with 'gateway'.  For example:

	$0 gateway storage-facility heartbeat get


Note that command arguments which have string content should be
quoted so that the shell does not consume the quotes which define
the content.
EOT

	exit 1
}


########################################################################
# MAIN
########################################################################

export LAST_COMMAND_ARGS
export QUERY_PARAMETERS=""

DSL=""
INVOCATION="$@"
MICROSERVICE=""


# Default to printing how to use the program.
if [[ $# = 0 ]]
then
	usage
fi

# Process command line arguments, stopping if an unknown flag is
# detected.
while getopts 'htv' arg
do
	case "$arg" in
		h)
			usage
			;;

		t)
			export TIME_REQUEST=true
			export VERBOSE=true
			;;

		v)
			export VERBOSE=true
			;;

		--)
			break
			;;

		?)
			err 127 "\nSee '$0 -h'."
			;;
	esac
done

shift $((OPTIND - 1))

MICROSERVICE="$COMMANDS/$1"

[[ -f "$MICROSERVICE" ]] || err 4 "could not find microservice: $1"

shift

# Scan the invocation, looking for the last subcommand
# specified.  All arguments up to and including it will be
# retained in DSL.  All arguments after the last subcommand
# will be exported in LAST_COMMAND_ARGS.
for arg in "$@"
do
	if [[ -f "$COMMANDS/$arg" ]]
	then
		DSL="$DSL ${LAST_COMMAND_ARGS:-} $arg"
		LAST_COMMAND_ARGS=""
	else
		LAST_COMMAND_ARGS="$LAST_COMMAND_ARGS $arg"
	fi
done

[[ -z "${DSL:-}" ]] && err 5 "could not find terminal command in: '$INVOCATION'"

exec $SH $MICROSERVICE $DSL

