(ns text-stream.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [text-stream.edits :as edits]
            [aleph.http :as http]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as bus]
            [clojure.core.async :as async]))

(def invalid-response
  {:status 400
   :headers {"Content-Type" "text/plain"}
   :body "<h1>400</h1> This isn't what I wanted!"})

;;; The following is *very bad* *placeholder* code, to be replaced with an
;;; actual database ASAP.
(def streams (ref []))

(defn find-stream [stream-id]
  "Returns the stream with `stream-id`."
  (and
   (< stream-id (count @streams))
   (nth @streams stream-id)))

(defn add-stream [text]
  "Adds a stream with the `text` and returns its `stream-id`."
  (dosync
   (let [strid (count @streams)]
     (alter streams conj (assoc (edits/initial-text text) :id strid))
     strid)))

(def streamrooms (bus/event-bus))

(defn read-stream-handler
  "Subscribe to a stream via websockets."
  [stream-id request]
  (d/let-flow [conn (d/catch
                        (http/websocket-connection request)
                        (fn [_] nil))]
              (if-not conn
                ;; You didn't connect over ws://
                invalid-response

                (d/let-flow [ready (s/take! conn)]
                            (if (= ready "go")
                              ;; First message must be "go"
                              (let [s (find-stream stream-id)]
                                ;; Send existing nonsense
                                (s/put! conn
                                        (str "insert:"
                                             (:text s)))

                                (s/put! conn
                                        (str "cursor:"
                                             (:pos s)))

                                ;; Feed messages to clients
                                (s/connect
                                 (bus/subscribe streamrooms stream-id)
                                 conn))
                              invalid-response)))))

(defn new-stream-handler [request]
  (d/let-flow [conn (d/catch
                        (http/websocket-connection request)
                        (fn [_] nil))]
              (if-not conn
                ;; You didn't connect over ws://
                invalid-response

                ;; First message should be of the form "inited:<INITIAL TEXT>"
                (d/let-flow
                 [text (s/take! conn)]
                 (if (and (>= (count text) 7)
                          (= (subs text 0 7) "inited:"))
                   (let [init-text (subs text 7)
                         sid (add-stream init-text)]
                     (s/consume
                      (fn [msg]
                        (let [source-map (find-stream sid)]
                          (when-let [new-map
                                     (edits/process-text-command msg source-map)]
                            (bus/publish! streamrooms sid msg)
                            (dosync
                             (alter streams assoc sid new-map)))))
                      conn)))))))

(defroutes app-routes
  (context "/api" []
           ;; api routes
           (GET "/s/:stream-id" [stream-id :as request]
                (try
                  (let [sid (Integer/parseInt stream-id)]
                    (if (find-stream sid)
                      (read-stream-handler sid request)
                      invalid-response))
                  (catch NumberFormatException _ invalid-response)))
           (GET "/new" req (new-stream-handler req)))
  (GET "/" [] "Home sweet home")
  (route/not-found "Not Found"))

(defn -main [& args]
  (let [port (Integer/parseInt (or (first args) "8080"))]
    (println "Starting server on port" port)
    (http/start-server app-routes {:port port})))
