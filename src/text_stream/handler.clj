(ns text-stream.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [text-stream.edits :as edits]))

(defn read-stream-handler [refid])

(defn new-stream-handler [request])

(defroutes app-routes
  (context "/api" []
           ;; api routes
           (GET "/s/:ref" [ref] (read-stream-handler ref))
           (POST "/new" req (new-stream-handler req)))
  (GET "/" [] "Hello World")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
