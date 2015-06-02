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
    (page/include-css "/view-stream.css")
    [:title (str "Viewing " title)]]
   [:body
    [:h1 title]
    [:hr]
    [:pre#stream
     [:span.cursor "|"]]
    (page/include-js "/utils.js" "/view-stream.js")]))

(defn new-stream []
  (page/html5
   [:head
    (page/include-css "/view-stream.css")
    [:title "Streaming..."]]
   [:body
    [:div#controls
     [:a#share]
     [:h1#title]
     [:a#set-title {:href "#"} "Set Title"]]
    [:pre#stream
     [:span.cursor "Type Here"]]
    (page/include-js "/utils.js" "/make-stream.js")]))

(def page-count 15)

(defn home [streams page]
  (page/html5
   [:head [:title "text-stream"]]
   [:body
    [:h1 "text-stream"]
    [:a {:href "/new"} "New Stream"]
    [:h2 "Streams"]
    [:ul
     (for [x (take page-count (drop (* page-count page) streams))]
       [:li [:a {:href (str "/s/" (:id x))} (:title x)]])]
    (when (> page 0)
      [:a#prev {:href (str "/?p=" (dec page))} "Previous "])
    (when (and (not (<= (count streams) page-count))
           (< (* page-count page) (/ (dec (count streams)) page-count)))
      [:a#next {:href (str "/?p=" (inc page))} "Next"])]))

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
