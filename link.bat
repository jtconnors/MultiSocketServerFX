
REM
REM Change this variable
REM
SET REPO=C:\Users\jtconnor\.m2\repository\

SET MODPATH=%JAVA_HOME%\jmods
SET MAINMODULE=multisocketserverfx
SET MAINCLASS=com.jtconnors.multisocketserverfx.MultiSocketServerFX
SET LAUNCHER=MultiSocketServerFX

SET MODPATH=%JAVA_HOME%\jmods;target\classes;%REPO%com\jtconnors\com.jtconnors.socket\11.0.3\com.jtconnors.socket-11.0.3.jar;%REPO%org\openjfx\javafx-base\11.0.1\javafx-base-11.0.1.jar;%REPO%org\openjfx\javafx-controls\11.0.1\javafx-controls-11.0.1.jar;%REPO%org\openjfx\javafx-fxml\11.0.1\javafx-fxml-11.0.1.jar;%REPO%org\openjfx\javafx-graphics\11.0.1\javafx-graphics-11.0.1.jar;%REPO%org\openjfx\javafx-base\11.0.1\javafx-base-11.0.1-win.jar;%REPO%org\openjfx\javafx-controls\11.0.1\javafx-controls-11.0.1-win.jar;%REPO%org\openjfx\javafx-fxml\11.0.1\javafx-fxml-11.0.1-win.jar;%REPO%org\openjfx\javafx-graphics\11.0.1\javafx-graphics-11.0.1-win.jar

jlink --module-path %MODPATH% --add-modules %MAINMODULE% --compress=2 --launcher %LAUNCHER%=%MAINMODULE%/%MAINCLASS% --output image 

