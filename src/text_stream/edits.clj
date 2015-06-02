(ns text-stream.edits)

(defn consolidate
  "Takes a seq of edits and composes them into a single function. Applies from
  last to first of edits seq. This makes composing a list of edits very easy
  with `cons`."
  [edits]
  (apply comp edits))

(defn perform
  "Performs the `edits` (in the manner defined by the `consolidate` function)
  on the provided `source-map`."
  [edits source-map]
  ((consolidate edits) source-map))

(defn cursor
  "Produces an edit function which moves the cursor to index `position-index`
  in the text."
  [position-index]
  (fn [source-map]
    (assoc source-map :pos position-index)))

(defn insert
  "Produces an edit function which inserts `text-insertion` into the text at
  the current cursor position."
  [text-insertion]
  (fn [source-map]
    (let [old-text (:text source-map)
          cursor (:pos source-map)
          new-text (str
                    (subs old-text 0 cursor)
                    text-insertion
                    (subs old-text cursor))
          new-position (+ cursor (count text-insertion))]
      (merge source-map
             {:pos new-position
              :text new-text}))))

(defn delete
  "Produces an edit function which deletes `deletion-count` characters from
  the text from the current cursor position (backwards)."
  [deletion-count]
  (fn [source-map]
    (let [old-text (:text source-map)
          cursor (:pos source-map)
          new-position (- cursor deletion-count)
          new-text (str
                    (subs old-text 0 new-position)
                    (subs old-text cursor))]
      (merge source-map
             {:pos new-position
              :text new-text}))))

(defn titled
  "Produces a titling function which sets the title to `title`."
  [title]
  (fn [source-map]
    (assoc source-map :title title)))

(defn forward-delete
  "Produces an edit function which deletes `deletion-count` characters from
  the text forwards from the current cursor position."
  [deletion-count]
  #((delete deletion-count)
    ((cursor (+ deletion-count (:pos %)))
     %)))

(defn initial-text
  "Produces a source-map which has the initial text `text`."
  [text]
  ((insert text)
   {:pos 0 :text "" :title ""}))

(def commands
  "Note: all commands happen to be 6 characters. 
  We'll exploit this later."
  {"inited" initial-text
   "insert" insert
   "delete" delete
   "cursor" cursor
   "titled" titled
   "fwddel" forward-delete})

(def cmd-len "Length of commands." 6)

(def cmd-validators
  "Functions which convert a string to fit type expected by command,
  or, if not possible, return `nil`."
  (let [parse-int #(try (Integer/parseInt (clojure.string/trim %))
                       (catch NumberFormatException _ nil))]
    {;; inited requires a different function, as it is only valid as the
     ;; first message in a stream
     initial-text (fn [_ _] nil)

     ;; we don't escape html here because the client does that and we
     ;; want to be able to send literal characters to non-web-based viewers
     insert (fn [in _] (and (string? in) in))

     delete (fn [in source-map]
              (if-let [n (parse-int in)]
                (and (<= n (:pos source-map)) n)))

     forward-delete (fn [in source-map]
              (if-let [n (parse-int in)]
                (and (< (:pos source-map) (count (:text source-map))) n)))

     cursor (fn [in source-map]
              (if-let [n (parse-int in)]
                (and (<= n (count (:text source-map))) n)))

     titled (fn [in _] (and (string? in) in))}))

(defn valid-command?
  "Is this a valid command? Returns the command string."
  [text]
  (and (>= (count text) (inc cmd-len))
       (let [cmd (subs text 0 cmd-len)]
         (and
          (contains? commands cmd)
          cmd))))

(defn process-text-command
  [text source-map]
  (if-let [cmd (valid-command? text)]
    (let [func (get commands cmd)
          text-input (subs text (inc cmd-len))]
      (if-let [valid-input ((get cmd-validators func)
                            text-input source-map)]
        ((func valid-input) source-map)))))
