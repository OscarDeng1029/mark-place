import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / "python_packages" / "adb_shell-0.4.4"))
from adb_shell.adb_device import AdbDeviceTcp

device = AdbDeviceTcp("127.0.0.1", 5555)
device.connect(rsa_keys=None, auth_timeout_s=0.1, transport_timeout_s=30, read_timeout_s=30)
device.shell("input tap 635 1438")
time.sleep(2)
device.shell("screencap -p /sdcard/MyLittleAtlas-profile.png")
device.pull(
    "/sdcard/MyLittleAtlas-profile.png",
    "dist/MyLittleAtlas-profile.png",
    transport_timeout_s=60,
    read_timeout_s=60,
)
device.shell("input tap 310 1290")
time.sleep(2)
device.shell("screencap -p /sdcard/MyLittleAtlas-journal.png")
device.pull(
    "/sdcard/MyLittleAtlas-journal.png",
    "dist/MyLittleAtlas-journal.png",
    transport_timeout_s=60,
    read_timeout_s=60,
)
device.close()
