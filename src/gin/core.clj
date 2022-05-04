(ns gin.core
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [signal.handler :refer [with-handler]]
            [gin.util :as util])
  (:gen-class))

(defonce ^:private system nil)

(def alter-system (partial alter-var-root #'system))

(defn system-start
  [s]
  (let [config (util/read-system s)]
    (log/info "Initializing components: metastore, hive, hdfs, http and rpc")
    (ig/load-namespaces config)
    (->> config ig/prep ig/init constantly alter-system)))

(defn system-stop
  []
  (alter-system ig/halt!))

(with-handler :term
  (log/info "Caught SIGTERM, shutting down")
  (system-stop)
  (log/info "All components shut down")
  (System/exit 0))

(defn -main
  []
  (try
    (system-start "config.edn")
    (catch Throwable t
      (log/error t)
      (System/exit 1))))
