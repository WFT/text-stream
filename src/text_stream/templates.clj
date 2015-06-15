(ns text-stream.templates
  (:require [hiccup.core :refer :all]
            [hiccup.page :as page]
            [hiccup.element :refer :all]
            [clojure.java.io :as io]
            [compojure.response :refer [Renderable render]]))

(defn response-default
  ([body] (response-default body {}))
  ([body options]
   (merge {:headers {"Content-Type" "text/html"}
           :status 200
           :body body} options)))

(defn view-stream
  [sid title]
  (page/html5
   [:head
    (javascript-tag (str "var sid = " sid ";"))
    (page/include-css "/view-stream.css" "/main.css")
    [:title (str "Viewing " title)]]
   [:body
    [:div#controls
     [:ul
      [:li [:h1#title title]]]]
    [:pre#stream.content
     [:span.cursor " "]]
    (page/include-js "/utils.js" "/view-stream.js")]))

(defn new-stream []
  (page/html5
   [:head
    (page/include-css "/view-stream.css" "/main.css")
    [:title "Streaming..."]]
   [:body
    [:div#controls
     [:ul
      [:li [:a#share.left]]
      [:li [:h1#title "Untitled Stream"]]
      [:li [:a#titleset.right {:href "#"} "Set Title"]]]]
    [:pre#stream.content
     [:span.cursor "Type Here"]]
    (page/include-js "/utils.js" "/make-stream.js")]))

(def page-count 15)

(defn home [streams page]
  (page/html5
   [:head
    (page/include-css "/main.css")
    [:title "text-stream"]]
   [:body
    [:div#controls
     [:ul
      [:li [:h1#title "text-stream"]]
      [:li [:a.right {:href "/new"} "New Stream"]]]]
    [:div.content
     [:h2 "Streams"]
     [:ul
      (for [x (take page-count (drop (* page-count page) streams))]
        [:li [:a {:href (str "/s/" (:id x))} (:title x)]])]
     (when (> page 0)
       [:a#prev {:href (str "/?p=" (dec page))} "Previous "])
     (when (and (not (<= (count streams) page-count))
                (< (* page-count page) (/ (dec (count streams)) page-count)))
       [:a#next {:href (str "/?p=" (inc page))} "Next"])]]))

(defn invalid-response
  [reason]
  (response-default
   (page/html5
          [:body
           [:h1 "400"]
           "Oh no! This isn't what I expected... "
           reason])
   {:status 400}))

;;; FIXME: This makes the whole thing work! Without it there are weird
;;; errors regarding some handlers returning a Boolean. Usually when
;;; a new viewer connects to the stream.
(extend-protocol Renderable
  java.lang.Boolean
  (render [b _] {:status 200
                 :body (str b)}))
