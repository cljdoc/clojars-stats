(require '[clojure.java.jdbc :as sql]
         '[clojure.edn :as edn]
         '[clj-http.lite.client :as http])
(import (java.time Instant))

(defn stats-files []
  (->> (:body (http/get "https://clojars.org/stats/"))
       (re-seq #"downloads-\d{8}\.edn")
       (map #(str "https://clojars.org/stats/" %))
       (set)
       (sort)))

;; (def f (first (stats-files)))

(defn parse-file [uri]
  (let [date (some->> (re-find #"(\d{4})(\d{2})(\d{2})" uri)
                      (rest)
                      (clojure.string/join "-"))]
    (assert date)
    (mapcat (fn [[[group artifact] downloads]]
              (for [d downloads]
                {:date (do (assert date) date)
                 :group_id (do (assert group) group)
                 :artifact_id (do (assert artifact) artifact)
                 :version (do (assert (key d)) (key d))
                 :downloads (do (assert (val d)) (val d))}))
            (edn/read-string (:body (http/get uri))))))

;; (clojure.pprint/pprint
;;  (take 5 (parse-file f)))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "clojars-stats.db"})

(defn create-stats-table! [db]
  (sql/db-do-commands db
                      (sql/create-table-ddl
                       :stats
                       [[:date :text]
                        [:group_id :text]
                        [:artifact_id :text]
                        [:version :text]
                        [:downloads :int]])))

(defn import! [files]
  (doseq [f files]
    (printf "Importing %s ..." f)
    (let [parsed-stats (parse-file f)]
      (printf "%s releases\n" (count parsed-stats))
      (flush)
      (sql/insert-multi! db :stats parsed-stats))))

(comment
  (do 
    (.delete (java.io.File. "clojars-stats.db"))
    (create-stats-table! db)
    (import! (take 10 (stats-files))))

  )
