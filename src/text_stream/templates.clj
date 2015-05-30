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
  [sid]
  (page/html5
   [:head
    (javascript-tag (str "var sid = " sid ";"))
    (css-resource "view-stream.css")
    [:title (str "Viewing Stream " sid)]]
   [:body
    [:h1 "Viewing Stream..."]
    [:pre#stream
     [:span.cursor "|"]]
    (js-resource "view-stream.js")]))

(defn new-stream []
  (page/html5
   [:head
    (css-resource "view-stream.css")
    [:title "Streaming..."]]
   [:body
    [:a#share]
    [:br]
    [:small#status "connecting..."]
    [:pre#stream
     [:span.cursor ""]]
    (js-resource "make-stream.js")]))

(def home
  (page/html5
   [:head [:title "text-stream"]]
   [:body
    [:h1 "text-stream"]]))

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
