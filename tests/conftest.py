from __future__ import annotations

import os
import sys
from pathlib import Path

os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")
os.environ.setdefault("XDG_DATA_HOME", str(Path("/tmp") / "taskclock-test-data"))

PROJECT_ROOT = Path(__file__).resolve().parents[1] / "projects" / "TaskClock"
if not PROJECT_ROOT.exists():
    PROJECT_ROOT = Path(__file__).resolve().parents[1] / "02_Projects" / "TaskClock"
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from taskclock_qt.qt_bootstrap import ensure_qt_runtime

ensure_qt_runtime()
