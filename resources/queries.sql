-- :name create-imported-files-table
-- :command :execute
-- :result :raw
-- :doc Create imported_files table
create table imported_files (
  date       TEXT,
  uri        TEXT
)

-- :name create-stats-table
-- :command :execute
-- :result :raw
-- :doc Create stats table
create table stats (
  date        TEXT,
  group_id    TEXT,
  artifact_id TEXT,
  version     TEXT,
  downloads   INT,
  CONSTRAINT stats_keys UNIQUE (date, group_id, artifact_id, version)
)

-- :name artifact-monthly
-- :doc Monthly downloads for specified artifact
SELECT strftime('%Y-%m', date) as timespan, SUM(downloads) as downloads
FROM stats
WHERE group_id = :group_id AND artifact_id = :artifact_id
GROUP BY timespan;
