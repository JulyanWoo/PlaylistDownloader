@echo off
set MAVEN_OPTS=--enable-native-access=javafx.graphics --add-opens=java.base/sun.misc=ALL-UNNAMED
mvn javafx:run