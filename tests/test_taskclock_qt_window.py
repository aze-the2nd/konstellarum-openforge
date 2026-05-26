from __future__ import annotations

import os
os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")

from PySide6 import QtWidgets

from taskclock_qt.core import APP_VERSION, TaskClockStore
from taskclock_qt.window import TaskClockWindow


def test_window_title_and_status_text() -> None:
    app = QtWidgets.QApplication.instance() or QtWidgets.QApplication([])
    window = TaskClockWindow(TaskClockStore())
    try:
        assert window.windowTitle() == f"TaskClock Qt v{APP_VERSION}"
        assert window.status_label.text() == "Bereit"
    finally:
        window.close()
        app.processEvents()


def test_pin_toggle_updates_status_without_forced_reshow(monkeypatch) -> None:
    app = QtWidgets.QApplication.instance() or QtWidgets.QApplication([])
    window = TaskClockWindow(TaskClockStore())
    calls: list[bool] = []
    monkeypatch.setattr(window, "_set_stays_on_top", calls.append)
    try:
        window.pin_toggle.setChecked(True)
        assert calls == [True]
        assert window.pin_toggle.text() == "Anpinnen: An"
        assert window.status_label.text() == "Fenster angepinnt"

        window.pin_toggle.setChecked(False)
        assert calls == [True, False]
        assert window.pin_toggle.text() == "Anpinnen: Aus"
        assert window.status_label.text() == "Fenster nicht mehr angepinnt"
    finally:
        window.close()
        app.processEvents()


def test_theme_toggle_switches_between_dark_and_light_modes() -> None:
    app = QtWidgets.QApplication.instance() or QtWidgets.QApplication([])
    window = TaskClockWindow(TaskClockStore())
    try:
        assert window.theme_toggle.text() == "☀"
        dark_stylesheet = window.styleSheet()
        assert "background: #0b1220" in dark_stylesheet
        assert "color: #e5eefc" in dark_stylesheet
        assert "font-family: 'DejaVu Sans', 'Liberation Sans', 'Arial', sans-serif" in dark_stylesheet
        assert "font-family: 'JetBrains Mono', 'DejaVu Sans Mono', 'Liberation Mono', monospace" in dark_stylesheet

        window.theme_toggle.click()

        light_stylesheet = window.styleSheet()
        assert window.theme_toggle.text() == "☾"
        assert "background: #f5f7fb" in light_stylesheet
        assert "color: #162033" in light_stylesheet
        assert "color: #52627a" in light_stylesheet
    finally:
        window.close()
        app.processEvents()


def test_start_new_workday_does_not_request_text_confirmation(monkeypatch) -> None:
    app = QtWidgets.QApplication.instance() or QtWidgets.QApplication([])
    store = TaskClockStore()
    store.add_task("Build")
    window = TaskClockWindow(store)
    monkeypatch.setattr(
        QtWidgets.QMessageBox,
        "warning",
        lambda *args, **kwargs: QtWidgets.QMessageBox.StandardButton.Yes,
    )
    monkeypatch.setattr(
        QtWidgets.QInputDialog,
        "getText",
        lambda *args, **kwargs: (_ for _ in ()).throw(AssertionError("unexpected text confirmation")),
    )
    monkeypatch.setattr(store, "save", lambda: None)
    try:
        window.start_new_workday()
        assert window.status_label.text() == "Neuer Workday gestartet"
    finally:
        window.close()
        app.processEvents()


def test_window_uses_dark_theme_and_monospace_timer() -> None:
    app = QtWidgets.QApplication.instance() or QtWidgets.QApplication([])
    window = TaskClockWindow(TaskClockStore())
    try:
        stylesheet = window.styleSheet()
        assert "background: #0b1220" in stylesheet
        assert "font-family: 'JetBrains Mono', 'DejaVu Sans Mono', 'Liberation Mono', monospace" in stylesheet
        assert "QLabel#MainTimerLabel" in stylesheet
    finally:
        window.close()
        app.processEvents()
