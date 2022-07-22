(ns clojure-bank.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor.error :as error-int]
            [ring.util.response :as ring-resp]
            [schema.core :as s]
            [clojure-bank.diplomat.http-in.account :as http-in.account]))

(defn about-page
  [_]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(s/set-fn-validation! true)

(defn home-page
  [_]
  (ring-resp/response "Hello World! XXX"))

(comment
  (defn reply-hello [request]
    {:status 200 :body (str "Hello, " (get-in request [:params :name]))}))

(def hello-world
  {:status 200
   :body   {:message "hi lorena"}})

(comment
  (def attach-guid
    {:name ::attach-guid
     :enter (fn [context] (assoc context ::guid (java.util.UUID/randomUUID)))}))

(def fist-interceptor
  {:name ::first-interceptor
   :enter (fn [ctx] (println "entering my first interceptor")
            ctx)
   :leave (fn [ctx] (println "leaving my first interceptor")
            ctx)})

;(def CommentRequest
;  {(s/optional-key :parent-comment-id) long
;   :text String
;   :share-services [(s/enum :twitter :facebook :google)]})
;
;(def parse-comment-request
;  (coerce/coercer CommentRequest coerce/json-coercion-matcher))
;
;(= (parse-comment-request
;     {:parent-comment-id (int 2128123123)
;      :text "This is awesome!"
;      :share-services ["twitter" "facebook"]})
;   {:parent-comment-id 2128123123
;    :text "This is awesome!"
;    :share-services [:twitter :facebook]})

(def error-interceptor
  {:name ::error-handler
   :error (fn [ctx ex-info]
            (println "cagou foi tudo")
            (println ex-info)
            (assoc ctx :response {:status 400 :body {:message "Another bad one"}}))})

(def error-interceptor
  (error-int/error-dispatch [ctx ex]
                            [{:exception-type :clojure.lang.ExceptionInfo}]
                            (assoc ctx :response {:status 400 :body ((ex-message ex) "Another bad one")})
                            :else
                            (assoc ctx :response {:status 401 :body {:message (ex-message ex)}})))

;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def common-interceptors [(body-params/body-params) http/json-body fist-interceptor error-interceptor ])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/about" :get (conj common-interceptors `about-page)]
              ["/hello-world" :get (conj common-interceptors (fn [_]
                                                               hello-world))
               :route-name :get-hello-world]
              ["/account/:account-id" :get (conj common-interceptors
                                                 http-in.account/get-account) :route-name :get-account]
              ["/account" :post (conj common-interceptors
                                      http-in.account/create-account!) :route-name :create-account]})

;; Consumed by clojure-bank.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env                     :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes            routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Tune the Secure Headers
              ;; and specifically the Content Security Policy appropriate to your service/application
              ;; For more information, see: https://content-security-policy.com/
              ;;   See also: https://github.com/pedestal/pedestal/issues/499
              ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
              ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
              ;;                                                          :frame-ancestors "'none'"}}

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path     "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
              ::http/type              :jetty
              ;;::http/host "localhost"
              ::http/port              8080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2?  false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false
                                        ;; Alternatively, You can specify you're own Jetty HTTPConfiguration
                                        ;; via the `:io.pedestal.http.jetty/http-configuration` container option.
                                        ;:io.pedestal.http.jetty/http-configuration (org.eclipse.jetty.server.HttpConfiguration.)
                                        }})
