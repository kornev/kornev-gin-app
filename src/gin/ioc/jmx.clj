(ns gin.ioc.jmx
  (:require [clj-http.client :as http]))

(defn req-keys
  [{:keys [host web-ui-port]}]
  {:api (str "http://" host ":" web-ui-port "/jmx")
   :opts {:accept :json
          :as :json}})

(defn beans
  [{:keys [api opts]} ref]
  (-> (http/get (str api "?qry=" ref) opts)
      :body
      :beans))

(defn active?
  [{:keys [jmx-req-keys]}]
  (let [mbean-name "Hadoop:service=NameNode,name=NameNodeStatus"
        mbean? (fn [{:keys [name] :as name-node}]
                 (when (= mbean-name name) name-node))]
    (->> (beans jmx-req-keys mbean-name)
         (some mbean?)
         :State
         (= "active"))))

(defn active-fs
  [hadoop-ctx]
  (some #(when (active? %) (:fs %))
        hadoop-ctx))
