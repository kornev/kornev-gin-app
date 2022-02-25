(ns gin.storage
  (:require [integrant.core :as ig]
            [next.jdbc.connection :as connection]
            [next.jdbc :as jdbc]
            [gin.hadoop :as hadoop])
  (:import (com.zaxxer.hikari HikariDataSource)
           (java.io Closeable)))

;;; Metastore

(defmethod ig/prep-key ::metastore
  [_  {:keys [jdbc-spec hikari-spec]}]
  (assoc hikari-spec :jdbcUrl (next.jdbc.connection/jdbc-url jdbc-spec)))

(defmethod ig/init-key ::metastore
  [_ db-spec]
  (connection/->pool HikariDataSource db-spec))

(defmethod ig/halt-key! ::metastore
  [_ pool]
  (.close ^Closeable pool))

;;; Hive

(defmethod ig/prep-key ::hive
  [_  {:keys [jdbc-spec hikari-spec]}]
  ;(assoc hikari-spec :jdbcUrl (next.jdbc.connection/jdbc-url jdbc-spec))
  jdbc-spec
  )

(defmethod ig/init-key ::hive
  [_ db-spec]
  ;(connection/->pool HikariDataSource db-spec)
  (jdbc/get-datasource db-spec ))

(defmethod ig/halt-key! ::hive
  [_ pool]
  (.close ^Closeable pool))

;;; Hdfs

(defmethod ig/init-key ::hdfs
  [_ {:keys [hadoop-spec]}]
  (-> hadoop-spec
      hadoop/conf
      hadoop/fs))

(defmethod ig/halt-key! ::hdfs
  [_ fs]
  (.close ^Closeable fs))