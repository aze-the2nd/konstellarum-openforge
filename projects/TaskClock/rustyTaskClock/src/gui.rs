use crate::core::{
    default_archive_export_filename, default_export_filename, default_log_file, ArchiveExportRange,
    ArchiveExportSummary, TaskClockStore, ThemeMode, APP_TITLE,
};
use chrono::{Datelike, NaiveDate, Utc};
use eframe::{egui, App, CreationContext, Frame};
use std::path::PathBuf;
use std::time::Duration;
use uuid::Uuid;

pub const DEFAULT_INNER_SIZE: [f32; 2] = [820.0, 560.0];
pub const COMPACT_MIN_INNER_SIZE: [f32; 2] = [360.0, 260.0];
const COMPACT_WIDTH_THRESHOLD: f32 = 520.0;
const COMPACT_HEIGHT_THRESHOLD: f32 = 420.0;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ExportRangeMode {
    Month,
    Week,
    DateRange,
}

#[derive(Debug, Clone)]
struct ExportManagerState {
    mode: ExportRangeMode,
    month_year: i32,
    month_month: u32,
    week_year: i32,
    week_week: u32,
    date_start: String,
    date_end: String,
}

impl ExportManagerState {
    fn new(now: chrono::DateTime<Utc>) -> Self {
        let today = now.date_naive();
        let iso_week = today.iso_week();
        Self {
            mode: ExportRangeMode::Month,
            month_year: today.year(),
            month_month: today.month(),
            week_year: iso_week.year(),
            week_week: iso_week.week(),
            date_start: today.to_string(),
            date_end: today.to_string(),
        }
    }

    fn selected_range(&self) -> Result<ArchiveExportRange, String> {
        match self.mode {
            ExportRangeMode::Month => Ok(ArchiveExportRange::Month {
                year: self.month_year,
                month: self.month_month,
            }),
            ExportRangeMode::Week => Ok(ArchiveExportRange::Week {
                year: self.week_year,
                week: self.week_week,
            }),
            ExportRangeMode::DateRange => {
                let start = NaiveDate::parse_from_str(self.date_start.trim(), "%Y-%m-%d")
                    .map_err(|_| String::from("Startdatum bitte als YYYY-MM-DD eingeben."))?;
                let end = NaiveDate::parse_from_str(self.date_end.trim(), "%Y-%m-%d")
                    .map_err(|_| String::from("Enddatum bitte als YYYY-MM-DD eingeben."))?;
                Ok(ArchiveExportRange::DateRange { start, end })
            }
        }
    }

    fn preview(
        &self,
        store: &TaskClockStore,
    ) -> Result<(ArchiveExportRange, ArchiveExportSummary), String> {
        let range = self.selected_range()?;
        let summary = store.archived_export_summary(&range);
        Ok((range, summary))
    }
}

pub fn use_compact_timer_layout(available_width: f32, available_height: f32) -> bool {
    available_width < COMPACT_WIDTH_THRESHOLD || available_height < COMPACT_HEIGHT_THRESHOLD
}

pub fn theme_toggle_symbol(theme_mode: ThemeMode) -> &'static str {
    match theme_mode {
        ThemeMode::Dark => "☀",
        ThemeMode::Light => "🌙",
    }
}

pub fn pin_toggle_label(pin_on_top: bool) -> &'static str {
    if pin_on_top {
        "📌 Angepinnt"
    } else {
        "📌 Anpinnen"
    }
}

pub fn archived_workdays_label(count: usize) -> String {
    format!("Archivierte Workdays: {count}")
}

pub fn run() {
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_title(APP_TITLE)
            .with_inner_size(DEFAULT_INNER_SIZE)
            .with_min_inner_size(COMPACT_MIN_INNER_SIZE),
        ..Default::default()
    };

    if let Err(err) = eframe::run_native(
        APP_TITLE,
        options,
        Box::new(|cc| Ok(Box::new(TaskClockApp::new(cc)))),
    ) {
        let _ = std::fs::write(default_log_file(), format!("{err:?}\n"));
        eprintln!("{err:?}");
    }
}

pub struct TaskClockApp {
    store: TaskClockStore,
    selected_task: Option<Uuid>,
    new_task_name: String,
    rename_buffer: String,
    editing_task: Option<Uuid>,
    status: String,
    pin_on_top: bool,
    export_path: PathBuf,
    export_manager: ExportManagerState,
}

impl TaskClockApp {
    pub fn new(cc: &CreationContext<'_>) -> Self {
        let (store, status) = match TaskClockStore::load() {
            Ok(store) => (store, String::from("Bereit")),
            Err(err) => (
                TaskClockStore::new_with_clock(Utc::now),
                format!("Laden fehlgeschlagen: {err}"),
            ),
        };
        cc.egui_ctx.set_visuals(visuals_for(store.theme_mode()));
        let export_manager = ExportManagerState::new(store.clock_now());
        let selected_task = store
            .active_task_id()
            .or_else(|| store.tasks().first().map(|task| task.id));
        let rename_buffer = selected_task
            .and_then(|id| store.task(id).map(|task| task.name.clone()))
            .unwrap_or_default();

        Self {
            store,
            selected_task,
            new_task_name: String::new(),
            rename_buffer,
            editing_task: None,
            status,
            pin_on_top: false,
            export_path: PathBuf::new(),
            export_manager,
        }
    }

    fn select_task(&mut self, id: Uuid) {
        self.selected_task = Some(id);
        if let Some(task) = self.store.task(id) {
            self.rename_buffer = task.name.clone();
        }
    }

    fn sync_selection(&mut self) {
        if let Some(editing_id) = self.editing_task {
            if self.store.task(editing_id).is_none() {
                self.editing_task = None;
                self.rename_buffer.clear();
            }
        }

        if let Some(id) = self.selected_task {
            if self.store.task(id).is_none() {
                self.selected_task = self.store.tasks().first().map(|task| task.id);
            }
        } else {
            self.selected_task = self.store.tasks().first().map(|task| task.id);
        }

        if self.editing_task.is_none() {
            if let Some(id) = self.selected_task {
                if let Some(task) = self.store.task(id) {
                    self.rename_buffer = task.name.clone();
                }
            } else {
                self.rename_buffer.clear();
            }
        }
    }

    fn add_task(&mut self) {
        let name = self.new_task_name.trim().to_string();
        if name.is_empty() {
            self.status = String::from("Bitte einen Task-Namen eingeben.");
            return;
        }
        let id = self.store.add_task(name.clone());
        self.new_task_name.clear();
        self.select_task(id);
        self.status = format!("Task '{name}' angelegt.");
        let _ = self.store.save();
    }

    fn stamp_selected(&mut self) {
        let Some(id) = self.selected_task else {
            self.status = String::from("Bitte zuerst einen Task auswählen.");
            return;
        };
        match self.store.stamp(id) {
            Ok(()) => {
                let active_name = self
                    .store
                    .task(id)
                    .map(|task| task.name.clone())
                    .unwrap_or_default();
                self.status = if self.store.active_task_id() == Some(id) {
                    format!("'{active_name}' gestartet.")
                } else {
                    format!("'{active_name}' gestoppt.")
                };
                let _ = self.store.save();
            }
            Err(err) => self.status = format!("Stempeln fehlgeschlagen: {err}"),
        }
    }

    fn stop_active(&mut self) {
        match self.store.stop_active() {
            Ok(Some(session)) => {
                self.status = format!(
                    "'{0}' gestoppt ({1}).",
                    session.task_name,
                    session.duration()
                );
                let _ = self.store.save();
            }
            Ok(None) => self.status = String::from("Keine aktive Task."),
            Err(err) => self.status = format!("Stopp fehlgeschlagen: {err}"),
        }
    }

    fn rename_task_from_buffer(&mut self, id: Uuid) {
        let name = self.rename_buffer.trim().to_string();
        if name.is_empty() {
            self.status = String::from("Der Name darf nicht leer sein.");
            return;
        }
        match self.store.rename_task(id, name.clone()) {
            Ok(()) => {
                self.selected_task = Some(id);
                self.editing_task = None;
                self.rename_buffer = name.clone();
                self.status = format!("Task '{}' umbenannt.", name);
                let _ = self.store.save();
            }
            Err(err) => self.status = format!("Umbenennen fehlgeschlagen: {err}"),
        }
    }

    fn delete_selected(&mut self) {
        let Some(id) = self.selected_task else {
            self.status = String::from("Keine Task ausgewählt.");
            return;
        };
        let name = self
            .store
            .task(id)
            .map(|task| task.name.clone())
            .unwrap_or_default();
        match self.store.delete_task(id) {
            Ok(()) => {
                self.sync_selection();
                self.status = format!("Task '{name}' gelöscht.");
                let _ = self.store.save();
            }
            Err(err) => self.status = format!("Löschen fehlgeschlagen: {err}"),
        }
    }

    fn start_new_workday(&mut self) {
        match self.store.start_new_workday() {
            Ok(archive) => {
                self.status = format!(
                    "Neuer Workday gestartet. Archiviert: {}. Gesamt: {}.",
                    archive.tasks.len(),
                    archive.total_seconds
                );
                let _ = self.store.save();
            }
            Err(err) => self.status = format!("Workday-Reset fehlgeschlagen: {err}"),
        }
    }

    fn export_csv(&mut self) {
        let filename = default_export_filename(Some(Utc::now()));
        let path = crate::core::app_data_dir().join("exports").join(filename);
        match self.store.export_csv(&path) {
            Ok(()) => {
                self.export_path = path.clone();
                self.status = format!("CSV exportiert: {}", path.display());
            }
            Err(err) => self.status = format!("Export fehlgeschlagen: {err}"),
        }
    }

    fn export_archived_range(&mut self) {
        let range = match self.export_manager.selected_range() {
            Ok(range) => range,
            Err(err) => {
                self.status = format!("Archiv-Export ungültig: {err}");
                return;
            }
        };
        let filename = default_archive_export_filename(&range);
        let path = crate::core::app_data_dir().join("exports").join(filename);
        match self.store.export_archived_csv(&path, &range) {
            Ok(summary) => {
                self.export_path = path.clone();
                self.status = format!(
                    "{} exportiert: {} Workdays, {} Zeilen.",
                    range.label(),
                    summary.archives,
                    summary.rows
                );
            }
            Err(err) => self.status = format!("Archiv-Export fehlgeschlagen: {err}"),
        }
    }
}

impl App for TaskClockApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut Frame) {
        ctx.request_repaint_after(Duration::from_secs(1));
        ctx.set_visuals(visuals_for(self.store.theme_mode()));
        self.sync_selection();

        let window_level = if self.pin_on_top {
            egui::WindowLevel::AlwaysOnTop
        } else {
            egui::WindowLevel::Normal
        };
        ctx.send_viewport_cmd(egui::ViewportCommand::WindowLevel(window_level));
        ctx.send_viewport_cmd(egui::ViewportCommand::Title(APP_TITLE.to_owned()));

        egui::TopBottomPanel::top("top_bar").show(ctx, |ui| {
            ui.horizontal_wrapped(|ui| {
                ui.vertical(|ui| {
                    ui.heading(APP_TITLE);
                    ui.label("Projektzeit stempeln. Klar, klein und standalone.");
                });
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    if ui
                        .button(theme_toggle_symbol(self.store.theme_mode()))
                        .on_hover_text(match self.store.theme_mode() {
                            ThemeMode::Dark => "Zum hellen Modus wechseln",
                            ThemeMode::Light => "Zum dunklen Modus wechseln",
                        })
                        .clicked()
                    {
                        let theme_mode = self.store.toggle_theme_mode();
                        self.status = format!("{}-Modus aktiviert.", theme_mode.label());
                        let _ = self.store.save();
                    }

                    if ui
                        .add(
                            egui::Button::new(pin_toggle_label(self.pin_on_top))
                                .selected(self.pin_on_top),
                        )
                        .clicked()
                    {
                        self.pin_on_top = !self.pin_on_top;
                    }
                });
            });
        });

        egui::CentralPanel::default().show(ctx, |ui| {
            ui.add_space(6.0);
            let active = self.store.active_task();
            let active_name = active
                .map(|task| task.name.as_str())
                .unwrap_or("Keine aktive Task");
            let active_time = active
                .map(|task| {
                    crate::core::fmt_seconds(self.store.current_seconds_for(task.id).unwrap_or(0))
                })
                .unwrap_or_else(|| String::from("00:00:00"));
            let workday_total = crate::core::fmt_seconds(self.store.current_workday_seconds());
            let compact_timers =
                use_compact_timer_layout(ui.available_width(), ui.available_height());

            egui::Frame::group(ui.style())
                .inner_margin(egui::Margin::same(if compact_timers { 12 } else { 18 }))
                .show(ui, |ui| {
                    let title_gap = if compact_timers { 2.0 } else { 4.0 };
                    let subtitle_gap = if compact_timers { 4.0 } else { 6.0 };
                    let name_size = if compact_timers { 20.0 } else { 22.0 };
                    let timer_size = if compact_timers { 30.0 } else { 36.0 };
                    let placeholder_size = if compact_timers { 20.0 } else { 22.0 };

                    let active_block = |ui: &mut egui::Ui| {
                        ui.label(egui::RichText::new("AKTIVE TASK").small().strong());
                        ui.add_space(title_gap);
                        ui.label(egui::RichText::new(active_name).size(name_size).strong());
                        ui.add_space(subtitle_gap);
                        ui.label(
                            egui::RichText::new(active_time.clone())
                                .size(timer_size)
                                .strong(),
                        );
                    };
                    let workday_block = |ui: &mut egui::Ui| {
                        ui.label(egui::RichText::new("WORKDAY GESAMT").small().strong());
                        ui.add_space(title_gap);
                        ui.label(egui::RichText::new(" ").size(placeholder_size));
                        ui.add_space(subtitle_gap);
                        ui.label(
                            egui::RichText::new(workday_total.clone())
                                .size(timer_size)
                                .strong(),
                        );
                    };

                    if compact_timers {
                        ui.vertical(|ui| {
                            active_block(ui);
                            ui.add_space(6.0);
                            workday_block(ui);
                            ui.add_space(4.0);
                            ui.horizontal_centered(|ui| {
                                ui.label(
                                    egui::RichText::new(archived_workdays_label(
                                        self.store.workdays().len(),
                                    ))
                                    .small(),
                                );
                            });
                        });
                    } else {
                        ui.horizontal(|ui| {
                            ui.vertical(active_block);
                            ui.separator();
                            ui.vertical(workday_block);
                        });
                        ui.add_space(4.0);
                        ui.horizontal_centered(|ui| {
                            ui.label(
                                egui::RichText::new(archived_workdays_label(
                                    self.store.workdays().len(),
                                ))
                                .small(),
                            );
                        });
                    }
                });

            egui::ScrollArea::vertical()
                .auto_shrink([false, false])
                .show(ui, |ui| {
                    ui.add_space(12.0);
                    ui.horizontal_wrapped(|ui| {
                        ui.label("Neue Task:");
                        let response = ui.add(
                            egui::TextEdit::singleline(&mut self.new_task_name)
                                .desired_width(220.0),
                        );
                        if response.lost_focus()
                            && ui.input(|input| input.key_pressed(egui::Key::Enter))
                        {
                            self.add_task();
                        }
                    });

                    ui.add_space(6.0);
                    ui.horizontal_wrapped(|ui| {
                        if ui.button("+ Anlegen").clicked() {
                            self.add_task();
                        }
                        if ui.button("▶ / ■ Stempeln").clicked() {
                            self.stamp_selected();
                        }
                        if ui.button("Aktive stoppen").clicked() {
                            self.stop_active();
                        }
                        if ui.button("Gesamt-CSV").clicked() {
                            self.export_csv();
                        }
                        if ui.button("Neuer Workday").clicked() {
                            self.start_new_workday();
                        }
                        if ui.button("Löschen").clicked() {
                            self.delete_selected();
                        }
                    });

                    ui.add_space(8.0);
                    egui::CollapsingHeader::new("Exportmanager")
                        .default_open(false)
                        .show(ui, |ui| {
                            ui.horizontal_wrapped(|ui| {
                                ui.label("Bereich:");
                                ui.selectable_value(
                                    &mut self.export_manager.mode,
                                    ExportRangeMode::Month,
                                    "Monat",
                                );
                                ui.selectable_value(
                                    &mut self.export_manager.mode,
                                    ExportRangeMode::Week,
                                    "Woche",
                                );
                                ui.selectable_value(
                                    &mut self.export_manager.mode,
                                    ExportRangeMode::DateRange,
                                    "Date-to-date",
                                );
                            });

                            ui.add_space(4.0);
                            match self.export_manager.mode {
                                ExportRangeMode::Month => {
                                    ui.horizontal_wrapped(|ui| {
                                        ui.label("Jahr");
                                        ui.add(
                                            egui::DragValue::new(
                                                &mut self.export_manager.month_year,
                                            )
                                            .speed(1),
                                        );
                                        ui.label("Monat");
                                        ui.add(
                                            egui::DragValue::new(
                                                &mut self.export_manager.month_month,
                                            )
                                            .range(1..=12)
                                            .speed(1),
                                        );
                                    });
                                }
                                ExportRangeMode::Week => {
                                    ui.horizontal_wrapped(|ui| {
                                        ui.label("ISO-Jahr");
                                        ui.add(
                                            egui::DragValue::new(
                                                &mut self.export_manager.week_year,
                                            )
                                            .speed(1),
                                        );
                                        ui.label("ISO-Woche");
                                        ui.add(
                                            egui::DragValue::new(
                                                &mut self.export_manager.week_week,
                                            )
                                            .range(1..=53)
                                            .speed(1),
                                        );
                                    });
                                }
                                ExportRangeMode::DateRange => {
                                    ui.horizontal_wrapped(|ui| {
                                        ui.label("Von");
                                        ui.add(
                                            egui::TextEdit::singleline(
                                                &mut self.export_manager.date_start,
                                            )
                                            .desired_width(110.0),
                                        )
                                        .on_hover_text("Format: YYYY-MM-DD");
                                        ui.label("Bis");
                                        ui.add(
                                            egui::TextEdit::singleline(
                                                &mut self.export_manager.date_end,
                                            )
                                            .desired_width(110.0),
                                        )
                                        .on_hover_text("Format: YYYY-MM-DD");
                                    });
                                }
                            }

                            ui.add_space(4.0);
                            let preview = self.export_manager.preview(&self.store);
                            ui.horizontal_wrapped(|ui| {
                                if ui.button("Archiv-CSV exportieren").clicked() {
                                    self.export_archived_range();
                                }
                                match &preview {
                                    Ok((range, summary)) => {
                                        ui.label(format!(
                                            "{} • {} Workdays • {} Zeilen",
                                            range.label(),
                                            summary.archives,
                                            summary.rows
                                        ));
                                    }
                                    Err(err) => {
                                        ui.colored_label(ui.visuals().error_fg_color, err);
                                    }
                                }
                            });

                            if let Ok((range, _)) = &preview {
                                ui.label(format!(
                                    "Dateiname: {}",
                                    default_archive_export_filename(range)
                                ));
                            }
                        });

                    ui.add_space(12.0);
                    egui::Grid::new("task_grid")
                        .striped(true)
                        .spacing([12.0, 8.0])
                        .show(ui, |ui| {
                            ui.strong("");
                            ui.strong("Task");
                            ui.strong("Zeit");
                            ui.strong("Aktion");
                            ui.end_row();

                            for task in self.store.tasks().to_vec() {
                                let is_selected = self.selected_task == Some(task.id);
                                let is_active = task.is_active();
                                let marker = if is_active { "●" } else { "○" };
                                if ui.selectable_label(is_selected, marker).clicked() {
                                    self.select_task(task.id);
                                }
                                if self.editing_task == Some(task.id) {
                                    let response = ui.add(
                                        egui::TextEdit::singleline(&mut self.rename_buffer)
                                            .desired_width(if compact_timers {
                                                140.0
                                            } else {
                                                220.0
                                            }),
                                    );
                                    let enter_pressed =
                                        ui.input(|input| input.key_pressed(egui::Key::Enter));
                                    if response.lost_focus() || enter_pressed {
                                        self.rename_task_from_buffer(task.id);
                                    }
                                } else if ui
                                    .selectable_label(is_selected, task.name.clone())
                                    .on_hover_text("Klicken zum Umbenennen")
                                    .clicked()
                                {
                                    self.select_task(task.id);
                                    self.editing_task = Some(task.id);
                                    self.rename_buffer = task.name.clone();
                                }
                                ui.label(crate::core::fmt_seconds(
                                    self.store
                                        .current_seconds_for(task.id)
                                        .unwrap_or(task.total_seconds),
                                ));
                                let action_label = if is_active { "Stop" } else { "Start" };
                                if ui.button(action_label).clicked() {
                                    self.select_task(task.id);
                                    self.stamp_selected();
                                }
                                ui.end_row();
                            }
                        });

                    ui.add_space(8.0);
                    ui.label(&self.status);
                    if !self.export_path.as_os_str().is_empty() {
                        ui.label(format!("Letzter Export: {}", self.export_path.display()));
                    }
                });
        });
    }
}

fn visuals_for(theme_mode: ThemeMode) -> egui::Visuals {
    match theme_mode {
        ThemeMode::Dark => egui::Visuals::dark(),
        ThemeMode::Light => egui::Visuals::light(),
    }
}

trait DurationLabel {
    fn duration(&self) -> String;
}

impl DurationLabel for crate::core::SessionRecord {
    fn duration(&self) -> String {
        crate::core::fmt_seconds(self.seconds)
    }
}
