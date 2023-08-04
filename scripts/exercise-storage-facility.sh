#!/bin/sh
# @(#) exercise-storage-facility.sh
#

DRIVER="$(dirname $0)/demo.sh"
COMPANY="yoda-panda"

[[ -x $DRIVER ]] || {
	echo "unable to locate driver script: '$DRIVER'" > /dev/stderr
	exit 1
}


deleteAll () {
	$DRIVER storage-facility $COMPANY get |
		jq '.facilities | .[] | .id' |
		while read id
		do
			$DRIVER storage-facility $COMPANY item $id delete || {
				echo "unable to delete facility: $id" > /dev/stderr
				exit 100
			}
		done
}


########################################################################
# MAIN
########################################################################

deleteAll || {
	echo "unable to delete all facilities" > /dev/stderr
	exit 100
}

echo "Creating Atlanta facility\n"
$DRIVER -v storage-facility $COMPANY put -f atlanta-facility

echo "\nExisting facilities\n"
$DRIVER storage-facility $COMPANY get | jq .

echo "\nCreating Denver facility\n"
$DRIVER -v storage-facility $COMPANY put -f denver-facility

echo "\nExisting facilities\n"
$DRIVER storage-facility $COMPANY get | jq .

echo "\nPerforming duplicate creation (should succeed)\n"
$DRIVER storage-facility $COMPANY put -f denver-facility | jq .

