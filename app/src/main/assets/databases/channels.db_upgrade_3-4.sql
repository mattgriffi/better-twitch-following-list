/*
 * Note, these upgrade files MUST use double quotes for string literals
 * due to a bug in SQLiteAssetHelper
 */

ALTER TABLE games ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0;