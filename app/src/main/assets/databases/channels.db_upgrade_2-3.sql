/*
 * Note, these upgrade files MUST use double quotes for string literals
 * due to a bug in SQLiteAssetHelper
 */

CREATE TABLE IF NOT EXISTS streams_legacy (
    _id INTEGER PRIMARY KEY,
    game TEXT NOT NULL DEFAULT "",
    type INTEGER NOT NULL DEFAULT 0,
    title TEXT NOT NULL DEFAULT "",
    viewer_count INTEGER NOT NULL DEFAULT 0,
    started_at INTEGER NOT NULL DEFAULT 0,
    language TEXT NOT NULL DEFAULT "en",
    thumbnail_url TEXT NOT NULL DEFAULT ""
);