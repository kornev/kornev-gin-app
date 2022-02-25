(ns gin.storage.hdfs
  (:require [clojure.string :as str])
  (:import (org.apache.hadoop.fs FileSystem Path)))

(defn- move
  [^FileSystem fs ^Path src ^Path dst]
  (. fs mkdirs dst)
  (. fs delete dst true)
  (. fs rename src dst))

(defn- unlink
  [^FileSystem fs ^Path src]
  (. fs delete src true))

(defn- path
  [^String s]
  (. Path getPathWithoutSchemeAndAuthority (Path. s)))

(defn- partition-dir
  [state]
  (let [db-home (:LOCATION state)
        cols (:PART_COLS state)
        vals (:PART_KEY_VALS state)]
    (->> (map #(str %1 "=" %2 ) cols vals)
         (str/join "/")
         (str db-home "/"))))

(defn upload-partition
  [hadoop-fs state]
  (let [src (-> (:DATA_LOCATION state) path)
        dst (-> (partition-dir state) path)]
    (move hadoop-fs src dst)
    {:PART_LOCATION (. dst toString)}))

(defn unload-partition
  [hadoop-fs state]
  (let [src (-> state :LOCATION path)]
    (unlink hadoop-fs src)
    {:PART_LOCATION (. src toString)}))
