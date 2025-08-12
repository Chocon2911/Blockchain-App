@ECHO OFF
setlocal

set MVNW_REPOURL=https://repo.maven.apache.org/maven2
set WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set JAVA_EXE=java
"%JAVA_EXE%" -version >NUL 2>&1
if ERRORLEVEL 1 (
  echo Java executable not found. Please install Java and ensure it's on your PATH.
  exit /b 1
)

if not exist %WRAPPER_JAR% (
  echo Downloading Maven Wrapper jar...
  powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar -OutFile '%WRAPPER_JAR%'" || (
    echo Failed to download Maven Wrapper jar.
    exit /b 1
  )
)

"%JAVA_EXE%" -cp %WRAPPER_JAR% %WRAPPER_LAUNCHER% %*
