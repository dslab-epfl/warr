This wiki helps you get started with WaRR.


### Introduction ###
WaRR has 2 components:
  * WaRRRecorder, a Chromium-based browser that logs users' clicks and keystrokes
  * WaRRReplayer, a Selenium-based replay tool

### Building WaRR ###

  1. Get Chromium source code and replace parts of it with the WaRRRecorder
    1. run `setUpWaRRRecorder.sh` (first time only)
    1. troubleshooting: http://code.google.com/p/chromium/wiki/LinuxBuildInstructions

  1. Build WaRR
    1. run `buildWaRR.sh`

### Using WaRR ###
  1. Record user actions (run the WaRRRecorder)
    1. run `src/out/Release/chrome`
    1. a user's clicks and keystrokes are saved in `/home/<username>/WaRRRecordedCommands.warr`

  1. Replay user actions (run the WaRRReplayer)
    1. `cd WaRRReplayer`
    1. `ant WaRRReplayer -Dchrome=<path to WaRRRecorder> -Dwarr_file=<path to a warr file containing WaRR actions>`
    1. Examples
      1. run `ant WaRRReplayer -Dchrome=../src/out/Release/chrome -Dwarr_file=searchForWaRRonGoogle.warr`
      1. run `ant WaRRReplayer -Dchrome=../src/out/Release/chrome -Dwarr_file=sendEmail.warr`
      1. run `ant WaRRReplayer -Dchrome=../src/out/Release/chrome -Dwarr_file=dragAndDrop.warr` (need to open `dragAndDrop.warr` and change the URL to the local file)

  1. Notes
    1. Built and tested on Linux 11.10 64 bits
    1. Built on Linux 11.04 64 bits
    1. Does not compile with GCC 4.6 (Chrome limitation)