@echo off
%1 mshta vbscript:CreateObject("WScript.Shell").Run("%~s0 ::",0,FALSE)(window.close)&&exit
javaw -jar C:\Users\Administrator\Desktop\fileServer.jar
exit