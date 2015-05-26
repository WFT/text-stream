(ns text-stream.edits-model)

(defn perform)

(defn consolidate
  "Takes a seq of edits and composes them into a single function.
   Applies from front to back of edits seq."
  [edits]
  (apply comp edits))

(defn move-cursor
  "Edit moves the cursor to a particular position.
"
  [position-index]
  (fn [source-map]
    (assoc-in source-map [:pos] position-index)))

(defn insert-text
  [text-insertion]
  (fn [source-map]
    (let [old-text (:text source-map)
          cursor (:pos source-map)
          new-text (str
                    (subs old-text 0 cursor)
                    text-insertion
                    (subs old-text cursor))
          new-position (+ cursor (count new-text))]
      {:pos new-position :text new-text})))

(defn delete-text
  [deletion-count]
  (fn [source-map]
    (let [old-text (:text source-map)
          cursor (:pos source-map)
          new-position (- cursor deletion-count)
          new-text (str
                    (subs old-text 0 new-position)
                    (subs old-text cursor))]
      {:pos new-position :text new-text})))

(defn initial-text
  [text]
  ((insert-text text)
   {:pos 0 :text ""}))
