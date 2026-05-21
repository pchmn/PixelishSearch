#!/bin/bash
#
# Capture a 5s Perfetto trace of PixelishSearch.
# Output: trace.perfetto-trace (gitignored). Drop it into https://ui.perfetto.dev
#
# Usage:
#   ./perfetto-trace.sh cold-start   # force-stop + start, measures cold launch
#   ./perfetto-trace.sh running      # records while the app is already open,
#                                    # for measuring in-app interactions
#
# Prerequisite — Google's helper script, downloaded once:
#   curl -O https://raw.githubusercontent.com/google/perfetto/master/tools/record_android_trace
#   chmod +x record_android_trace
#
set -e

MODE="${1:-}"
case "$MODE" in
    cold-start|running) ;;
    *)
        echo "Usage: $0 {cold-start|running}" >&2
        exit 1
        ;;
esac

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

# Give perfetto a moment to attach atrace categories before triggering work.
sleep 1

if [ "$MODE" = "cold-start" ]; then
    # Force-stop ensures the next launch is a real cold start (process killed).
    adb shell am force-stop com.pchmn.pixelishsearch
    adb shell am start -n com.pchmn.pixelishsearch/.search.MainActivity
else
    echo "Recording for ~4s — interact with the app now."
fi

# Wait for the capture to finish writing trace.perfetto-trace.
wait $CAPTURE_PID
echo "Trace saved to trace.perfetto-trace"
