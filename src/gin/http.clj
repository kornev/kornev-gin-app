(ns gin.http
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import [org.eclipse.jetty.server Server]))

(defmethod ig/init-key ::server
  [_ {:keys [handler options]}]
  (run-jetty handler
             (assoc options :join? false)))

(defmethod ig/halt-key! ::server
  [_ server]
  (.stop ^Server server))
