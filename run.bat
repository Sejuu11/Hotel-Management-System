@echo off
REM ====================================================================
REM Launch Grand Palace Hotel Management System Desktop Application
REM ====================================================================

title Hotel Management System Launcher

IF NOT EXIST "target\hotel-management-system-1.0.0-jar-with-dependencies.jar" (
    echo Building project with Maven...
    call mvn clean package
)

echo Starting Grand Palace Hotel Management System...
java -jar "target\hotel-management-system-1.0.0-jar-with-dependencies.jar"
pause
