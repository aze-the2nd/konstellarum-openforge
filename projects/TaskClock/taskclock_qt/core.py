from __future__ import annotations

import csv
import json
import os
import platform
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable, Iterable
from uuid import uuid4

Clock = Callable[[], datetime]

APP_NAME = "TaskClock"
APP_VERSION = "1.4.3"
APP_TITLE = f"TaskClock Qt v{APP_VERSION}"


def now_utc() -> datetime:
    return datetime.now(timezone.utc)


def app_data_dir() -> Path:
    system = platform.system()
    if system == "Windows":
        base = Path(os.environ.get("APPDATA", Path.home() / "AppData" / "Roaming"))
    elif system == "Darwin":
        base = Path.home() / "Library" / "Application Support"
    else:
        base = Path(os.environ.get("XDG_DATA_HOME", Path.home() / ".local" / "share"))
    path = base / APP_NAME
    path.mkdir(parents=True, exist_ok=True)
    return path


def data_file() -> Path:
    return app_data_dir() / "taskclock_data.json"


def log_file() -> Path:
    return app_data_dir() / "taskclock_error.log"


def parse_dt(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value)


def fmt_seconds(seconds: int) -> str:
    hours, rem = divmod(max(0, seconds), 3600)
    minutes, secs = divmod(rem, 60)
    return f"{hours:02d}:{minutes:02d}:{secs:02d}"


def default_export_filename(moment: datetime | None = None) -> str:
    stamp = (moment or now_utc()).astimezone().strftime("%Y-%m-%d_%H%M")
    return f"taskclock_{stamp}.csv"


@dataclass
class Task:
    id: str
    name: str
    total_seconds: int = 0
    active_since: str | None = None
    sessions: list[dict[str, Any]] = field(default_factory=list)

    @property
    def is_active(self) -> bool:
        return self.active_since is not None

    def current_seconds(self, clock: Clock = now_utc) -> int:
        started = parse_dt(self.active_since)
        if not started:
            return self.total_seconds
        return self.total_seconds + int((clock() - started).total_seconds())

    def start(self, clock: Clock = now_utc) -> None:
        if not self.is_active:
            self.active_since = clock().isoformat()

    def stop(self, clock: Clock = now_utc) -> None:
        started = parse_dt(self.active_since)
        if not started:
            return
        ended = clock()
        seconds = max(0, int((ended - started).total_seconds()))
        self.total_seconds += seconds
        self.sessions.append(
            {
                "task": self.name,
                "start": started.isoformat(),
                "end": ended.isoformat(),
                "seconds": seconds,
            }
        )
        self.active_since = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "name": self.name,
            "total_seconds": self.total_seconds,
            "active_since": self.active_since,
            "sessions": self.sessions,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "Task":
        return cls(
            id=str(data.get("id") or uuid4()),
            name=str(data.get("name", "Unbenannte Task")).strip() or "Unbenannte Task",
            total_seconds=int(data.get("total_seconds", 0)),
            active_since=data.get("active_since"),
            sessions=list(data.get("sessions", [])),
        )


class TaskClockStore:
    def __init__(
        self,
        tasks: Iterable[Task] | None = None,
        clock: Clock = now_utc,
        workdays: Iterable[dict[str, Any]] | None = None,
    ) -> None:
        self.tasks: list[Task] = list(tasks or [])
        self.clock = clock
        self.workdays: list[dict[str, Any]] = list(workdays or [])

    def active_task(self) -> Task | None:
        return next((task for task in self.tasks if task.is_active), None)

    def add_task(self, name: str) -> Task:
        task = Task(id=uuid4().hex, name=name.strip() or "Unbenannte Task")
        self.tasks.append(task)
        return task

    def delete_task(self, task_id: str) -> None:
        task = self.get_task(task_id)
        if task is None:
            raise KeyError(task_id)
        if task.is_active:
            raise ValueError("active task must be stopped before deleting")
        self.tasks.remove(task)

    def get_task(self, task_id: str) -> Task | None:
        return next((task for task in self.tasks if task.id == task_id), None)

    def stop_active(self) -> Task | None:
        active = self.active_task()
        if active:
            active.stop(self.clock)
        return active

    def stamp(self, task_id: str) -> Task:
        task = self.get_task(task_id)
        if task is None:
            raise KeyError(task_id)

        active = self.active_task()
        if active is task:
            task.stop(self.clock)
        else:
            if active is not None:
                active.stop(self.clock)
            task.start(self.clock)
        return task

    def to_dict(self) -> dict[str, Any]:
        return {"tasks": [task.to_dict() for task in self.tasks], "workdays": self.workdays}

    def grand_total_seconds(self) -> int:
        return sum(task.current_seconds(self.clock) for task in self.tasks)

    def start_new_workday(self) -> dict[str, Any]:
        ended_at = self.clock().isoformat()
        self.stop_active()
        archive = {
            "id": uuid4().hex,
            "ended_at": ended_at,
            "total_seconds": sum(task.total_seconds for task in self.tasks),
            "tasks": [task.to_dict() for task in self.tasks],
        }
        self.workdays.append(archive)
        for task in self.tasks:
            task.total_seconds = 0
            task.active_since = None
            task.sessions.clear()
        return archive

    def save(self, path: Path | None = None) -> None:
        target = path or data_file()
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(self.to_dict(), indent=2, ensure_ascii=False), encoding="utf-8")

    @classmethod
    def load(cls, path: Path | None = None, clock: Clock = now_utc) -> "TaskClockStore":
        source = path or data_file()
        if not source.exists():
            return cls(clock=clock)
        data = json.loads(source.read_text(encoding="utf-8"))
        tasks = [Task.from_dict(item) for item in data.get("tasks", [])]
        return cls(tasks=tasks, clock=clock, workdays=list(data.get("workdays", [])))

    def session_rows(self) -> list[dict[str, Any]]:
        rows: list[dict[str, Any]] = []
        for task in self.tasks:
            for session in task.sessions:
                seconds = int(session.get("seconds", 0))
                rows.append(
                    {
                        "task": session.get("task", task.name),
                        "start": session.get("start", ""),
                        "end": session.get("end", ""),
                        "seconds": seconds,
                        "duration": fmt_seconds(seconds),
                    }
                )
        return rows

    def export_csv(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=["task", "start", "end", "seconds", "duration"])
            writer.writeheader()
            writer.writerows(self.session_rows())
