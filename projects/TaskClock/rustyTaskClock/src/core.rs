use chrono::{DateTime, Utc};
use csv::Writer;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::env;
use std::fs;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use thiserror::Error;
use uuid::Uuid;

pub const APP_NAME: &str = "rustyTaskClock";
pub const APP_VERSION: &str = env!("CARGO_PKG_VERSION");
pub const APP_TITLE: &str = concat!("rustyTaskClock v", env!("CARGO_PKG_VERSION"));

pub type Clock = Arc<dyn Fn() -> DateTime<Utc> + Send + Sync>;

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum ThemeMode {
    Dark,
    Light,
}

impl Default for ThemeMode {
    fn default() -> Self {
        Self::Dark
    }
}

impl ThemeMode {
    pub fn toggle(self) -> Self {
        match self {
            Self::Dark => Self::Light,
            Self::Light => Self::Dark,
        }
    }

    pub fn label(self) -> &'static str {
        match self {
            Self::Dark => "Dunkel",
            Self::Light => "Hell",
        }
    }
}

pub trait TaskIdLike {
    fn resolve(&self) -> Uuid;
}

pub fn task_uuid_from_text(text: &str) -> Uuid {
    Uuid::parse_str(text).unwrap_or_else(|_| Uuid::new_v5(&Uuid::NAMESPACE_OID, text.as_bytes()))
}

impl TaskIdLike for Uuid {
    fn resolve(&self) -> Uuid {
        *self
    }
}

impl TaskIdLike for &Uuid {
    fn resolve(&self) -> Uuid {
        **self
    }
}

impl TaskIdLike for String {
    fn resolve(&self) -> Uuid {
        task_uuid_from_text(self)
    }
}

impl TaskIdLike for &String {
    fn resolve(&self) -> Uuid {
        task_uuid_from_text(self)
    }
}

impl TaskIdLike for &str {
    fn resolve(&self) -> Uuid {
        task_uuid_from_text(self)
    }
}

pub fn now_utc() -> DateTime<Utc> {
    Utc::now()
}

pub fn fmt_seconds(seconds: i64) -> String {
    let seconds = seconds.max(0);
    let hours = seconds / 3600;
    let minutes = (seconds % 3600) / 60;
    let secs = seconds % 60;
    format!("{hours:02}:{minutes:02}:{secs:02}")
}

pub fn default_export_filename(moment: Option<DateTime<Utc>>) -> String {
    let stamp = moment.unwrap_or_else(now_utc).format("%Y-%m-%d_%H%M");
    format!("taskclock_{stamp}.csv")
}

pub fn app_data_dir() -> PathBuf {
    let base = if cfg!(target_os = "windows") {
        env::var_os("APPDATA")
            .map(PathBuf::from)
            .unwrap_or_else(|| dirs_fallback().join("AppData").join("Roaming"))
    } else if cfg!(target_os = "macos") {
        dirs_fallback().join("Library").join("Application Support")
    } else {
        env::var_os("XDG_DATA_HOME")
            .map(PathBuf::from)
            .unwrap_or_else(|| dirs_fallback().join(".local").join("share"))
    };

    let path = base.join(APP_NAME);
    let _ = fs::create_dir_all(&path);
    path
}

pub fn default_data_file() -> PathBuf {
    app_data_dir().join("taskclock_data.json")
}

pub fn default_log_file() -> PathBuf {
    app_data_dir().join("taskclock_error.log")
}

fn dirs_fallback() -> PathBuf {
    env::var_os("HOME")
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("."))
}

#[derive(Debug, Error)]
pub enum StoreError {
    #[error("failed to read {path}: {source}")]
    Read {
        path: PathBuf,
        source: std::io::Error,
    },

    #[error("failed to write {path}: {source}")]
    Write {
        path: PathBuf,
        source: std::io::Error,
    },

    #[error("invalid JSON in {path}: {source}")]
    Parse {
        path: PathBuf,
        source: serde_json::Error,
    },

    #[error("failed to write CSV to {path}: {source}")]
    Csv { path: PathBuf, source: csv::Error },

    #[error("task {0} not found")]
    TaskNotFound(Uuid),

    #[error("active task must be stopped before deletion")]
    ActiveTaskDelete,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct SessionRecord {
    pub task_id: Uuid,
    pub task_name: String,
    pub start: DateTime<Utc>,
    pub end: DateTime<Utc>,
    pub seconds: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct Task {
    pub id: Uuid,
    pub name: String,
    pub total_seconds: i64,
    pub active_since: Option<DateTime<Utc>>,
    #[serde(default)]
    pub sessions: Vec<SessionRecord>,
}

impl Task {
    pub fn new(name: impl Into<String>) -> Self {
        Self {
            id: Uuid::new_v4(),
            name: sanitize_name(name.into()),
            total_seconds: 0,
            active_since: None,
            sessions: Vec::new(),
        }
    }

    pub fn id(&self) -> Uuid {
        self.id
    }

    pub fn name(&self) -> &str {
        &self.name
    }

    pub fn total_seconds(&self) -> i64 {
        self.total_seconds
    }

    pub fn sessions(&self) -> &[SessionRecord] {
        &self.sessions
    }

    pub fn is_active(&self) -> bool {
        self.active_since.is_some()
    }

    pub fn current_seconds(&self, clock: &Clock) -> i64 {
        match self.active_since {
            Some(start) => self.total_seconds + ((*clock)() - start).num_seconds().max(0),
            None => self.total_seconds,
        }
    }

    pub fn start(&mut self, clock: &Clock) {
        if self.active_since.is_none() {
            self.active_since = Some((*clock)());
        }
    }

    pub fn stop(&mut self, clock: &Clock) -> Option<SessionRecord> {
        let start = self.active_since?;
        let end = (*clock)();
        let seconds = (end - start).num_seconds().max(0);
        self.total_seconds += seconds;
        let session = SessionRecord {
            task_id: self.id,
            task_name: self.name.clone(),
            start,
            end,
            seconds,
        };
        self.sessions.push(session.clone());
        self.active_since = None;
        Some(session)
    }

    pub fn rename(&mut self, name: impl Into<String>) {
        self.name = sanitize_name(name.into());
        for session in &mut self.sessions {
            session.task_name = self.name.clone();
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct WorkdayArchive {
    pub id: Uuid,
    pub ended_at: DateTime<Utc>,
    pub total_seconds: i64,
    pub tasks: Vec<Task>,
}

impl WorkdayArchive {
    pub fn session_rows(&self) -> Vec<SessionRow> {
        self.tasks
            .iter()
            .flat_map(|task| task.sessions.iter().cloned())
            .map(|session| SessionRow {
                workday_id: Some(self.id),
                workday_ended_at: Some(self.ended_at),
                task_id: session.task_id,
                task_name: session.task_name,
                start: session.start,
                end: session.end,
                seconds: session.seconds,
                duration: fmt_seconds(session.seconds),
            })
            .collect()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct SessionRow {
    pub workday_id: Option<Uuid>,
    pub workday_ended_at: Option<DateTime<Utc>>,
    pub task_id: Uuid,
    pub task_name: String,
    pub start: DateTime<Utc>,
    pub end: DateTime<Utc>,
    pub seconds: i64,
    pub duration: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct PersistedStore {
    schema_version: u32,
    tasks: Vec<Task>,
    #[serde(default)]
    workdays: Vec<WorkdayArchive>,
    #[serde(default)]
    theme_mode: ThemeMode,
}

#[derive(Clone)]
pub struct TaskClockStore {
    tasks: Vec<Task>,
    workdays: Vec<WorkdayArchive>,
    theme_mode: ThemeMode,
    clock: Clock,
}

impl TaskClockStore {
    pub fn new_with_clock<F>(clock: F) -> Self
    where
        F: Fn() -> DateTime<Utc> + Send + Sync + 'static,
    {
        Self {
            tasks: Vec::new(),
            workdays: Vec::new(),
            theme_mode: ThemeMode::default(),
            clock: Arc::new(clock),
        }
    }

    pub fn with_clock(clock: Clock) -> Self {
        Self {
            tasks: Vec::new(),
            workdays: Vec::new(),
            theme_mode: ThemeMode::default(),
            clock,
        }
    }

    pub fn load() -> Result<Self, StoreError> {
        Self::load_from_path(default_data_file(), now_utc)
    }

    pub fn load_from_path<P, F>(path: P, clock: F) -> Result<Self, StoreError>
    where
        P: AsRef<Path>,
        F: Fn() -> DateTime<Utc> + Send + Sync + 'static,
    {
        let path = path.as_ref();
        if !path.exists() {
            return Ok(Self::new_with_clock(clock));
        }

        let raw = fs::read_to_string(path).map_err(|source| StoreError::Read {
            path: path.to_path_buf(),
            source,
        })?;

        let value: Value = serde_json::from_str(&raw).map_err(|source| StoreError::Parse {
            path: path.to_path_buf(),
            source,
        })?;

        let tasks = value
            .get("tasks")
            .and_then(Value::as_array)
            .map(|items| items.iter().filter_map(parse_task_value).collect())
            .unwrap_or_default();
        let workdays = value
            .get("workdays")
            .and_then(Value::as_array)
            .map(|items| items.iter().filter_map(parse_workday_value).collect())
            .unwrap_or_default();
        let theme_mode = value
            .get("theme_mode")
            .and_then(parse_theme_mode_value)
            .unwrap_or_default();

        Ok(Self {
            tasks,
            workdays,
            theme_mode,
            clock: Arc::new(clock),
        })
    }

    pub fn save(&self) -> Result<(), StoreError> {
        self.save_to_path(default_data_file())
    }

    pub fn save_to_path<P: AsRef<Path>>(&self, path: P) -> Result<(), StoreError> {
        let path = path.as_ref();
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent).map_err(|source| StoreError::Write {
                path: parent.to_path_buf(),
                source,
            })?;
        }

        let persisted = PersistedStore {
            schema_version: 2,
            tasks: self.tasks.clone(),
            workdays: self.workdays.clone(),
            theme_mode: self.theme_mode,
        };
        let json =
            serde_json::to_string_pretty(&persisted).map_err(|source| StoreError::Write {
                path: path.to_path_buf(),
                source: std::io::Error::other(source.to_string()),
            })?;
        fs::write(path, json).map_err(|source| StoreError::Write {
            path: path.to_path_buf(),
            source,
        })
    }

    pub fn tasks(&self) -> &[Task] {
        &self.tasks
    }

    pub fn tasks_mut(&mut self) -> &mut [Task] {
        &mut self.tasks
    }

    pub fn workdays(&self) -> &[WorkdayArchive] {
        &self.workdays
    }

    pub fn theme_mode(&self) -> ThemeMode {
        self.theme_mode
    }

    pub fn set_theme_mode(&mut self, theme_mode: ThemeMode) {
        self.theme_mode = theme_mode;
    }

    pub fn toggle_theme_mode(&mut self) -> ThemeMode {
        self.theme_mode = self.theme_mode.toggle();
        self.theme_mode
    }

    pub fn task<I: TaskIdLike>(&self, id: I) -> Option<&Task> {
        let id = id.resolve();
        self.tasks.iter().find(|task| task.id == id)
    }

    pub fn task_mut<I: TaskIdLike>(&mut self, id: I) -> Option<&mut Task> {
        let id = id.resolve();
        self.tasks.iter_mut().find(|task| task.id == id)
    }

    pub fn current_seconds_for<I: TaskIdLike>(&self, id: I) -> Option<i64> {
        let id = id.resolve();
        self.task(id).map(|task| task.current_seconds(&self.clock))
    }

    pub fn clock_now(&self) -> DateTime<Utc> {
        (*self.clock)()
    }

    pub fn add_task(&mut self, name: impl Into<String>) -> Uuid {
        let task = Task::new(name);
        let id = task.id;
        self.tasks.push(task);
        id
    }

    pub fn rename_task<I: TaskIdLike>(
        &mut self,
        id: I,
        name: impl Into<String>,
    ) -> Result<(), StoreError> {
        let id = id.resolve();
        let task = self.task_mut(id).ok_or(StoreError::TaskNotFound(id))?;
        task.rename(name);
        Ok(())
    }

    pub fn delete_task<I: TaskIdLike>(&mut self, id: I) -> Result<(), StoreError> {
        let id = id.resolve();
        let index = self
            .tasks
            .iter()
            .position(|task| task.id == id)
            .ok_or(StoreError::TaskNotFound(id))?;
        if self.tasks[index].is_active() {
            return Err(StoreError::ActiveTaskDelete);
        }
        self.tasks.remove(index);
        Ok(())
    }

    pub fn active_task_id(&self) -> Option<Uuid> {
        self.tasks
            .iter()
            .find(|task| task.is_active())
            .map(|task| task.id)
    }

    pub fn active_task(&self) -> Option<&Task> {
        self.tasks.iter().find(|task| task.is_active())
    }

    pub fn active_task_mut(&mut self) -> Option<&mut Task> {
        self.tasks.iter_mut().find(|task| task.is_active())
    }

    pub fn stop_active(&mut self) -> Result<Option<SessionRecord>, StoreError> {
        let clock = self.clock.clone();
        Ok(self.active_task_mut().and_then(|task| task.stop(&clock)))
    }

    pub fn stamp<I: TaskIdLike>(&mut self, id: I) -> Result<(), StoreError> {
        let id = id.resolve();
        let clock = self.clock.clone();
        let active_id = self.active_task_id();
        match active_id {
            Some(current) if current == id => {
                let task = self.task_mut(id).ok_or(StoreError::TaskNotFound(id))?;
                let _ = task.stop(&clock);
            }
            Some(current) => {
                if let Some(task) = self.task_mut(current) {
                    let _ = task.stop(&clock);
                }
                let task = self.task_mut(id).ok_or(StoreError::TaskNotFound(id))?;
                task.start(&clock);
            }
            None => {
                let task = self.task_mut(id).ok_or(StoreError::TaskNotFound(id))?;
                task.start(&clock);
            }
        }
        Ok(())
    }

    pub fn current_workday_seconds(&self) -> i64 {
        self.tasks
            .iter()
            .map(|task| task.current_seconds(&self.clock))
            .sum()
    }

    pub fn start_new_workday(&mut self) -> Result<WorkdayArchive, StoreError> {
        let clock = self.clock.clone();
        let _ = self.stop_active()?;
        let ended_at = (*clock)();
        let archive = WorkdayArchive {
            id: Uuid::new_v4(),
            ended_at,
            total_seconds: self.tasks.iter().map(|task| task.total_seconds).sum(),
            tasks: self.tasks.clone(),
        };
        self.workdays.push(archive.clone());
        for task in &mut self.tasks {
            task.total_seconds = 0;
            task.active_since = None;
            task.sessions.clear();
        }
        Ok(archive)
    }

    pub fn session_rows(&self) -> Vec<SessionRow> {
        let mut rows = Vec::new();
        let now = self.clock_now();

        for task in &self.tasks {
            rows.extend(task.sessions.iter().cloned().map(|session| SessionRow {
                workday_id: None,
                workday_ended_at: None,
                task_id: session.task_id,
                task_name: session.task_name,
                start: session.start,
                end: session.end,
                seconds: session.seconds,
                duration: fmt_seconds(session.seconds),
            }));

            if let Some(start) = task.active_since {
                let seconds = (now - start).num_seconds().max(0);
                rows.push(SessionRow {
                    workday_id: None,
                    workday_ended_at: None,
                    task_id: task.id,
                    task_name: task.name.clone(),
                    start,
                    end: now,
                    seconds,
                    duration: fmt_seconds(seconds),
                });
            }
        }

        for archive in &self.workdays {
            rows.extend(archive.session_rows());
        }

        rows
    }

    pub fn export_csv<P: AsRef<Path>>(&self, path: P) -> Result<(), StoreError> {
        let path = path.as_ref();
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent).map_err(|source| StoreError::Write {
                path: parent.to_path_buf(),
                source,
            })?;
        }

        let mut writer = Writer::from_path(path).map_err(|source| StoreError::Csv {
            path: path.to_path_buf(),
            source,
        })?;
        writer
            .write_record([
                "workday_id",
                "workday_ended_at",
                "task_id",
                "task_name",
                "start",
                "end",
                "seconds",
                "duration",
            ])
            .map_err(|source| StoreError::Csv {
                path: path.to_path_buf(),
                source,
            })?;

        for row in self.session_rows() {
            writer.serialize(row).map_err(|source| StoreError::Csv {
                path: path.to_path_buf(),
                source,
            })?;
        }
        writer.flush().map_err(|source| StoreError::Write {
            path: path.to_path_buf(),
            source,
        })
    }
}

fn sanitize_name(name: String) -> String {
    let trimmed = name.trim();
    if trimmed.is_empty() {
        "Unbenannte Task".to_string()
    } else {
        trimmed.to_string()
    }
}

fn parse_task_value(value: &Value) -> Option<Task> {
    let object = value.as_object()?;
    let id = object
        .get("id")
        .and_then(Value::as_str)
        .map(task_uuid_from_text)
        .unwrap_or_else(Uuid::new_v4);
    let name = object
        .get("name")
        .and_then(Value::as_str)
        .map(|name| sanitize_name(name.to_string()))
        .unwrap_or_else(|| "Unbenannte Task".to_string());
    let total_seconds = object
        .get("total_seconds")
        .and_then(Value::as_i64)
        .unwrap_or(0);
    let active_since = object
        .get("active_since")
        .and_then(Value::as_str)
        .and_then(|value| value.parse::<DateTime<Utc>>().ok());
    let sessions = object
        .get("sessions")
        .and_then(Value::as_array)
        .map(|entries| entries.iter().filter_map(parse_session_value).collect())
        .unwrap_or_default();

    Some(Task {
        id,
        name,
        total_seconds,
        active_since,
        sessions,
    })
}

fn parse_session_value(value: &Value) -> Option<SessionRecord> {
    let object = value.as_object()?;
    let task_id = object
        .get("task_id")
        .or_else(|| object.get("taskId"))
        .and_then(Value::as_str)
        .map(task_uuid_from_text)
        .unwrap_or_else(Uuid::new_v4);
    let task_name = object
        .get("task_name")
        .or_else(|| object.get("taskName"))
        .and_then(Value::as_str)
        .map(ToString::to_string)
        .unwrap_or_else(|| "Unbenannte Task".to_string());
    let start = object
        .get("start")
        .and_then(Value::as_str)
        .and_then(|value| value.parse::<DateTime<Utc>>().ok())?;
    let end = object
        .get("end")
        .and_then(Value::as_str)
        .and_then(|value| value.parse::<DateTime<Utc>>().ok())?;
    let seconds = object
        .get("seconds")
        .and_then(Value::as_i64)
        .unwrap_or_else(|| (end - start).num_seconds().max(0));

    Some(SessionRecord {
        task_id,
        task_name,
        start,
        end,
        seconds,
    })
}

fn parse_workday_value(value: &Value) -> Option<WorkdayArchive> {
    let object = value.as_object()?;
    let id = object
        .get("id")
        .and_then(Value::as_str)
        .map(task_uuid_from_text)
        .unwrap_or_else(Uuid::new_v4);
    let ended_at = object
        .get("ended_at")
        .and_then(Value::as_str)
        .and_then(|value| value.parse::<DateTime<Utc>>().ok())
        .unwrap_or_else(Utc::now);
    let tasks: Vec<Task> = object
        .get("tasks")
        .and_then(Value::as_array)
        .map(|entries| entries.iter().filter_map(parse_task_value).collect())
        .unwrap_or_default();
    let total_seconds = object
        .get("total_seconds")
        .and_then(Value::as_i64)
        .unwrap_or_else(|| tasks.iter().map(|task| task.total_seconds).sum());

    Some(WorkdayArchive {
        id,
        ended_at,
        total_seconds,
        tasks,
    })
}

fn parse_theme_mode_value(value: &Value) -> Option<ThemeMode> {
    match value.as_str()?.trim().to_ascii_lowercase().as_str() {
        "dark" => Some(ThemeMode::Dark),
        "light" => Some(ThemeMode::Light),
        _ => None,
    }
}
