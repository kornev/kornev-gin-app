(ns gin.ioc.hadoop
  (:import (org.apache.hadoop.conf Configuration)
           (org.apache.hadoop.fs FileSystem)))

(defn props
  [{:keys [host fs-metadata-port]} user]
  {"fs.defaultFS" (str "hdfs://" host ":" fs-metadata-port "/user/" user)
   "hadoop.job.ugi" user})

(defn conf
  [hadoop-spec]
  (let [conf (Configuration.)]
    (doseq [[k v] hadoop-spec]
      (. conf set k v))
    conf))

(defn fs
  [^Configuration conf]
  (FileSystem/newInstance
   (. FileSystem (getDefaultUri conf)) conf (. conf get "hadoop.job.ugi")))
