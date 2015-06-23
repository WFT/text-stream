(ns text-stream.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [text-stream.handler :refer :all]
            [aleph.http :as http]
            [manifold.stream :as s]))

(deftest web-routes
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest streams
  (let [port "8675"
        serv (-main port)]
    (try
      (testing "adding stream"
        (let [wsock @(http/websocket-client
                      (str "ws://localhost:" port "/api/new"))]
          (s/put! wsock (str "i" "abc123"))
          (let [msg @(s/take! wsock)
                sid (subs msg 7)]
            (is (= (subs msg 0 7) "cnnect:"))
            (s/put! wsock "<")
            (s/put! wsock "-")
            (s/put! wsock ">")
            (s/put! wsock "+hello")
            ;; Content should now be "abc13hello"
            (testing "viewing stream"
              (let [vsock @(http/websocket-client
                            (str "ws://localhost:" port "/api/s/" sid))
                    contentmsg @(s/take! vsock)
                    cursmsg @(s/take! vsock)]
                (is (= contentmsg "+abc13hello"))
                (is (= cursmsg "c10"))

                (s/put! wsock "+ another test")
                (is (= @(s/take! vsock) "+ another test"))
                (.close vsock))))
          (.close wsock)))
      ;; make sure the server closes
      (finally (.close serv)))))
