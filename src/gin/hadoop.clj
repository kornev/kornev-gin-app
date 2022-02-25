(ns gin.hadoop
  (:import (org.apache.hadoop.conf Configuration)
           (org.apache.hadoop.fs FileSystem)))

(defn conf [hadoop-spec]
  (let [conf (Configuration.)]
    (doseq [[k v] hadoop-spec]
      (. conf set k v))
  conf))

(defn fs [^Configuration conf]
  (FileSystem/newInstance
    (. FileSystem (getDefaultUri conf)) conf (. conf get "hadoop.job.ugi")))
