(ns cljdoc.clojars-stats.api
  (:require [cljdoc.clojars-stats.db :as db]
            [ring.middleware.params :as params]
            [ring.adapter.jetty :as jetty]
            [reitit.ring :as ring]
            [muuntaja.core :as m]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.coercion.spec :as spec]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja-middleware]
            [reitit.ring.middleware.exception :as exception-middleware]))

(def exposed-queries
  ;; NEVER change those map entries, only add new stuff as needed
  {"/artifact-monthly" {:query-fn db/artifact-monthly
                        :summary "get downloads per timespan"
                        :params {:group_id string?
                                 :artifact_id string?}}})

(defn routes [db]
  (for [[path {:keys [query-fn params summary]}] exposed-queries]
    [path {:get {:parameters {:query params}
                 :swagger {:summary summary}
                 :handler (fn [request]
                            (let [data (get-in request [:parameters :query])]
                              {:status 200
                               :body (query-fn db data)}))}}]))

(defn make-handler [db]
  (ring/ring-handler
    (ring/router
      [["/swagger.json"
        {:get {:no-doc true
               :swagger {:title "Clojars Stats API"
                         :description "https://github.com/cljdoc/clojars-stats"}
               :handler (swagger/create-swagger-handler)}}]
       (routes db)]
      {:data {:coercion spec/coercion
              :muuntaja m/instance
              :middleware [params/wrap-params
                           muuntaja-middleware/format-negotiate-middleware
                           muuntaja-middleware/format-response-middleware
                           exception-middleware/exception-middleware
                           muuntaja-middleware/format-request-middleware
                           coercion/coerce-response-middleware
                           coercion/coerce-request-middleware]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:docExpansion "list"
                  :validatorUrl nil}})
      (ring/create-default-handler))))

(defn start! [{:keys [db port]}]
  (assert (and db port))
  (jetty/run-jetty (make-handler db) {:port port :join? false}))

(comment
  (def db {:classname "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname "clojars-stats.db"})
  (def srv (start! {:db db :port 3000}))
  (.stop srv)
  )
