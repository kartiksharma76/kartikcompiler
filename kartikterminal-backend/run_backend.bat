@echo off
echo ==============================================
echo   Cleaning and Running KartikTerminal Backend
echo ==============================================
java "-Dmaven.multiModuleProjectDirectory=." -cp maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain spring-boot:run
pause
