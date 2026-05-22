from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from PySide6 import QtCore, QtGui, QtWidgets

from .core import APP_TITLE, APP_VERSION, Task, TaskClockStore, default_export_filename, fmt_seconds


@dataclass(frozen=True)
class Palette:
    bg: str = "#f5f7fb"
    surface: str = "#ffffff"
    card: str = "#eaf0f8"
    card_alt: str = "#dde7f4"
    text: str = "#162033"
    muted: str = "#52627a"
    primary: str = "#1463ff"
    primary_dark: str = "#0d49bd"
    danger: str = "#dc2626"
    border: str = "#c9d4e5"
    active_bg: str = "#dbeafe"
    active_fg: str = "#0f2a52"


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

    def refresh_rows(self) -> None:
        if not self.store.tasks:
            return
        top_left = self.index(0, 0)
        bottom_right = self.index(len(self.store.tasks) - 1, len(self.COLUMNS) - 1)
        self.dataChanged.emit(top_left, bottom_right, [])

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
                return QtGui.QBrush(QtGui.QColor("#0f2a52"))
            if column == 0:
                return QtGui.QBrush(QtGui.QColor("#52627a"))
            return QtGui.QBrush(QtGui.QColor("#162033"))

        if role == QtCore.Qt.ItemDataRole.BackgroundRole and task.is_active:
            return QtGui.QBrush(QtGui.QColor("#dbeafe"))

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

        self.pin_toggle = QtWidgets.QPushButton("Anpinnen: Aus")
        self.pin_toggle.setObjectName("PinToggle")
        self.pin_toggle.setCheckable(True)
        self.pin_toggle.toggled.connect(self.toggle_pin)
        header.addWidget(self.pin_toggle)
        outer.addLayout(header)

        self.hero_card = QtWidgets.QFrame()
        self.hero_card.setObjectName("HeroCard")
        hero_layout = QtWidgets.QGridLayout(self.hero_card)
        hero_layout.setContentsMargins(24, 20, 24, 20)
        hero_layout.setHorizontalSpacing(28)
        hero_layout.setVerticalSpacing(8)
        hero_tag = QtWidgets.QLabel("AKTIVE TASK")
        hero_tag.setObjectName("CardTag")
        total_tag = QtWidgets.QLabel("WORKDAY GESAMT")
        total_tag.setObjectName("CardTag")
        self.active_title = QtWidgets.QLabel("Keine aktive Task")
        self.active_title.setObjectName("ActiveTitle")
        self.active_time = QtWidgets.QLabel("00:00:00")
        self.active_time.setObjectName("TimerLabel")
        self.total_time = QtWidgets.QLabel("00:00:00")
        self.total_time.setObjectName("MainTimerLabel")
        hero_layout.addWidget(hero_tag, 0, 0)
        hero_layout.addWidget(total_tag, 0, 1)
        hero_layout.addWidget(self.active_title, 1, 0)
        hero_layout.addWidget(self.active_time, 2, 0)
        hero_layout.addWidget(self.total_time, 1, 1, 2, 1)
        hero_layout.setColumnStretch(0, 2)
        hero_layout.setColumnStretch(1, 1)
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
        self.workday_button = QtWidgets.QPushButton("Neuer Workday")
        self.workday_button.setObjectName("SecondaryButton")
        self.workday_button.clicked.connect(self.start_new_workday)
        self.delete_button = QtWidgets.QPushButton("Löschen")
        self.delete_button.setObjectName("DangerButton")
        self.delete_button.clicked.connect(self.delete_selected)
        for button in [
            self.add_button,
            self.stamp_button,
            self.stop_button,
            self.export_button,
            self.workday_button,
            self.delete_button,
        ]:
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
        self.table_view.verticalHeader().setDefaultSectionSize(48)
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
                font-family: 'Inter', 'Segoe UI', 'Arial';
                font-size: 12pt;
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
            QPushButton#PinToggle {{
                background: {self.palette.card};
                color: {self.palette.text};
                border: 1px solid {self.palette.border};
                border-radius: 16px;
                padding: 10px 18px;
                font-weight: 800;
            }}
            QPushButton#PinToggle:checked {{
                background: {self.palette.primary};
                color: #ffffff;
                border-color: {self.palette.primary_dark};
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
                font-family: 'Cascadia Mono', 'Consolas', monospace;
            }}
            QLabel#MainTimerLabel {{
                color: {self.palette.text};
                font-size: 34pt;
                font-weight: 900;
                font-family: 'Cascadia Mono', 'Consolas', monospace;
            }}
            QPushButton {{
                border: none;
                border-radius: 12px;
                padding: 12px 18px;
                font-weight: 700;
            }}
            QPushButton#PrimaryButton {{
                background: {self.palette.primary};
                color: #ffffff;
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
                color: #ffffff;
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
                font-size: 12pt;
            }}
            QHeaderView::section {{
                background: {self.palette.card};
                color: {self.palette.muted};
                padding: 14px 12px;
                border: none;
                font-weight: 700;
            }}
            QTableView::item {{
                padding: 14px 12px;
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
        self.refresh_ui(select_task_id=task.id, structure_changed=True)

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
        self.refresh_ui(select_task_id=task.id, structure_changed=True)

    def stop_active(self) -> None:
        active = self.store.stop_active()
        if active is None:
            self.status_label.setText("Keine aktive Task")
        else:
            self.status_label.setText(f"Gestoppt: {active.name}")
            self.store.save()
        self.refresh_ui(select_task_id=active.id if active else None, structure_changed=True)

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
        self.refresh_ui(structure_changed=True)

    def export_csv(self) -> None:
        path_text, _ = QtWidgets.QFileDialog.getSaveFileName(
            self,
            "CSV exportieren",
            str(Path.home() / default_export_filename(self.store.clock())),
            "CSV (*.csv)",
        )
        if not path_text:
            return
        path = Path(path_text)
        self.store.export_csv(path)
        self.status_label.setText(f"CSV exportiert: {path}")
        QtWidgets.QMessageBox.information(self, "TaskClock", "CSV exportiert.")

    def start_new_workday(self) -> None:
        if not self.store.tasks:
            QtWidgets.QMessageBox.information(self, "TaskClock", "Noch keine Tasks vorhanden.")
            return
        total = fmt_seconds(self.store.grand_total_seconds())
        if (
            QtWidgets.QMessageBox.warning(
                self,
                "Neuer Workday",
                "Das stoppt die aktive Task, archiviert den aktuellen Workday "
                f"({total}) und setzt alle Task-Zeiten auf 00:00:00 zurück.\n\nFortfahren?",
                QtWidgets.QMessageBox.StandardButton.Yes | QtWidgets.QMessageBox.StandardButton.No,
                QtWidgets.QMessageBox.StandardButton.No,
            )
            != QtWidgets.QMessageBox.StandardButton.Yes
        ):
            return
        confirmation, ok = QtWidgets.QInputDialog.getText(
            self,
            "Neuer Workday bestätigen",
            "Zum Starten exakt NEUER WORKDAY eingeben:",
        )
        if not ok or confirmation.strip() != "NEUER WORKDAY":
            self.status_label.setText("Neuer Workday abgebrochen")
            return
        archive = self.store.start_new_workday()
        self.store.save()
        self.status_label.setText(f"Neuer Workday gestartet. Archiviert: {fmt_seconds(int(archive['total_seconds']))}")
        self.refresh_ui(structure_changed=True)

    def toggle_pin(self, enabled: bool) -> None:
        self.pin_toggle.setText("Anpinnen: An" if enabled else "Anpinnen: Aus")
        self.setWindowFlag(QtCore.Qt.WindowType.WindowStaysOnTopHint, enabled)
        self.show()
        self.status_label.setText("Fenster angepinnt" if enabled else "Fenster nicht mehr angepinnt")

    def refresh_ui(self, select_task_id: str | None = None, structure_changed: bool = False) -> None:
        active = self.store.active_task()
        self.active_title.setText(active.name if active else "Keine aktive Task")
        self.active_time.setText(fmt_seconds(active.current_seconds(self.store.clock)) if active else "00:00:00")
        self.total_time.setText(fmt_seconds(self.store.grand_total_seconds()))

        current_id = select_task_id
        if current_id is None:
            selection = self.table_view.selectionModel().selectedRows()
            if selection and selection[0].row() < len(self.store.tasks):
                current_id = self.store.tasks[selection[0].row()].id

        if structure_changed:
            self.model.refresh()
        else:
            self.model.refresh_rows()

        if current_id is not None:
            index = self.model.index_for_task(current_id)
            if index.isValid():
                self.table_view.selectionModel().select(
                    index,
                    QtCore.QItemSelectionModel.SelectionFlag.ClearAndSelect | QtCore.QItemSelectionModel.SelectionFlag.Rows,
                )
                self.table_view.scrollTo(index)

    def closeEvent(self, event: QtGui.QCloseEvent) -> None:  # noqa: N802
        self.store.save()
        super().closeEvent(event)
