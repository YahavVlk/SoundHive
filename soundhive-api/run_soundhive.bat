@echo off
echo ===============================
echo Building SoundHive backend...
echo ===============================
call .\mvnw.cmd clean package -DskipTests

IF EXIST target\soundhive-api-0.0.1-SNAPSHOT.jar (
    echo ===============================
    echo Starting server from JAR...
    echo ===============================
    java -jar target\soundhive-api-0.0.1-SNAPSHOT.jar
) ELSE (
    echo.
    echo ❌ Failed to build JAR. Please check for compile errors.
    pause
)
