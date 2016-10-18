(ns chatlogin.utils
  (:require [clojure.data.json :as json]
            [hiccup.core :refer [html]]
            [ring.util.response :refer [response redirect content-type status]]))


(defn http-error [code message]
  {:code code :message message})


(defn to-json [x]
  (json/write-str x))

(defn from-json [text]
  (when text
    (json/read-str text
                   :key-fn keyword)))

(defn json-resp
  ([x] (json-resp x 200))
  ([x http-status]
   (-> (response (to-json x))
     (status http-status)
     (content-type "application/json"))))

(defn html-resp
  ([x] (html-resp x 200))
  ([x http-status]
   (-> (response (html x))
     (status http-status)
     (content-type "text/html"))))


(defn json-resp-error [error]
  (json-resp error (:code error)))


(defn mapmap-kv [f m]
  "maps hashmaps with pairs"
  (reduce-kv (fn [prev k v]
                (let [[n-k n-v] (f k v)]
                  (assoc prev n-k n-v))) {} m))

(defn mapmap [f m]
  "maps hashmaps"
  (mapmap-kv (fn [k v] (list k (f v))) m))


(defn uuid [] (str (java.util.UUID/randomUUID)))
