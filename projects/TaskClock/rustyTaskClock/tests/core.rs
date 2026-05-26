use chrono::{DateTime, TimeZone, Utc};
use rusty_taskclock::{
    default_export_filename, fmt_seconds,
    gui::{
        pin_toggle_label, theme_toggle_symbol, use_compact_timer_layout, COMPACT_MIN_INNER_SIZE,
    },
    TaskClockStore, ThemeMode,
};
use std::sync::{Arc, Mutex};
use tempfile::tempdir;

fn utc(y: i32, m: u32, d: u32, h: u32, min: u32, s: u32) -> DateTime<Utc> {
    Utc.with_ymd_and_hms(y, m, d, h, min, s).unwrap()
}

fn manual_clock(
    initial: DateTime<Utc>,
) -> (
    Arc<Mutex<DateTime<Utc>>>,
    impl Fn() -> DateTime<Utc> + Send + Sync + 'static,
) {
    let now = Arc::new(Mutex::new(initial));
    let clock_now = Arc::clone(&now);
    let clock = move || *clock_now.lock().unwrap();
    (now, clock)
}

#[test]
fn format_duration_pads_to_hh_mm_ss() {
    assert_eq!(fmt_seconds(0), "00:00:00");
    assert_eq!(fmt_seconds(3661), "01:01:01");
}

#[test]
fn gui_uses_small_minimum_window_and_compact_timer_threshold() {
    assert_eq!(COMPACT_MIN_INNER_SIZE, [360.0, 260.0]);
    assert!(use_compact_timer_layout(420.0, 500.0));
    assert!(use_compact_timer_layout(760.0, 360.0));
    assert!(!use_compact_timer_layout(760.0, 520.0));
}

#[test]
fn gui_control_labels_match_polished_toggle_direction() {
    assert_eq!(theme_toggle_symbol(ThemeMode::Dark), "☀");
    assert_eq!(theme_toggle_symbol(ThemeMode::Light), "🌙");
    assert_eq!(pin_toggle_label(false), "📌 Anpinnen");
    assert_eq!(pin_toggle_label(true), "📌 Angepinnt");
}

#[test]
fn default_export_filename_is_version_safe_and_deterministic() {
    assert_eq!(
        default_export_filename(Some(utc(2026, 5, 26, 13, 7, 0))),
        "taskclock_2026-05-26_1307.csv"
    );
}

#[test]
fn stamp_enforces_single_active_task() {
    let (now, clock) = manual_clock(utc(2026, 5, 26, 10, 0, 0));
    let mut store = TaskClockStore::new_with_clock(clock);
    let a = store.add_task("Build");
    let b = store.add_task("Review");

    store.stamp(a).unwrap();
    assert_eq!(store.active_task_id(), Some(a));

    *now.lock().unwrap() = utc(2026, 5, 26, 10, 15, 0);
    store.stamp(b).unwrap();
    assert_eq!(store.active_task_id(), Some(b));
    assert_eq!(store.task(a).unwrap().total_seconds(), 900);
    assert_eq!(store.task(b).unwrap().total_seconds(), 0);
}

#[test]
fn start_new_workday_archives_and_resets_current_state() {
    let (now, clock) = manual_clock(utc(2026, 5, 26, 10, 0, 0));
    let mut store = TaskClockStore::new_with_clock(clock);
    let task = store.add_task("Build");
    store.stamp(task).unwrap();

    *now.lock().unwrap() = utc(2026, 5, 26, 11, 0, 0);
    let archive = store.start_new_workday().unwrap();

    assert_eq!(archive.total_seconds, 3600);
    assert_eq!(store.workdays().len(), 1);
    assert_eq!(store.task(task).unwrap().total_seconds(), 0);
    assert!(store.task(task).unwrap().sessions().is_empty());
    assert_eq!(store.active_task_id(), None);
}

#[test]
fn save_and_load_round_trip_persists_tasks_and_workdays() {
    let (now, clock) = manual_clock(utc(2026, 5, 26, 11, 0, 0));
    let mut store = TaskClockStore::new_with_clock(clock);
    let task = store.add_task("Build");
    store.stamp(task).unwrap();
    store.set_theme_mode(ThemeMode::Light);

    *now.lock().unwrap() = utc(2026, 5, 26, 11, 30, 0);
    store.stop_active().unwrap();
    *now.lock().unwrap() = utc(2026, 5, 26, 12, 0, 0);
    let archive = store.start_new_workday().unwrap();
    assert_eq!(archive.total_seconds, 1800);

    let dir = tempdir().unwrap();
    let path = dir.path().join("taskclock_data.json");
    store.save_to_path(&path).unwrap();

    let loaded = TaskClockStore::load_from_path(&path, || utc(2026, 5, 26, 12, 0, 0)).unwrap();
    assert_eq!(loaded.tasks().len(), 1);
    assert_eq!(loaded.workdays().len(), 1);
    assert_eq!(loaded.theme_mode(), ThemeMode::Light);
    assert_eq!(loaded.task(task).unwrap().name(), "Build");
}

#[test]
fn load_tolerates_missing_fields_and_skips_invalid_task_entries() {
    let dir = tempdir().unwrap();
    let path = dir.path().join("taskclock_data.json");
    std::fs::write(
        &path,
        r#"{
            "tasks": [
                {"id": "good", "name": "Build", "total_seconds": 30},
                "broken"
            ],
            "workdays": [{"id": "archive-1", "total_seconds": 30}]
        }"#,
    )
    .unwrap();

    let store = TaskClockStore::load_from_path(&path, || utc(2026, 5, 26, 12, 0, 0)).unwrap();
    assert_eq!(store.tasks().len(), 1);
    assert_eq!(store.task("good").unwrap().name(), "Build");
    assert_eq!(store.workdays().len(), 1);
}

#[test]
fn export_csv_includes_current_and_archived_sessions() {
    let (now, clock) = manual_clock(utc(2026, 5, 26, 13, 0, 0));
    let mut store = TaskClockStore::new_with_clock(clock);
    let task = store.add_task("Build");
    store.stamp(task).unwrap();

    *now.lock().unwrap() = utc(2026, 5, 26, 13, 30, 0);
    store.stop_active().unwrap();
    *now.lock().unwrap() = utc(2026, 5, 26, 14, 0, 0);
    store.start_new_workday().unwrap();

    let dir = tempdir().unwrap();
    let path = dir.path().join("export.csv");
    store.export_csv(&path).unwrap();

    let csv = std::fs::read_to_string(&path).unwrap();
    assert!(
        csv.contains("workday_id,workday_ended_at,task_id,task_name,start,end,seconds,duration")
    );
    assert!(csv.contains("Build"));
}
