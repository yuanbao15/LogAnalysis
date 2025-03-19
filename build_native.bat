@echo off
chcp 65001
setlocal enabledelayedexpansion
echo 正在构建native image...

rem 如果有release 作清空处理，无则创建release目录
if exist "release" rmdir /s /q "release"
if not exist "release" mkdir release

:: 设置Visual Studio环境变量
set VS2022_PATH=D:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools
call "%VS2022_PATH%\VC\Auxiliary\Build\vcvars64.bat"

:: 设置JAVA_HOME为GraalVM路径
set JAVA_HOME=%JAVA_GRAALVM_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%

:: 首先构建JAR文件
echo 正在构建JAR文件...
call mvn clean package -Dfile.encoding=UTF-8

:: 使用native-image构建可执行文件
echo 正在生成本地可执行文件...
:: 能生成exe，可运行，但依赖jar包，不能独立运行
:: call native-image -jar target/LogAnalysis-1.2-SNAPSHOT.jar -H:Path=release -H:Name=LogAnalysis-1.2 -H:Class=com.idea.LogAnalysisUI --verbose LogAnalysis
:: 能生成exe，不可运行，运行报错awt问题
:: call native-image -jar target/LogAnalysis-1.2-SNAPSHOT.jar -H:Path=release -H:Name=LogAnalysis-1.2 -H:Class=com.idea.LogAnalysisUI -H:ConfigurationFileDirectories=META-INF/native-image -H:IncludeResources=.* --initialize-at-run-time=java.awt,javax.swing --no-fallback --static --verbose LogAnalysis
:: 不能生成exe，打exe报错：error LNK2001: 无法解析的外部符号 Java_jdk_internal_misc_VM_getNanoTimeAdjustment
call native-image -jar target/LogAnalysis-1.2-SNAPSHOT.jar -H:Path=release -H:Name=LogAnalysis_1.2 -H:Class=com.idea.LogAnalysisUI -H:ConfigurationFileDirectories=META-INF/native-image -H:IncludeResources=.* -H:+JNI -H:+AddAllCharsets -H:+ReportExceptionStackTraces -H:CLibraryPath=%JAVA_HOME%\lib -H:CLibraryPath=%JAVA_HOME%\bin --initialize-at-run-time=java.awt,javax.swing,sun.awt,sun.java2d,com.sun.java.swing --initialize-at-build-time=com.idea.LogAnalysisUI --no-fallback --static --verbose LogAnalysis


:: 复制必要的文件到release目录
echo 正在复制相关文件到release目录...
copy target\LogAnalysis-1.2-SNAPSHOT.jar release\
if exist "README.md" copy README.md release\

:: 复制必要的DLL文件
echo 正在复制必要的DLL文件...
copy "%JAVA_HOME%\bin\awt.dll" release\
copy "%JAVA_HOME%\bin\jawt.dll" release\
copy "%JAVA_HOME%\bin\fontmanager.dll" release\
copy "%JAVA_HOME%\bin\freetype.dll" release\
copy "%JAVA_HOME%\bin\javajpeg.dll" release\
copy "%JAVA_HOME%\bin\lcms.dll" release\
copy "%JAVA_HOME%\bin\mlib_image.dll" release\

echo 构建完成！
echo 所有文件已生成在release目录下!