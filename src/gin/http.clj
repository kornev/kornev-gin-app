(ns gin.http
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import (org.eclipse.jetty.util.component LifeCycle)))

(defmethod ig/init-key ::server
  [_ {:keys [handler jetty-spec]}]
  (run-jetty handler
             (assoc jetty-spec :join? false)))

(defmethod ig/halt-key! ::server
  [_ server]
  (.stop ^LifeCycle server))
