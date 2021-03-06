(ns text-stream.edits-test
  (:require [clojure.test :refer :all]
            [text-stream.edits :refer :all]))

(deftest source-map
  (testing "initial-text"
    (let [text "Hello, world!"
          pos (count text)
          source-map (initial-text text)]
      (is (= (:content source-map) text))
      (is (= (:pos source-map) pos)))))

(deftest editing-functions
  (testing "titled"
    (let [title "This is our title!"
          text "Hello, world!"
          source-map (initial-text text)
          titled-map ((titled title) source-map)]
      (is (= (:title titled-map) title)
          "Changing the title.")
      (is (= (assoc source-map :title title) titled-map)
          "Maintaining other keys.")))

  (testing "cursor"
    (let [title "Title"
          text "Hello, world!"
          source-map ((titled title) (initial-text text))
          index (int (/ (count text) 2))
          cursor-map ((cursor index) source-map)]
      (is (= (:pos cursor-map) index)
          "Changing cursor position.")
      (is (= (assoc source-map :pos index) cursor-map)
          "Maintaining other keys.")))

  (testing "cursor-left"
    (let [title "Title"
          text "Hello, world!"
          source-map ((titled title) (initial-text text))
          cursor1-map ((cursor-left 1) source-map)
          cursor-all-map ((cursor-left (count text)) source-map)]

      (testing "left 1"
        (is (= (:pos cursor1-map) (dec (count text)))
            "Changing cursor position.")
        (is (= cursor1-map (assoc source-map :pos (dec (count text))))
            "Maintaining other keys."))

      (testing "left all"
        (is (= (:pos cursor-all-map) 0)
            "Changing cursor position.")
        (is (= cursor-all-map (assoc source-map :pos 0))
            "Maintaining other keys."))))

  (testing "cursor-right"
    (let [title "Title"
          text "Hello, world!"
          source-map ((comp (cursor 0) (titled title)) (initial-text text))
          cursor1-map ((cursor-right 1) source-map)
          cursor-all-map ((cursor-right (count text)) source-map)]

      (testing "right 1"
        (is (= (:pos cursor1-map) 1)
            "Changing cursor position.")
        (is (= cursor1-map (assoc source-map :pos 1))
            "Maintaining other keys."))

      (testing "right all"
        (is (= (:pos cursor-all-map) (count text))
            "Changing cursor position.")
        (is (= cursor-all-map (assoc source-map :pos (count text)))
            "Maintaining other keys."))))
  
  (testing "delete"
    (let [text "Hello, world!"
          source-map ((titled "Test") (initial-text text))
          tests #(for [n (range %1)]
                   (let [pos (- (:pos %2) n)]
                     {:n n
                      :result ((delete n) %2)
                      :correct
                      (merge %2
                             {:pos pos
                              :content (str
                                     (subs text 0 pos)
                                     (subs text (:pos %2)))})}))
          end-tests (tests (inc (:pos source-map)) source-map)
          middle-index (int (/ (count text) 2))
          middle-tests (tests middle-index ((cursor middle-index) source-map))]
      
      (testing "from end"
        (for [t end-tests]
          (is (= (:result t) (:correct t)))))

      (testing "from middle"
        (for [t end-tests]
          (is (= (:result t) (:correct t)))))

      (testing "out of bounds"
        (is (thrown? StringIndexOutOfBoundsException
                     ((delete (inc (count text))) source-map))))))
  
  (testing "forward-delete"
    (let [text "Hello, world!"
          source-map ((titled "Test") (initial-text text))
          beginning-map ((cursor 0) source-map)
          middle-map ((cursor (int (/ (count text) 2))) source-map)]

      (testing "from beginning"
        (is (= ((forward-delete 1) beginning-map)
               ((comp (delete 1) (cursor 1)) source-map))
            "Deleting forwards 1 from :pos 0."))

      (testing "from middle"
        (is (= ((forward-delete 1) middle-map)
               ((comp (delete 1) (cursor (inc (:pos middle-map))))
                source-map))
            "Deleting forwards 1 from the middle."))
      
      (testing "out of bounds"
        (is (thrown? StringIndexOutOfBoundsException
                     ((forward-delete 1) source-map)))))))

(deftest consolidation
  (testing "consolidate"
    (is (= ((consolidate
             [(delete 3) (insert "abc") (cursor 0)])
            (initial-text "hello"))
           ((cursor 0) (initial-text "hello")))))

  (testing "perform"
    (is (= (perform
            [(delete 3) (insert "abc") (cursor 0)]
            (initial-text "hello"))
           ((cursor 0) (initial-text "hello"))))))

(deftest command-processing
  (testing "command validity"
    (is (= (valid-command? "+thisisastring") "+") "Insert string.")
    (is (= (valid-command? "-3") "-") "Backspace.")
    (is (= (valid-command? "<3") "<") "Left.")
    (is (= (valid-command? ">3") ">") "Right.")
    (is (= (valid-command? "iboop") "i") "Init.")
    (is (= (valid-command? "c0") "c") "Cursor.")
    (is (= (valid-command? "toop") "t") "Title.")
    (is (= (valid-command? "d7") "d") "Forward delete.")
    (testing "default arguments"
      (is (= (valid-command? "-") "-") "Backspace.")
      (is (= (valid-command? "<") "<") "Left.")
      (is (= (valid-command? ">") ">") "Right.")
      (is (= (valid-command? "d") "d") "Forward delete.")))

  (testing "command processing"
    (let [source-map ((cursor 4) (initial-text "hello there"))]
      (is (= (process-text-command "+thisisastring" source-map)
             ((insert "thisisastring") source-map))
          "Insert string.")
      (is (= (process-text-command "-3" source-map)
             ((delete 3) source-map)) "Backspace.")
      (is (= (process-text-command "<3" source-map)
             ((cursor-left 3) source-map)) "Left.")
      (is (= (process-text-command ">3" source-map)
             ((cursor-right 3) source-map)) "Right.")
      (is (= (process-text-command "c0" source-map)
             ((cursor 0) source-map)) "Cursor.")
      (is (= (process-text-command "toop" source-map)
             ((titled "oop") source-map)) "Title.")
      (is (= (process-text-command "d3" source-map)
             ((forward-delete 3) source-map)) "Forward delete.")
      (testing "default arguments"
        (is (= (process-text-command "-" source-map)
               ((delete 1) source-map)) "Backspace.")
        (is (= (process-text-command "<" source-map)
               ((cursor-left 1) source-map)) "Left.")
        (is (= (process-text-command ">" source-map)
               ((cursor-right 1) source-map)) "Right.")
        (is (= (process-text-command "d" source-map)
               ((forward-delete 1) source-map)) "Forward delete.")))))
