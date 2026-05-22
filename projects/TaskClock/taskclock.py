#!/usr/bin/env python3
"""TaskClock: minimale Stand-alone-GUI zur Projektzeit-Stempelung."""

from __future__ import annotations

import csv
import json
import os
import platform
import sys
import tkinter as tk
import traceback
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from tkinter import filedialog, font, messagebox, simpledialog, ttk
from typing import Any

APP_NAME = "TaskClock"
APP_VERSION = "1.3.2"
APP_TITLE = f"{APP_NAME} v{APP_VERSION}"


def app_data_dir() -> Path:
    """Return a writable per-user data directory on Windows, macOS and Linux."""
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


DATA_FILE = app_data_dir() / "taskclock_data.json"
LOG_FILE = app_data_dir() / "taskclock_error.log"

COLORS = {
    "bg": "#0f172a",
    "surface": "#111827",
    "card": "#1f2937",
    "card_alt": "#273449",
    "text": "#e5e7eb",
    "muted": "#94a3b8",
    "primary": "#38bdf8",
    "primary_dark": "#0284c7",
    "danger": "#f87171",
    "border": "#334155",
}


def now_utc() -> datetime:
    return datetime.now(timezone.utc)


def parse_dt(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value)


def fmt_seconds(seconds: int) -> str:
    hours, rem = divmod(max(0, seconds), 3600)
    minutes, secs = divmod(rem, 60)
    return f"{hours:02d}:{minutes:02d}:{secs:02d}"


@dataclass
class Task:
    name: str
    total_seconds: int = 0
    active_since: str | None = None
    sessions: list[dict[str, Any]] = field(default_factory=list)

    @property
    def is_active(self) -> bool:
        return self.active_since is not None

    def current_seconds(self) -> int:
        started = parse_dt(self.active_since)
        if not started:
            return self.total_seconds
        return self.total_seconds + int((now_utc() - started).total_seconds())

    def start(self) -> None:
        if not self.is_active:
            self.active_since = now_utc().isoformat()

    def stop(self) -> None:
        started = parse_dt(self.active_since)
        if not started:
            return
        ended = now_utc()
        seconds = max(0, int((ended - started).total_seconds()))
        self.total_seconds += seconds
        self.sessions.append(
            {"task": self.name, "start": started.isoformat(), "end": ended.isoformat(), "seconds": seconds}
        )
        self.active_since = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "name": self.name,
            "total_seconds": self.total_seconds,
            "active_since": self.active_since,
            "sessions": self.sessions,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "Task":
        return cls(
            name=str(data.get("name", "Unbenannte Task")),
            total_seconds=int(data.get("total_seconds", 0)),
            active_since=data.get("active_since"),
            sessions=list(data.get("sessions", [])),
        )


class TaskClockApp(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title(APP_TITLE)
        self.geometry("760x520")
        self.minsize(680, 440)
        self.configure(bg=COLORS["bg"])

        self.tasks: list[Task] = self.load_tasks()
        self.pinned = tk.BooleanVar(value=False)
        self.active_title = tk.StringVar(value="Keine aktive Task")
        self.active_time = tk.StringVar(value="00:00:00")
        self.status_text = tk.StringVar(value="Bereit")

        self.setup_style()
        self.build_ui()
        self.refresh()
        self.protocol("WM_DELETE_WINDOW", self.on_close)

    def setup_style(self) -> None:
        # Font families with spaces must be grouped for Tcl/Tk; otherwise
        # Tk parses "Segoe UI 11" as family="Segoe", size="UI" and crashes.
        self.option_add("*Font", "{Segoe UI} 11")
        default = font.nametofont("TkDefaultFont")
        default.configure(family="Segoe UI", size=11)

        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure("App.TFrame", background=COLORS["bg"])
        style.configure("Card.TFrame", background=COLORS["surface"], relief="flat")
        style.configure("Title.TLabel", background=COLORS["bg"], foreground=COLORS["text"], font=("Segoe UI", 22, "bold"))
        style.configure("Muted.TLabel", background=COLORS["bg"], foreground=COLORS["muted"], font=("Segoe UI", 10))
        style.configure("CardTitle.TLabel", background=COLORS["surface"], foreground=COLORS["muted"], font=("Segoe UI", 10, "bold"))
        style.configure("Timer.TLabel", background=COLORS["surface"], foreground=COLORS["primary"], font=("Consolas", 34, "bold"))
        style.configure("Active.TLabel", background=COLORS["surface"], foreground=COLORS["text"], font=("Segoe UI", 15, "bold"))
        style.configure("Status.TLabel", background=COLORS["bg"], foreground=COLORS["muted"], font=("Segoe UI", 9))

        style.configure("Primary.TButton", font=("Segoe UI", 12, "bold"), padding=(18, 12), borderwidth=0)
        style.map("Primary.TButton", background=[("active", COLORS["primary_dark"]), ("!disabled", COLORS["primary"])], foreground=[("!disabled", "#07111f")])
        style.configure("Secondary.TButton", font=("Segoe UI", 11), padding=(16, 10), borderwidth=0)
        style.map("Secondary.TButton", background=[("active", COLORS["card_alt"]), ("!disabled", COLORS["card"])], foreground=[("!disabled", COLORS["text"])])
        style.configure("Danger.TButton", font=("Segoe UI", 11), padding=(16, 10), borderwidth=0)
        style.map("Danger.TButton", background=[("active", "#dc2626"), ("!disabled", COLORS["danger"])], foreground=[("!disabled", "#1f0a0a")])
        style.configure("Modern.TCheckbutton", background=COLORS["bg"], foreground=COLORS["text"], font=("Segoe UI", 10), padding=8)
        style.map("Modern.TCheckbutton", background=[("active", COLORS["bg"])], foreground=[("active", COLORS["text"])])

        style.configure(
            "Modern.Treeview",
            background=COLORS["surface"],
            foreground=COLORS["text"],
            fieldbackground=COLORS["surface"],
            borderwidth=0,
            rowheight=42,
            font=("Segoe UI", 11),
        )
        style.configure(
            "Modern.Treeview.Heading",
            background=COLORS["card"],
            foreground=COLORS["muted"],
            relief="flat",
            font=("Segoe UI", 10, "bold"),
        )
        style.map("Modern.Treeview", background=[("selected", COLORS["primary_dark"])], foreground=[("selected", "white")])

    def build_ui(self) -> None:
        root = ttk.Frame(self, style="App.TFrame", padding=24)
        root.pack(fill=tk.BOTH, expand=True)

        header = ttk.Frame(root, style="App.TFrame")
        header.pack(fill=tk.X)
        ttk.Label(header, text=APP_TITLE, style="Title.TLabel").pack(side=tk.LEFT)
        ttk.Checkbutton(header, text="Fenster anpinnen", variable=self.pinned, command=self.toggle_pin, style="Modern.TCheckbutton").pack(side=tk.RIGHT)

        ttk.Label(root, text="Projektzeit stempeln. Minimal, klar, schnell.", style="Muted.TLabel").pack(anchor="w", pady=(2, 18))

        hero = ttk.Frame(root, style="Card.TFrame", padding=22)
        hero.pack(fill=tk.X, pady=(0, 18))
        ttk.Label(hero, text="AKTIVE TASK", style="CardTitle.TLabel").pack(anchor="w")
        ttk.Label(hero, textvariable=self.active_title, style="Active.TLabel").pack(anchor="w", pady=(4, 0))
        ttk.Label(hero, textvariable=self.active_time, style="Timer.TLabel").pack(anchor="w", pady=(8, 0))

        actions = ttk.Frame(root, style="App.TFrame")
        actions.pack(fill=tk.X, pady=(0, 14))
        ttk.Button(actions, text="+ Neue Task", command=self.add_task, style="Primary.TButton").pack(side=tk.LEFT)
        ttk.Button(actions, text="▶ / ■ Stempeln", command=self.stamp_selected, style="Secondary.TButton").pack(side=tk.LEFT, padx=10)
        ttk.Button(actions, text="Aktive stoppen", command=self.stop_active, style="Secondary.TButton").pack(side=tk.LEFT)
        ttk.Button(actions, text="CSV exportieren", command=self.export_csv, style="Secondary.TButton").pack(side=tk.LEFT, padx=10)
        ttk.Button(actions, text="Löschen", command=self.delete_selected, style="Danger.TButton").pack(side=tk.RIGHT)

        table_card = ttk.Frame(root, style="Card.TFrame", padding=1)
        table_card.pack(fill=tk.BOTH, expand=True)
        self.tree = ttk.Treeview(table_card, columns=("status", "task", "time"), show="headings", style="Modern.Treeview", selectmode="browse")
        self.tree.heading("status", text="")
        self.tree.heading("task", text="Task")
        self.tree.heading("time", text="Zeit")
        self.tree.column("status", width=54, minwidth=54, anchor="center", stretch=False)
        self.tree.column("task", width=420, anchor="w")
        self.tree.column("time", width=130, anchor="e", stretch=False)
        self.tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.tree.bind("<Double-Button-1>", lambda _event: self.stamp_selected())
        self.tree.tag_configure("active", background="#123044", foreground="#e0f2fe")
        self.tree.tag_configure("idle", background=COLORS["surface"], foreground=COLORS["text"])

        scrollbar = ttk.Scrollbar(table_card, command=self.tree.yview)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.tree.configure(yscrollcommand=scrollbar.set)

        ttk.Label(root, textvariable=self.status_text, style="Status.TLabel").pack(anchor="w", pady=(10, 0))

    def load_tasks(self) -> list[Task]:
        if not DATA_FILE.exists():
            return []
        try:
            data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
            return [Task.from_dict(item) for item in data.get("tasks", [])]
        except (OSError, json.JSONDecodeError, TypeError, ValueError) as exc:
            messagebox.showwarning("TaskClock", f"Daten konnten nicht geladen werden: {exc}")
            return []

    def save_tasks(self) -> None:
        DATA_FILE.write_text(
            json.dumps({"tasks": [task.to_dict() for task in self.tasks]}, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )

    def active_task(self) -> Task | None:
        return next((task for task in self.tasks if task.is_active), None)

    def add_task(self) -> None:
        name = simpledialog.askstring("Neue Task", "Name der Task:", parent=self)
        if not name:
            return
        self.tasks.append(Task(name=name.strip()))
        self.status_text.set(f"Task angelegt: {name.strip()}")
        self.save_tasks()
        self.refresh()

    def selected_index(self) -> int | None:
        selection = self.tree.selection()
        if not selection:
            return None
        return int(selection[0])

    def selected_task(self) -> Task | None:
        index = self.selected_index()
        if index is None:
            messagebox.showinfo("TaskClock", "Bitte eine Task auswählen.")
            return None
        return self.tasks[index]

    def stamp_selected(self) -> None:
        task = self.selected_task()
        if not task:
            return
        active = self.active_task()
        if active is task:
            task.stop()
            self.status_text.set(f"Gestoppt: {task.name}")
        else:
            if active:
                active.stop()
            task.start()
            self.status_text.set(f"Aktiv: {task.name}")
        self.save_tasks()
        self.refresh()

    def stop_active(self) -> None:
        active = self.active_task()
        if active:
            active.stop()
            self.status_text.set(f"Gestoppt: {active.name}")
            self.save_tasks()
        else:
            self.status_text.set("Keine aktive Task")
        self.refresh()

    def delete_selected(self) -> None:
        task = self.selected_task()
        if not task:
            return
        if task.is_active:
            messagebox.showwarning("TaskClock", "Aktive Task zuerst stoppen.")
            return
        if messagebox.askyesno("Task löschen", f"„{task.name}“ löschen?"):
            self.tasks.remove(task)
            self.status_text.set(f"Gelöscht: {task.name}")
            self.save_tasks()
            self.refresh()

    def export_csv(self) -> None:
        path = filedialog.asksaveasfilename(
            title="CSV exportieren",
            defaultextension=".csv",
            filetypes=[("CSV", "*.csv")],
        )
        if not path:
            return
        with open(path, "w", newline="", encoding="utf-8") as handle:
            writer = csv.writer(handle)
            writer.writerow(["task", "start", "end", "seconds", "duration"])
            for task in self.tasks:
                for session in task.sessions:
                    seconds = int(session.get("seconds", 0))
                    writer.writerow([session.get("task", task.name), session.get("start", ""), session.get("end", ""), seconds, fmt_seconds(seconds)])
        self.status_text.set(f"CSV exportiert: {path}")
        messagebox.showinfo("TaskClock", "CSV exportiert.")

    def toggle_pin(self) -> None:
        self.attributes("-topmost", self.pinned.get())
        self.status_text.set("Fenster angepinnt" if self.pinned.get() else "Fenster nicht mehr angepinnt")

    def refresh(self) -> None:
        selected = self.selected_index()
        active = self.active_task()
        self.active_title.set(active.name if active else "Keine aktive Task")
        self.active_time.set(fmt_seconds(active.current_seconds()) if active else "00:00:00")

        self.tree.delete(*self.tree.get_children())
        for index, task in enumerate(self.tasks):
            status = "●" if task.is_active else "○"
            tag = "active" if task.is_active else "idle"
            self.tree.insert("", tk.END, iid=str(index), values=(status, task.name, fmt_seconds(task.current_seconds())), tags=(tag,))
        if selected is not None and selected < len(self.tasks):
            self.tree.selection_set(str(selected))
        self.after(1000, self.refresh)

    def on_close(self) -> None:
        self.save_tasks()
        self.destroy()


if __name__ == "__main__":
    try:
        TaskClockApp().mainloop()
    except Exception:
        LOG_FILE.write_text(traceback.format_exc(), encoding="utf-8")
        try:
            root = tk.Tk()
            root.withdraw()
            messagebox.showerror(APP_TITLE, f"TaskClock konnte nicht starten. Fehlerlog:\n{LOG_FILE}")
            root.destroy()
        except Exception:
            print(f"TaskClock konnte nicht starten. Fehlerlog: {LOG_FILE}", file=sys.stderr)
        raise
