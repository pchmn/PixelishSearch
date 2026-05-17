#!/bin/bash
#
# Capture a 5s Perfetto trace covering a cold start of PixelishSearch.
# Output: trace.perfetto-trace (gitignored). Drop it into https://ui.perfetto.dev
#
# Prerequisite — Google's helper script, downloaded once:
#   curl -O https://raw.githubusercontent.com/google/perfetto/master/tools/record_android_trace
#   chmod +x record_android_trace
#
set -e

if [ ! -x ./record_android_trace ]; then
    echo "record_android_trace not found. Run:" >&2
    echo "  curl -O https://raw.githubusercontent.com/google/perfetto/master/tools/record_android_trace" >&2
    echo "  chmod +x record_android_trace" >&2
    exit 1
fi

# Start capture in the background.
./record_android_trace -o trace.perfetto-trace -t 5s -b 32mb \
    sched freq idle am wm gfx view binder_driver dalvik input \
    -a com.pchmn.pixelishsearch &
CAPTURE_PID=$!

# Give perfetto a moment to attach atrace categories before we trigger the cold start.
sleep 1

# Force-stop ensures the next launch is a real cold start (process killed).
adb shell am force-stop com.pchmn.pixelishsearch
adb shell am start -n com.pchmn.pixelishsearch/.search.MainActivity

# Wait for the capture to finish writing trace.perfetto-trace.
wait $CAPTURE_PID
echo "Trace saved to trace.perfetto-trace"
