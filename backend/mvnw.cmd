@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch Script
@REM
@REM This script executes the Maven toolkit.
@REM
@REM $Id: mvn.bat 1374528 2012-08-19 20:07:05Z hboutemy $
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Set up the Maven Home
@REM ----------------------------------------------------------------------------
@IF "%MAVEN_HOME%"=="" (
    @FOR %%i IN (%~dp0.) DO @SET MAVEN_HOME=%%~dpi..
)

@REM ----------------------------------------------------------------------------
@REM Set up the command line
@REM ----------------------------------------------------------------------------
@SET MAVEN_CMD_LINE_ARGS=%*

@REM For Maven 2.x, the MAVEN_OPTS should be specified in the mavenrc file.
@REM For example:
@REM SET MAVEN_OPTS="-Xms128m -Xmx512m"
@IF "%MAVEN_OPTS%"=="" (
    @SET MAVEN_OPTS="-Xms256m -Xmx512m"
)

@REM For Maven 3.x, use `MAVEN_DEBUG_OPTS` to set debug options.
@REM For example:
@REM SET MAVEN_DEBUG_OPTS="-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"
@REM
@REM For Maven 2.x, use `MAVEN_DEBUG_OPTS` to set debug options.
@REM For example:
@REM SET MAVEN_DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"

@IF NOT "%MAVEN_DEBUG_OPTS%"=="" (
    @SET MAVEN_OPTS=%MAVEN_OPTS% %MAVEN_DEBUG_OPTS%
)

@REM ----------------------------------------------------------------------------
@REM Run the Maven command
@REM ----------------------------------------------------------------------------
@SET LOCAL_MAVEN_REPOSITORY=
@IF NOT "%M2_HOME%"=="" (
  @SET LOCAL_MAVEN_REPOSITORY=-Dmaven.multiModuleProjectDirectory="%M2_HOME%"
)

@REM Check for JAVA_HOME
@IF "%JAVA_HOME%"=="" (
    @ECHO ERROR: JAVA_HOME not found in your environment.
    @ECHO Please set the JAVA_HOME variable in your environment to match the
    @ECHO location of your Java installation.
    @GOTO END
)

@REM Execute Maven
"%JAVA_HOME%\bin\java.exe" %MAVEN_OPTS% %LOCAL_MAVEN_REPOSITORY% ^
  -classpath "%MAVEN_HOME%\boot\plexus-classworlds-*.jar" ^
  "-Dmaven.home=%MAVEN_HOME%" ^
  org.codehaus.plexus.classworlds.launcher.Launcher %MAVEN_CMD_LINE_ARGS%

:END
