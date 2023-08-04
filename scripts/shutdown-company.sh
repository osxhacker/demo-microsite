#!/usr/bin/env sh
# @(#) Driver script for shutting down the company microservice
#

DEMO_SH="$(dirname $0)/demo.sh"
export SCRIPT_HOME="$(cd $(dirname $0); pwd)/demo"
source $SCRIPT_HOME/shell.subr


allCompanies() {
	$DEMO_SH company get |
		$JQ '.companies | .[]._links.delete.href' |
		sed -e 's/"//g' -e 's,.*/,,'
}


deleteCompany() {
	$DEMO_SH company item $1 delete
}


########################################################################
# MAIN
########################################################################

# For the purposes of this demo, whenever shutting down the company
# microservice, first ensure all companies are deleted.  This allows
# other microservices to remove them.
allCompanies | while read id
do
	deleteCompany $id
done

$DEMO_SH company shutdown put "message='${1:-stopping}'"

