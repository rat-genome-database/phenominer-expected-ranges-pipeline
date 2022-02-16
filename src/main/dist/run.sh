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

java -Dspring.config=$APPDIR/../properties/default_db.xml \
     -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
     -jar lib/$APNNAME.jar "$@" | tee run.log

mailx -s "[$SERVER] Phenominer Expected Ranges Pipeline OK" $EMAIL_LIST < run.log