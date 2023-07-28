#!/bin/sh
## runnning sample
#jar -cvf TallySales.jar -C target/classes .

BASEDIR=`dirname $0`
echo $BASEDIR

jars="TallySales.jar:$BASEDIR/*:$BASEDIR/lib/*"
mainclass='jp.co.local.TallySales'

#echo java -Xmx1024m -cp="$jars" "$mainclass" $*
java -Xmx1024m -cp "$jars" "$mainclass"