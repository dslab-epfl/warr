#!/bin/bash
svn co http://src.chromium.org/svn/trunk/tools/depot_tools
export PATH="$PATH":`pwd`/depot_tools
gclient config http://src.chromium.org/svn/trunk/src 
gclient sync --revision src@71331

cp -R WaRRRecorder/* src/third_party/WebKit/Source/WebCore/

