#!/usr/bin/env bash
. /etc/profile
APPNAME=PhenominerExpectedRanges
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAIL_LIST=jthota@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu,jthota@mcw.edu,jdepons@mcw.edu
fi
cd $APPDIR
pwd
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
export PHENOMINER_EXPECTED_RANGES_OPTS="$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@" | tee run.log
mailx -s "[$SERVER] Phenominer Expected Ranges Pipeline OK" $EMAIL_LIST < run.log