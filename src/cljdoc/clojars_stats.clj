(ns cljdoc.clojars-stats
  (:require [cljdoc.clojars-stats.db :as db]
            [cljdoc.clojars-stats.api :as api]
            [clojure.java.jdbc :as sql]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-http.lite.client :as http]
            [tea-time.core :as tt])
  (:import (java.time Instant)))

(def data-dir (doto (io/file "data") (.mkdir)))

(defn stats-files []
  (->> (:body (http/get "https://clojars.org/stats/"))
       (re-seq #"downloads-\d{8}\.edn")
       (map #(str "https://clojars.org/stats/" %))
       (set)))

(defn read-file [uri]
  (let [data-dir (io/file "data")]
    (if (.exists data-dir)
      (let [local-file (io/file data-dir (last (string/split uri #"/")))]
        (when-not (.exists local-file)
          (println "Downloading" (.getName local-file))
          (spit local-file (slurp uri)))
        (edn/read-string (slurp local-file)))
      (edn/read-string (slurp uri)))))

(defn uri->date [uri]
  (some->> (re-find #"(\d{4})(\d{2})(\d{2})" uri)
           (rest)
           (string/join "-")))

(defn parse-file [uri]
  (let [date (uri->date uri)]
    (assert date)
    (mapcat (fn [[[group artifact] downloads]]
              (for [d downloads]
                {:date (do (assert date) date)
                 :group_id (do (assert group) group)
                 :artifact_id (do (assert artifact) artifact)
                 :version (do (assert (key d)) (key d))
                 :downloads (do (assert (val d)) (val d))}))
            (read-file uri))))

(defn import-file! [db f]
  (printf "Reading %s ..." f)
  (let [parsed-stats (parse-file f)]
    (printf "%s releases\n" (count parsed-stats))
    (flush)
    (sql/insert-multi! db :stats parsed-stats)))

(defn import! [db files]
  (doseq [f files]
    (import-file! db f)
    (sql/insert! db :imported_files {:date (uri->date f) :uri f})))

(defn not-yet-imported [db]
  (->> (map :uri (sql/query db ["SELECT uri FROM imported_files"]))
       (apply disj (stats-files))
       (sort)
       (reverse)))

(defn update! [db]
  (let [todo (not-yet-imported db)]
    (println (count todo) "files to import...")
    (import! db (take 20 todo))
    (< 0 (count todo))))

(defn -main [db-file]
  (let [db {:classname   "org.sqlite.JDBC"
            :subprotocol "sqlite"
            :subname     db-file}]
    (when-not (.exists (java.io.File. db-file))
      (println "Creating tables...")
      (db/create-stats-table db)
      (db/create-imported-files-table db))
    (println "Starting background process...")
    (tt/start!)
    (api/start! {:db db :port 3000})
    (tt/every! (* 1 60) (bound-fn [] (update! db)))
    (let [signal (java.util.concurrent.CountDownLatch. 1)]
      (.await signal))))

(comment
  (time (-main))

  (db/create-imported-files-table db)
  (db/artifact-monthly db {:group_id "reagent" :artifact_id "reagent"})

  (sql/insert! db :imported_files {:date "a" :uri "b"})

  (while (update!))

  )
