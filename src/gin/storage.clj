(ns gin.storage
  (:require [integrant.core :as ig]
            [hikari-cp.core :as cp]))

(def metastore-defaults
  {:adapter "postgresql"
   :database-name "hive"
   :minimum-idle 1})

(defmethod ig/prep-key ::metastore
  [_ options]
  (merge metastore-defaults options))

(defmethod ig/init-key ::metastore [_ options]
  (cp/make-datasource options))

(defmethod ig/halt-key! ::metastore
  [_ db-spec]
  (cp/close-datasource db-spec))
