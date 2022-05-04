(ns gin.log
  (:require [clojure.tools.logging :as log]
            [fipp.edn :refer [pprint]]
            [next.jdbc :as jdbc]))

(defn- sql-query-logger
  [tid]
  (fn
    [sym sql-params]
    (log/debug (str tid " " sym "\n")
               (str (first sql-params) "\n")
               (-> sql-params rest vec))
    (System/currentTimeMillis)))

(defn- sql-result-logger
  [tid]
  (fn
    [sym state result]
    (log/debug (str tid " " sym "\n")
               (with-out-str (pprint result))
               (str (- (System/currentTimeMillis) state)
                    "ms"))))

(defn wrap-log-execute
  [connectable tid]
  (let [id (rand-int 99999)]
    (jdbc/with-logging connectable
                       (sql-query-logger (str tid ":" id))
                       (sql-result-logger (str tid ":" id)))))
