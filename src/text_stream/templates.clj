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

(defn resource-text
  [directory]
  (comp
   slurp
   io/file
   io/resource
   (partial str directory "/")))

(def js-resource (comp javascript-tag (resource-text "js")))
(defn css-resource
  [css]
  [:style
   ((resource-text "css") css)])

(defn view-stream
  [sid title]
  (page/html5
   [:head
    (javascript-tag (str "var sid = " sid ";"))
    (css-resource "view-stream.css")
    [:title (str "Viewing " title)]]
   [:body
    [:h1 title]
    [:hr]
    [:pre#stream
     [:span.cursor "|"]]
    (js-resource "utils.js")
    (js-resource "view-stream.js")]))

(defn new-stream []
  (page/html5
   [:head
    (css-resource "view-stream.css")
    [:title "Streaming..."]]
   [:body
    [:a#share]
    [:pre#stream
     [:span.cursor ""]]
    (js-resource "utils.js")
    (js-resource "make-stream.js")]))

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

(extend-protocol Renderable
  java.lang.Boolean
  (render [b _] {:status 200
                 :body (str b)}))
