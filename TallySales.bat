@echo off

REM ## Running sample
REM .\TallySales.bat

SET BASEDIR=%~dp0
echo %BASEDIR%

REM jar -cvf TallySales.jar -C target\classes .

set jars="TallySales.jar;%BASEDIR%*;%BASEDIR%\lib\*"

set mainclass=jp.co.local.TallySales
REM echo java -Xmx1024m -cp "%jars%" "%mainclass%" %*
java -Xmx1024m -cp "%jars%" "%mainclass%" %*