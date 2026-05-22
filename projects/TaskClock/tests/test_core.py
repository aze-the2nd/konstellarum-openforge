from __future__ import annotations

from datetime import datetime, timezone

from taskclock_qt.core import Task, TaskClockStore, default_export_filename, fmt_seconds


class FrozenClock:
    def __init__(self, value: datetime) -> None:
        self.value = value

    def __call__(self) -> datetime:
        return self.value


def utc(hour: int, minute: int = 0, second: int = 0) -> datetime:
    return datetime(2026, 5, 22, hour, minute, second, tzinfo=timezone.utc)


def test_grand_total_seconds_includes_running_task_live() -> None:
    clock = FrozenClock(utc(9, 15))
    store = TaskClockStore(
        tasks=[
            Task(id="a", name="Planung", total_seconds=120),
            Task(id="b", name="Build", total_seconds=300, active_since=utc(9, 0).isoformat()),
        ],
        clock=clock,
    )

    assert store.grand_total_seconds() == 120 + 300 + 15 * 60
    assert fmt_seconds(store.grand_total_seconds()) == "00:22:00"


def test_default_export_filename_uses_date_and_hhmm() -> None:
    assert default_export_filename(utc(16, 7, 45)) == "taskclock_2026-05-22_1607.csv"


def test_start_new_workday_archives_current_day_and_resets_tasks() -> None:
    clock = FrozenClock(utc(10, 0))
    active = Task(id="a", name="Build", total_seconds=60, active_since=utc(9, 58).isoformat())
    inactive = Task(
        id="b",
        name="Review",
        total_seconds=180,
        sessions=[{"task": "Review", "start": utc(8).isoformat(), "end": utc(8, 3).isoformat(), "seconds": 180}],
    )
    store = TaskClockStore(tasks=[active, inactive], clock=clock)

    archive = store.start_new_workday()

    assert archive["total_seconds"] == 60 + 2 * 60 + 180
    assert archive["ended_at"] == utc(10).isoformat()
    assert archive["tasks"][0]["name"] == "Build"
    assert archive["tasks"][0]["total_seconds"] == 180
    assert store.workdays == [archive]
    assert [task.name for task in store.tasks] == ["Build", "Review"]
    assert all(task.total_seconds == 0 for task in store.tasks)
    assert all(not task.sessions for task in store.tasks)
    assert all(not task.is_active for task in store.tasks)
