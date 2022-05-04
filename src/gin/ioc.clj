(ns gin.ioc
  (:require [integrant.core :as ig]
            [farseer.http :refer [make-app] :rename {make-app make-handler}]
            [ring.adapter.jetty :refer [run-jetty]]
            [next.jdbc.connection :as connection]
            [next.jdbc :as jdbc]
            [gin.ioc.hadoop :as hadoop]
            [gin.ioc.jmx :as jmx]
            [gin.rpc :as rpc])
  (:import (org.eclipse.jetty.util.component LifeCycle)
           (com.zaxxer.hikari HikariDataSource)
           (java.io Closeable)))

;;; Metastore

(defmethod ig/prep-key ::metastore
  [_  {:keys [jdbc-spec hikari-spec]}]
  (assoc hikari-spec :jdbcUrl (next.jdbc.connection/jdbc-url jdbc-spec)))

(defmethod ig/init-key ::metastore
  [_ db-spec]
  (connection/->pool HikariDataSource db-spec))

(defmethod ig/halt-key! ::metastore
  [_ connection-impl]
  (.close ^Closeable connection-impl))

;;; Hive

(defmethod ig/prep-key ::hive
  [_  {:keys [jdbc-spec hikari-spec]}]
  (let [{:keys [dbtype host port user password]} jdbc-spec]
    (merge hikari-spec
           {:jdbcUrl (str "jdbc:" dbtype "://" host ":" port "?hive.resultset.use.unique.column.names=false")
            :username user
            :password password})))

(defmethod ig/init-key ::hive
  [_ db-spec]
  (connection/->pool HikariDataSource db-spec))

(defmethod ig/halt-key! ::hive
  [_ connection-impl]
  (.close ^Closeable connection-impl))

;;; Hdfs

(defmethod ig/init-key ::hdfs
  [_ {:keys [hadoop-spec]}]
  (let [{:keys [namenodes user props]} hadoop-spec]
    (for [nn namenodes]
      {:fs (-> (merge props
                      (hadoop/props nn user))
               hadoop/conf
               hadoop/fs)
       :jmx-req-keys (jmx/req-keys nn)})))

(defmethod ig/halt-key! ::hdfs
  [_ hadoop-impl]
  (doseq [m hadoop-impl
          :let [fs (:fs m)]]
    (.close ^Closeable fs)))

;;; Http

(defmethod ig/init-key ::http
  [_ {:keys [handler jetty-spec]}]
  (run-jetty handler
             (assoc jetty-spec :join? false)))

(defmethod ig/halt-key! ::http
  [_ jetty-impl]
  (.stop ^LifeCycle jetty-impl))

;;; Rpc

(def ^:private farseer-default-spec
  {:http/path "/rpc"
   :rpc/handlers {:table/describe {:handler/function #'rpc/table-describe-rpc}
                  :partition/list {:handler/function #'rpc/partition-list-rpc}
                  :partition/find {:handler/function #'rpc/partition-find-rpc}
                  :partition/parent {:handler/function #'rpc/partition-parent-rpc}
                  :partition/attach {:handler/function #'rpc/partition-attach-rpc}
                  :partition/detach {:handler/function #'rpc/partition-detach-rpc}}})

(defmethod ig/init-key ::rpc
  [_ {:keys [metastore hive hdfs farseer-spec]}]
  (make-handler (merge farseer-default-spec
                       farseer-spec)
                {:metastore-ds metastore
                 :hive-ds hive
                 :hadoop-ctx hdfs}))
