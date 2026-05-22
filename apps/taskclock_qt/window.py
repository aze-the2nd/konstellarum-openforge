from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from PySide6 import QtCore, QtGui, QtWidgets

from .core import APP_TITLE, APP_VERSION, Task, TaskClockStore, fmt_seconds


@dataclass(frozen=True)
class Palette:
    bg: str = "#0b1220"
    surface: str = "#101827"
    card: str = "#172033"
    card_alt: str = "#1f2a40"
    text: str = "#e5eefc"
    muted: str = "#91a4c7"
    primary: str = "#4cc9f0"
    primary_dark: str = "#1d8fb8"
    danger: str = "#f87171"
    border: str = "#24334d"
    active_bg: str = "#11324d"
    active_fg: str = "#d6f4ff"


class TaskTableModel(QtCore.QAbstractTableModel):
    COLUMNS = ("", "Task", "Zeit")

    def __init__(self, store: TaskClockStore) -> None:
        super().__init__()
        self.store = store

    def rowCount(self, parent: QtCore.QModelIndex = QtCore.QModelIndex()) -> int:  # noqa: N802
        return 0 if parent.isValid() else len(self.store.tasks)

    def columnCount(self, parent: QtCore.QModelIndex = QtCore.QModelIndex()) -> int:  # noqa: N802
        return 0 if parent.isValid() else len(self.COLUMNS)

    def headerData(self, section: int, orientation: QtCore.Qt.Orientation, role: int = QtCore.Qt.ItemDataRole.DisplayRole):  # noqa: N802
        if role != QtCore.Qt.ItemDataRole.DisplayRole:
            return None
        if orientation == QtCore.Qt.Orientation.Horizontal:
            return self.COLUMNS[section]
        return str(section + 1)

    def task_at(self, row: int) -> Task:
        return self.store.tasks[row]

    def index_for_task(self, task_id: str) -> QtCore.QModelIndex:
        for row, task in enumerate(self.store.tasks):
            if task.id == task_id:
                return self.index(row, 0)
        return QtCore.QModelIndex()

    def refresh(self) -> None:
        self.beginResetModel()
        self.endResetModel()

    def data(self, index: QtCore.QModelIndex, role: int = QtCore.Qt.ItemDataRole.DisplayRole):  # noqa: N802
        if not index.isValid():
            return None
        task = self.store.tasks[index.row()]
        column = index.column()

        if role == QtCore.Qt.ItemDataRole.DisplayRole:
            if column == 0:
                return "●" if task.is_active else "○"
            if column == 1:
                return task.name
            if column == 2:
                return fmt_seconds(task.current_seconds(self.store.clock))

        if role == QtCore.Qt.ItemDataRole.TextAlignmentRole:
            if column == 0:
                return QtCore.Qt.AlignmentFlag.AlignCenter
            if column == 2:
                return QtCore.Qt.AlignmentFlag.AlignRight | QtCore.Qt.AlignmentFlag.AlignVCenter
            return QtCore.Qt.AlignmentFlag.AlignVCenter | QtCore.Qt.AlignmentFlag.AlignLeft

        if role == QtCore.Qt.ItemDataRole.FontRole:
            font = QtGui.QFont("Segoe UI", 11)
            if column == 1 and task.is_active:
                font.setBold(True)
            return font

        if role == QtCore.Qt.ItemDataRole.ForegroundRole:
            if task.is_active:
                return QtGui.QBrush(QtGui.QColor("#dff8ff"))
            if column == 0:
                return QtGui.QBrush(QtGui.QColor("#8ca0bf"))
            return QtGui.QBrush(QtGui.QColor("#e5eefc"))

        if role == QtCore.Qt.ItemDataRole.BackgroundRole and task.is_active:
            return QtGui.QBrush(QtGui.QColor("#123854"))

        return None


class TaskClockWindow(QtWidgets.QMainWindow):
    def __init__(self, store: TaskClockStore | None = None) -> None:
        super().__init__()
        self.store = store or TaskClockStore.load()
        self.palette = Palette()
        self.setWindowTitle(APP_TITLE)
        self.resize(860, 560)
        self.setMinimumSize(760, 500)

        self.model = TaskTableModel(self.store)
        self._build_ui()
        self._apply_theme()
        self._wire_timer()
        self.refresh_ui()

    def _build_ui(self) -> None:
        root = QtWidgets.QWidget(self)
        self.setCentralWidget(root)
        outer = QtWidgets.QVBoxLayout(root)
        outer.setContentsMargins(24, 24, 24, 18)
        outer.setSpacing(16)

        header = QtWidgets.QHBoxLayout()
        title_col = QtWidgets.QVBoxLayout()
        title = QtWidgets.QLabel(APP_TITLE)
        title.setObjectName("TitleLabel")
        subtitle = QtWidgets.QLabel("Projektzeit stempeln. Klar, direkt, modern.")
        subtitle.setObjectName("SubtitleLabel")
        title_col.addWidget(title)
        title_col.addWidget(subtitle)
        header.addLayout(title_col)
        header.addStretch(1)

        self.pin_checkbox = QtWidgets.QCheckBox("Fenster anpinnen")
        self.pin_checkbox.toggled.connect(self.toggle_pin)
        header.addWidget(self.pin_checkbox)
        outer.addLayout(header)

        self.hero_card = QtWidgets.QFrame()
        self.hero_card.setObjectName("HeroCard")
        hero_layout = QtWidgets.QVBoxLayout(self.hero_card)
        hero_layout.setContentsMargins(22, 20, 22, 20)
        hero_layout.setSpacing(8)
        hero_tag = QtWidgets.QLabel("AKTIVE TASK")
        hero_tag.setObjectName("CardTag")
        self.active_title = QtWidgets.QLabel("Keine aktive Task")
        self.active_title.setObjectName("ActiveTitle")
        self.active_time = QtWidgets.QLabel("00:00:00")
        self.active_time.setObjectName("TimerLabel")
        hero_layout.addWidget(hero_tag)
        hero_layout.addWidget(self.active_title)
        hero_layout.addWidget(self.active_time)
        outer.addWidget(self.hero_card)

        actions = QtWidgets.QHBoxLayout()
        actions.setSpacing(10)
        self.add_button = QtWidgets.QPushButton("+ Neue Task")
        self.add_button.setObjectName("PrimaryButton")
        self.add_button.clicked.connect(self.add_task)
        self.stamp_button = QtWidgets.QPushButton("▶ / ■ Stempeln")
        self.stamp_button.setObjectName("SecondaryButton")
        self.stamp_button.clicked.connect(self.stamp_selected)
        self.stop_button = QtWidgets.QPushButton("Aktive stoppen")
        self.stop_button.setObjectName("SecondaryButton")
        self.stop_button.clicked.connect(self.stop_active)
        self.export_button = QtWidgets.QPushButton("CSV exportieren")
        self.export_button.setObjectName("SecondaryButton")
        self.export_button.clicked.connect(self.export_csv)
        self.delete_button = QtWidgets.QPushButton("Löschen")
        self.delete_button.setObjectName("DangerButton")
        self.delete_button.clicked.connect(self.delete_selected)
        for button in [self.add_button, self.stamp_button, self.stop_button, self.export_button, self.delete_button]:
            actions.addWidget(button)
        actions.addStretch(1)
        outer.addLayout(actions)

        table_card = QtWidgets.QFrame()
        table_card.setObjectName("TableCard")
        table_layout = QtWidgets.QVBoxLayout(table_card)
        table_layout.setContentsMargins(1, 1, 1, 1)
        table_layout.setSpacing(0)

        self.table_view = QtWidgets.QTableView()
        self.table_view.setObjectName("TaskTable")
        self.table_view.setModel(self.model)
        self.table_view.setSelectionBehavior(QtWidgets.QAbstractItemView.SelectionBehavior.SelectRows)
        self.table_view.setSelectionMode(QtWidgets.QAbstractItemView.SelectionMode.SingleSelection)
        self.table_view.setEditTriggers(QtWidgets.QAbstractItemView.EditTrigger.NoEditTriggers)
        self.table_view.setAlternatingRowColors(False)
        self.table_view.verticalHeader().setVisible(False)
        self.table_view.horizontalHeader().setStretchLastSection(True)
        self.table_view.horizontalHeader().setSectionResizeMode(0, QtWidgets.QHeaderView.ResizeMode.Fixed)
        self.table_view.horizontalHeader().setSectionResizeMode(1, QtWidgets.QHeaderView.ResizeMode.Stretch)
        self.table_view.horizontalHeader().setSectionResizeMode(2, QtWidgets.QHeaderView.ResizeMode.ResizeToContents)
        self.table_view.horizontalHeader().setDefaultAlignment(QtCore.Qt.AlignmentFlag.AlignLeft)
        self.table_view.setColumnWidth(0, 54)
        self.table_view.doubleClicked.connect(lambda _index: self.stamp_selected())
        table_layout.addWidget(self.table_view)
        outer.addWidget(table_card, 1)

        self.status_label = QtWidgets.QLabel("Bereit")
        self.status_label.setObjectName("StatusLabel")
        outer.addWidget(self.status_label)

    def _apply_theme(self) -> None:
        self.setStyleSheet(
            f"""
            QWidget {{
                background: {self.palette.bg};
                color: {self.palette.text};
                font-family: 'Segoe UI';
                font-size: 11pt;
            }}
            QLabel#TitleLabel {{
                font-size: 22pt;
                font-weight: 700;
                color: {self.palette.text};
            }}
            QLabel#SubtitleLabel, QLabel#StatusLabel {{
                color: {self.palette.muted};
                font-size: 10pt;
            }}
            QCheckBox {{
                color: {self.palette.text};
                spacing: 8px;
            }}
            QFrame#HeroCard, QFrame#TableCard {{
                background: {self.palette.surface};
                border: 1px solid {self.palette.border};
                border-radius: 16px;
            }}
            QLabel#CardTag {{
                color: {self.palette.muted};
                font-size: 9pt;
                font-weight: 700;
                letter-spacing: 0.08em;
            }}
            QLabel#ActiveTitle {{
                color: {self.palette.text};
                font-size: 15pt;
                font-weight: 700;
            }}
            QLabel#TimerLabel {{
                color: {self.palette.primary};
                font-size: 30pt;
                font-weight: 800;
                font-family: 'Consolas';
            }}
            QPushButton {{
                border: none;
                border-radius: 12px;
                padding: 12px 18px;
                font-weight: 700;
            }}
            QPushButton#PrimaryButton {{
                background: {self.palette.primary};
                color: #06111f;
            }}
            QPushButton#PrimaryButton:hover {{
                background: {self.palette.primary_dark};
            }}
            QPushButton#SecondaryButton {{
                background: {self.palette.card};
                color: {self.palette.text};
            }}
            QPushButton#SecondaryButton:hover {{
                background: {self.palette.card_alt};
            }}
            QPushButton#DangerButton {{
                background: {self.palette.danger};
                color: #250a0a;
            }}
            QPushButton#DangerButton:hover {{
                background: #ef4444;
            }}
            QTableView#TaskTable {{
                background: transparent;
                alternate-background-color: transparent;
                border: none;
                gridline-color: {self.palette.border};
                selection-background-color: {self.palette.primary_dark};
                selection-color: white;
            }}
            QHeaderView::section {{
                background: {self.palette.card};
                color: {self.palette.muted};
                padding: 10px 12px;
                border: none;
                font-weight: 700;
            }}
            QTableView::item {{
                padding: 10px 12px;
            }}
            """
        )

    def _wire_timer(self) -> None:
        self.timer = QtCore.QTimer(self)
        self.timer.setInterval(1000)
        self.timer.timeout.connect(self.refresh_ui)
        self.timer.start()

    def selected_task(self) -> Task | None:
        selection = self.table_view.selectionModel().selectedRows()
        if not selection:
            QtWidgets.QMessageBox.information(self, "TaskClock", "Bitte eine Task auswählen.")
            return None
        return self.store.tasks[selection[0].row()]

    def add_task(self) -> None:
        name, ok = QtWidgets.QInputDialog.getText(self, "Neue Task", "Name der Task:")
        if not ok or not name.strip():
            return
        task = self.store.add_task(name)
        self.store.save()
        self.status_label.setText(f"Task angelegt: {task.name}")
        self.refresh_ui(select_task_id=task.id)

    def stamp_selected(self) -> None:
        task = self.selected_task()
        if task is None:
            return
        active = self.store.active_task()
        self.store.stamp(task.id)
        self.store.save()
        if active is task:
            self.status_label.setText(f"Gestoppt: {task.name}")
        else:
            self.status_label.setText(f"Aktiv: {task.name}")
        self.refresh_ui(select_task_id=task.id)

    def stop_active(self) -> None:
        active = self.store.stop_active()
        if active is None:
            self.status_label.setText("Keine aktive Task")
        else:
            self.status_label.setText(f"Gestoppt: {active.name}")
            self.store.save()
        self.refresh_ui(select_task_id=active.id if active else None)

    def delete_selected(self) -> None:
        task = self.selected_task()
        if task is None:
            return
        if task.is_active:
            QtWidgets.QMessageBox.warning(self, "TaskClock", "Aktive Task zuerst stoppen.")
            return
        if QtWidgets.QMessageBox.question(self, "Task löschen", f"„{task.name}“ löschen?") != QtWidgets.QMessageBox.StandardButton.Yes:
            return
        self.store.delete_task(task.id)
        self.store.save()
        self.status_label.setText(f"Gelöscht: {task.name}")
        self.refresh_ui()

    def export_csv(self) -> None:
        path_text, _ = QtWidgets.QFileDialog.getSaveFileName(
            self,
            "CSV exportieren",
            str(Path.home() / "taskclock-export.csv"),
            "CSV (*.csv)",
        )
        if not path_text:
            return
        path = Path(path_text)
        self.store.export_csv(path)
        self.status_label.setText(f"CSV exportiert: {path}")
        QtWidgets.QMessageBox.information(self, "TaskClock", "CSV exportiert.")

    def toggle_pin(self, enabled: bool) -> None:
        self.setWindowFlag(QtCore.Qt.WindowType.WindowStaysOnTopHint, enabled)
        self.show()
        self.status_label.setText("Fenster angepinnt" if enabled else "Fenster nicht mehr angepinnt")

    def refresh_ui(self, select_task_id: str | None = None) -> None:
        active = self.store.active_task()
        self.active_title.setText(active.name if active else "Keine aktive Task")
        self.active_time.setText(fmt_seconds(active.current_seconds(self.store.clock)) if active else "00:00:00")

        current_id = select_task_id
        if current_id is None:
            selection = self.table_view.selectionModel().selectedRows()
            if selection:
                current_id = self.store.tasks[selection[0].row()].id

        self.model.refresh()

        if current_id is not None:
            index = self.model.index_for_task(current_id)
            if index.isValid():
                self.table_view.selectionModel().select(
                    index,
                    QtCore.QItemSelectionModel.SelectionFlag.Select | QtCore.QItemSelectionModel.SelectionFlag.Rows,
                )
                self.table_view.scrollTo(index)

    def closeEvent(self, event: QtGui.QCloseEvent) -> None:  # noqa: N802
        self.store.save()
        super().closeEvent(event)
