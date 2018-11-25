(ns cljdoc.clojars-stats
  (:require [cljdoc.clojars-stats.db :as db]
            [cljdoc.clojars-stats.api :as api]
            [clojure.java.jdbc :as sql]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [tea-time.core :as tt])
  (:import (java.time Instant Duration LocalDate)
           (java.time.temporal ChronoUnit)))

(defn stats-files []
  (->> (slurp "https://clojars.org/stats/")
       (re-seq #"downloads-\d{8}\.edn")
       (map #(str "https://clojars.org/stats/" %))
       (set)))

(defn uri->date [uri]
  (some->> (re-find #"(\d{4})(\d{2})(\d{2})" uri)
           (rest)
           (string/join "-")
           (LocalDate/parse)))

(defn stats-for-date-exist? [db-spec date]
  (println "Checking" date)
  (-> (sql/query db-spec ["select exists(SELECT date FROM stats where date = ? limit 1)" date])
      first first val pos?))

(defn to-import [db-spec]
  (let [retention-years 1
        statsf (stats-files)]
    (->> (zipmap (map uri->date statsf) statsf)
         (remove #(.isBefore (key %)
                             (.minusYears (LocalDate/now) retention-years)))
         (remove #(stats-for-date-exist? db-spec (key %)))
         vals
         sort)))

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
            (edn/read-string (slurp uri)))))

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

(defn start! [{:keys [db-spec]}]
  (db/create-stats-table db-spec)
  (db/create-imported-files-table db-spec)
  (println "Starting background process...")
  (tt/start!)
  (tt/every! (* 1 60) (bound-fn [] (update! db-spec))))

(defn -main [db-file]
  (start! {:db-spec {:classname   "org.sqlite.JDBC"
                     :subprotocol "sqlite"
                     :subname     db-file}
           :retention (Duration/of 365 ChronoUnit/DAYS)})
  (let [signal (java.util.concurrent.CountDownLatch. 1)]
    (.await signal)))

(comment
  (def db {:classname   "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname     "clojars-stats.db"})

  (time (-main))
  (.minusYears (LocalDate/now) 1)

  (to-import db)

  (stats-for-date-exist? db (LocalDate/parse "2017-05-01"))

  (db/create-imported-files-table db)
  (db/create-stats-table db)
  (db/artifact-monthly db {:group_id "reagent" :artifact_id "reagent"})

  (sql/insert! db :imported_files {:date "a" :uri "b"})

  (while (update! db))


  (update! db)

  )
