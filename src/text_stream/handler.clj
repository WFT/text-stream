(ns text-stream.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer
             [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer
             [wrap-resource]]
            [text-stream.edits :as edits]
            [text-stream.templates :as templates]
            [text-stream.data :as db]
            [aleph.http :as http]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as bus]))


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
                                (s/put! conn "First message must be 'go'.")
                                (s/close! conn)
                                (templates/invalid-response
                                 "First message must be 'go'."))
                              
                              (let [s (db/find-stream stream-id)]
                                ;; Send existing nonsense
                                (s/put! conn
                                        (str "+"
                                             (:content s)))

                                (s/put! conn
                                        (str "c"
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

                ;; First message should be of the form "i<INITIAL TEXT>"
                (d/let-flow
                 [text (s/take! conn)]
                 (if (and (>= (count text) edits/cmd-len)
                          (= (subs text 0 edits/cmd-len) "i"))
                   (let [init-text (subs text edits/cmd-len)
                         sid (:id (db/add-stream init-text))]
                     
                     ;; Respond with `sid`
                     (s/put! conn (str "cnnect:" sid))

                     (s/consume
                      (fn [msg]
                        (let [source-map (db/find-stream sid)]
                          (when-let [new-map
                                     (edits/process-text-command msg source-map)]
                            (db/update-stream new-map)
                            (bus/publish! streamrooms sid msg))
                          (templates/response-default "PUBLISHED")))
                      conn)
                     
                     (s/on-closed
                      conn
                      (fn []
                        (db/update-stream
                         (assoc (db/find-stream sid) :closed true))

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

(defmacro if-let-long
  "Lets `bindings`, attempting to convert each string value to an Int,
  followed by executing `then`.
  If any of these conversions fails, executes `else`."
  ([bindings then]
   `(if-let-long ~bindings ~then nil))
  ([bindings then else]
   `(try
      (let ~(apply vector (map-indexed (fn [i x] (if (odd? i)
                                     `(Long/parseLong ~x)
                                     x)) bindings))
        ~then)
      (catch NumberFormatException e#
        ~else))))

(defmacro stream-id-route 
  "Creates GET route `route`, binding `request` to the request and
  `sid` to a valid stream-id, otherwise returning a 404 with an appropriate
  message."
  [route sid request & body]
  (let [bad-response#
        (templates/invalid-response
         "This stream doesn't exist!")]
    `(GET ~route [~'sid :as ~'request]
          (if-let-long [~'sid ~'sid]
                      (if (db/find-stream ~'sid)
                        (do ~@body)
                        ~bad-response#)
                      ~bad-response#))))

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
    (:content (db/find-stream sid))
    {:content "text/plain"}))

  (stream-id-route
   "/s/:sid" sid request
   (templates/response-default
    (templates/view-stream sid (:title (db/find-stream sid)))))

  (GET "/p" {{p :p} :params}
       (if p
         (templates/response-default (str p))
         (templates/response-default "nope")))
  (GET "/new" [] (templates/new-stream))
  (GET "/" {{p :p} :params}
       (templates/response-default
        (if-let-long [page p] 
         (templates/home page)
         (templates/home 0))))
  (route/not-found "Not Found"))

(def app
  (-> #'app-routes
      (wrap-defaults site-defaults)
      (wrap-resource "/css")
      (wrap-resource "/js")))

(defn -main [& args]
  (let [port (Integer/parseInt (or (first args) "8080"))]
    (println "Starting server on port" port)
    (http/start-server
     #'app
     {:port port})))
