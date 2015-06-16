(ns text-stream.data
  (:require [korma.db :refer :all]
            [korma.core :refer :all]
            [text-stream.edits :as edits]))

(defdb db (postgres {:db "textstream"
                  :user "field-thompson"}))

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
