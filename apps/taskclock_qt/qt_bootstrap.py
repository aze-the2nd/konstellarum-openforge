from __future__ import annotations

import ctypes
from pathlib import Path


def ensure_qt_runtime() -> None:
    """Load a bundled libEGL shim when the host system does not provide one.

    Qt 6 widgets on some minimal Linux environments fail to import because
    libEGL.so.1 is missing. The TaskClock Qt release ships a tiny compatibility
    shim so the app can still start in those environments.
    """

    try:
        ctypes.CDLL("libEGL.so.1")
        return
    except OSError:
        pass

    bundled = Path(__file__).resolve().parent / "native" / "libEGL.so.1"
    if bundled.exists():
        ctypes.CDLL(str(bundled), mode=ctypes.RTLD_GLOBAL)
