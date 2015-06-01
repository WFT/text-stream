(ns text-stream.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [text-stream.edits :as edits]
            [text-stream.templates :as templates]
            [aleph.http :as http]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as bus]))

;;; The following is *very bad* *placeholder* code, to be replaced with an
;;; actual database ASAP.
(def streams (ref []))

(defn find-stream [stream-id]
  "Returns the stream with `stream-id`."
  (and
   (< stream-id (count @streams))
   (let [stream (nth @streams stream-id)]
     (and (not (:private stream))
          stream))))

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
                (templates/invalid-response
                 "Expected a WebSocket connection.")

                (d/let-flow [ready (s/take! conn)]
                            ;; First message must be "go"
                            ;; (trim is to allow debug via wssh)
                            (if-not (= (clojure.string/trim ready) "go")
                              (do
                                (s/close! conn)
                                (templates/invalid-response
                                 "First message must be 'go'."))
                              
                              (let [s (find-stream stream-id)]
                                ;; Send existing nonsense
                                (s/put! conn
                                        (str "insert:"
                                             (:text s)))

                                (s/put! conn
                                        (str "cursor:"
                                             (:pos s)))

                                (if (:closed s)
                                  ;; Make sure we're not closed...
                                  (s/close! conn)
                                  
                                  ;; Feed messages to clients
                                  (s/connect
                                   (bus/subscribe streamrooms stream-id)
                                   conn))))))))

(defn new-stream-handler [request]
  (d/let-flow [conn (d/catch
                        (http/websocket-connection request)
                        (fn [_] nil))]
              (if-not conn
                ;; You didn't connect over ws://
                (templates/invalid-response
                 "Expected a WebSocket connection.")

                ;; First message should be of the form "inited:<INITIAL TEXT>"
                (d/let-flow
                 [text (s/take! conn)]
                 (if (and (>= (count text) 7)
                          (= (subs text 0 7) "inited:"))
                   (let [init-text (subs text 7)
                         sid (add-stream init-text)]
                     
                     ;; Respond with `sid`
                     (s/put! conn (str "cnnect:" sid))

                     (s/consume
                      (fn [msg]
                        (let [source-map (find-stream sid)]
                          (when-let [new-map
                                     (edits/process-text-command msg source-map)]
                            (bus/publish! streamrooms sid msg)
                            (dosync
                             (alter streams assoc sid new-map)))
                          (templates/response-default "PUBLISHED")))
                      conn)
                     
                     (s/on-closed
                      conn
                      (fn []
                        (dosync
                         (alter streams assoc sid
                                (assoc (find-stream sid) :closed true)))
                        (doall
                         (map #(.close %)
                              ;; You don't have to go home, but you
                              ;; can't stay here...
                              (bus/downstream streamrooms sid)))
                        (templates/response-default "CLOSED")))

                     (templates/response-default
                      "CONNECTED"))
                   
                   (templates/invalid-response
                    "Request does not conform to text-stream protocol."))))))

(defmacro stream-id-route [route sid request & body]
  `(GET ~route [~'sid :as ~'request]
        (try
          (let [~'sid (Integer/parseInt ~'sid)]
            (if (find-stream ~'sid)
              (do ~@body)
              (templates/invalid-response
               "This stream doesn't exist!")))
          (catch NumberFormatException e#
            (templates/invalid-response
               "This stream doesn't exist!")))))

(defroutes app-routes
  (context "/api" []
           ;; api routes
           (stream-id-route
            "/s/:sid" sid request
            (read-stream-handler sid request))
           (GET "/new" req (new-stream-handler req)))
  (stream-id-route
   "/d/s/:sid" sid request
   (templates/response-default
    (:text (find-stream sid))
    {:content "text/plain"}))

  (stream-id-route
   "/s/:sid" sid request
   (templates/response-default
    (templates/view-stream sid)))
  
  (GET "/new" [] (templates/new-stream))
  (GET "/" {{page :p} :params} (templates/home @streams (or page 0)))
  (route/not-found "Not Found"))

(defn -main [& args]
  (let [port (Integer/parseInt (or (first args) "8080"))]
    (println "Starting server on port" port)
    (http/start-server #'app-routes {:port port})))
