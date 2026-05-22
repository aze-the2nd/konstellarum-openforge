from __future__ import annotations

import csv
from datetime import datetime, timezone
from pathlib import Path

import pytest

from apps.taskclock_qt.core import TaskClockStore


class Clock:
    def __init__(self, *values: datetime):
        self._values = list(values)

    def __call__(self) -> datetime:
        if not self._values:
            raise AssertionError("clock exhausted")
        return self._values.pop(0)


def dt(value: str) -> datetime:
    return datetime.fromisoformat(value).astimezone(timezone.utc)


def test_stamp_switches_active_task_and_records_sessions(tmp_path: Path) -> None:
    store = TaskClockStore(
        clock=Clock(
            dt("2026-05-22T12:00:00+00:00"),
            dt("2026-05-22T12:03:00+00:00"),
            dt("2026-05-22T12:03:00+00:00"),
        )
    )
    alpha = store.add_task("Alpha")
    beta = store.add_task("Beta")

    store.stamp(alpha.id)
    assert store.active_task().name == "Alpha"

    store.stamp(beta.id)
    assert store.active_task().name == "Beta"
    assert alpha.total_seconds == 180
    assert alpha.sessions == [
        {
            "task": "Alpha",
            "start": "2026-05-22T12:00:00+00:00",
            "end": "2026-05-22T12:03:00+00:00",
            "seconds": 180,
        }
    ]


@pytest.mark.parametrize("value", ["2026-05-22T12:34:56+00:00", "2026-05-22T12:34:56"])
def test_json_roundtrip_preserves_tasks(tmp_path: Path, value: str) -> None:
    store = TaskClockStore(clock=Clock(dt("2026-05-22T12:00:00+00:00")))
    task = store.add_task("Design")
    task.active_since = value
    task.total_seconds = 42
    task.sessions.append(
        {
            "task": "Design",
            "start": "2026-05-22T12:00:00+00:00",
            "end": "2026-05-22T12:00:42+00:00",
            "seconds": 42,
        }
    )

    path = tmp_path / "taskclock.json"
    store.save(path)

    loaded = TaskClockStore.load(path, clock=Clock(dt("2026-05-22T12:10:00+00:00")))
    assert [task.name for task in loaded.tasks] == ["Design"]
    assert loaded.tasks[0].total_seconds == 42
    assert loaded.tasks[0].active_since == value
    assert loaded.tasks[0].sessions == task.sessions


def test_export_csv_writes_sessions_with_duration(tmp_path: Path) -> None:
    store = TaskClockStore(clock=Clock(dt("2026-05-22T12:00:00+00:00")))
    task = store.add_task("Write docs")
    task.sessions.append(
        {
            "task": "Write docs",
            "start": "2026-05-22T12:00:00+00:00",
            "end": "2026-05-22T12:07:30+00:00",
            "seconds": 450,
        }
    )

    path = tmp_path / "export.csv"
    store.export_csv(path)

    rows = list(csv.reader(path.open(newline="", encoding="utf-8")))
    assert rows == [
        ["task", "start", "end", "seconds", "duration"],
        ["Write docs", "2026-05-22T12:00:00+00:00", "2026-05-22T12:07:30+00:00", "450", "00:07:30"],
    ]


def test_delete_active_task_is_rejected() -> None:
    store = TaskClockStore(clock=Clock(dt("2026-05-22T12:00:00+00:00")))
    task = store.add_task("Busy")
    store.stamp(task.id)

    with pytest.raises(ValueError):
        store.delete_task(task.id)
