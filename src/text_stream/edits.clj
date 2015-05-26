(ns text-stream.edits
  (:gen-class))

(defn perform
  "Performs the `edits` (in the manner defined by the `consolidate` function)
  on the provided `source-map`."
  [edits source-map]
  ((consolidate edits) source-map))

(defn consolidate
  "Takes a seq of edits and composes them into a single function. Applies from
  last to first of edits seq. This makes composing a list of edits very easy
  with `cons`."
  [edits]
  (apply comp edits))

(defn cursor
  "Produces an edit function which moves the cursor to index `position-index`
  in the text."
  [position-index]
  (fn [source-map]
    (assoc-in source-map [:pos] position-index)))

(defn insert
  "Produces an edit function which inserts `text-insertion` into the text at
  the current cursor position."
  [text-insertion]
  (fn [source-map]
    (println "insert into:" source-map)
    (let [old-text (:text source-map)
          cursor (:pos source-map)
          new-text (str
                    (subs old-text 0 cursor)
                    text-insertion
                    (subs old-text cursor))
          new-position (+ cursor (count text-insertion))]
      {:pos new-position :text new-text})))

(defn delete
  "Produces an edit function which deletes `deletion-count` characters from
  the text from the current cursor position (backwards)."
  [deletion-count]
  (fn [source-map]
    (println "delete from" source-map)
    (let [old-text (:text source-map)
          cursor (:pos source-map)
          new-position (- cursor deletion-count)
          new-text (str
                    (subs old-text 0 new-position)
                    (subs old-text cursor))]
      {:pos new-position :text new-text})))

(defn forward-delete
  "Produces an edit function which deletes `deletion-count` characters from
  the text forwards from the current cursor position."
  [deletion-count]
  #((delete deletion-count)
    ((cursor (+ deletion-count (:pos %)))
     %)))

(defn initial-text
  [text]
  ((insert-text text)
   {:pos 0 :text ""}))
