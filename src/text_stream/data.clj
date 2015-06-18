(ns text-stream.data
  (:require [korma.db :refer :all]
            [korma.core :refer :all]
            [text-stream.edits :as edits]))



(defdb db
  (if (System/getenv "DATABASE_URL")
    (let [db-uri (java.net.URI. (System/getenv "DATABASE_URL"))
          user-and-password (clojure.string/split (.getUserInfo db-uri) #":")]
      {:classname "org.postgresql.Driver"
       :subprotocol "postgresql"
       :user (get user-and-password 0)
       :password (get user-and-password 1) ; may be nil
       :subname (if (= -1 (.getPort db-uri))
                  (format "//%s%s" (.getHost db-uri) (.getPath db-uri))
                  (format "//%s:%s%s" (.getHost db-uri) (.getPort db-uri)
                          (.getPath db-uri)))})
    (postgres
     {:db "textstream"})))

(defentity stream)

(defn find-stream [stream-id]
  (first
   (select stream
           (where (= :id stream-id))
           (limit 1))))

(defn add-stream [text]
  (insert stream
          (values (edits/initial-text text))))

(defn update-stream [source-map]
  (update stream
   (set-fields (dissoc source-map :id))
   (where (= :id (:id source-map)))))

(defn get-stream-page [page-count page]
  (select stream
          (fields :id :title)
          (limit page-count)
          (offset (* page page-count))))

(defn last-page? [page-count page]
  (let [count (:cnt
               (first
                (select stream (aggregate (count :id) :cnt))))]
    (or (<= count page-count)
        (not (< (* page-count page) (/ (dec count) page-count))))))
