./record_android_trace -o trace.perfetto-trace -t 5s -b 32mb \
    sched freq idle am wm gfx view binder_driver dalvik input \
    -a com.pchmn.pixelishsearch