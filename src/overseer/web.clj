(ns overseer.web
  (:import [java.util Arrays]
           [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.client.googleapis.auth.oauth2
            GoogleIdTokenVerifier
            GoogleIdTokenVerifier$Builder
            GoogleIdToken
            GoogleIdToken$Payload
            ]
           )
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [compojure.handler :refer [site]]
            [carica.core :as c]
            [compojure.route :as route]
            [compojure.coercions :refer [as-int]]
            [clojure.java.io :as io]
            [clojure.tools.nrepl.server :as nrepl-server]
            [jdbc-ring-session.core :refer [jdbc-store]]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [clojure.tools.trace :as trace]
            [overseer.db :as db]
            [clojure.pprint :as pp]
            [overseer.database :as data]
            [overseer.dates :as dates]
            [overseer.attendance :as att]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [environ.core :refer [env]])
  (:gen-class))

(def transport (GoogleNetHttpTransport/newTrustedTransport))
(def jsonFactory (JacksonFactory/getDefaultInstance))

(defn verifyer []
  (let [CLIENT_ID "test"]
    (doto (new GoogleIdTokenVerifier$Builder transport jsonFactory)
      (.setAudience (Arrays/asList CLIENT_ID))
      (.build))))

(defn verify [idTokenString]
  (let [APP_DOMAIN_NAME ""]
    (if-let [idToken (.verify (verifyer) idTokenString)]
      (let [payload (.getPayload idToken)]
        (if (= (.getHostedDomain payload) APP_DOMAIN_NAME)
          (println (str "User ID: " (.getSubject payload)))
          (println "Invalid ID token")))
      (println "Invalid ID token"))))

(def users {"admin" {:username "admin"
                     :password (creds/hash-bcrypt (env :admin))
                     :roles #{::admin ::user}}
            "super" {:username "super"
                     :password (creds/hash-bcrypt (env :admin))
                     :roles #{::admin ::user ::super}}
            "user" {:username "user"
                    :password (creds/hash-bcrypt (env :userpass))
                    :roles #{::user}}})
(defn year-resp []
  (let [years (data/get-years)]
    (resp/response {:years (map :name years)
                    :current_year (dates/get-current-year-string years)})))

(defn student-page-response [student-id]
  (resp/response {:student (first (att/get-student-with-att student-id))}))

(defroutes app
  (GET "/" [] (friend/authenticated (io/resource "index.html")))
  (GET "/resetdb" []
       (friend/authorize #{::super} ;; (db/reset-db)
                         (resp/redirect "/")))

  (GET "/dates/today" [] (dates/today-string))
  (GET "/sampledb" []
       (friend/authorize #{::super} ;; (data/sample-db true)
                         (resp/redirect "/")))

  (GET "/students" []
    (friend/authorize #{::user}
                      (resp/response (att/get-student-list))))

  (GET "/students/:id" [id :<< as-int]
     (friend/authorize #{::user} (student-page-response id)))

  (POST "/students" [name]
    (friend/authorize #{::admin}
                      (let [made? (data/make-student name)]
                        (resp/response {:made made?
                                        :students (att/get-student-list)}))))

  (PUT "/students/:id" [id :<< as-int name]
        (friend/authorize #{::admin}
                          (data/rename id name))
        (student-page-response id))

  (POST "/students/:id/togglehours" [id :<< as-int]
    (friend/authorize #{::admin}
                      (do (data/toggle-student-older id)
                          (student-page-response id))))

  (POST "/students/:id/absent" [id :<< as-int]
    (friend/authorize #{::admin}
                      (do (data/toggle-student-absent id)
                          (student-page-response id))))

  (POST "/students/:id/excuse" [id :<< as-int day]
        (friend/authorize #{::admin}
                          (data/excuse-date id day))
        (student-page-response id))

  (POST "/students/:id/override" [id :<< as-int  day]
        (friend/authorize #{::admin}
                          (data/override-date id day))
        (student-page-response id))

  (POST "/students/:id/swipe/delete" [id :<< as-int  swipe]
        (friend/authorize #{::admin}
                          (data/delete-swipe swipe)
                          (student-page-response id)))

  (POST "/students/:id/swipe" [id :<< as-int direction  missing]
        (friend/authorize #{::user}
                          (if (= direction "in")
                            (do (when missing (data/swipe-out id missing))
                                (data/swipe-in id))
                            (do (when missing (data/swipe-in id missing))
                                (data/swipe-out id))))
        (resp/response (att/get-student-list)))


  (GET "/reports/years" []
       (friend/authorize #{::user} (year-resp)))

  (DELETE "/reports/years/:year" [year]
        (friend/authorize #{::admin}
                          (data/delete-year year)
                          (year-resp)))
  (POST "/reports/years" [from_date to_date]
        (friend/authorize #{::admin}
                          (let [made? (data/make-year from_date to_date)]
                            (resp/response {:made made?}))))

  (GET "/reports/:year" [year]
        (friend/authorize #{::admin}
                          (resp/response (db/get-report year))))

  (GET "/users/login" req
       (io/resource "login.html"))
  (GET "/users/logout" req
       (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/users/is-user" req
       (friend/authorize #{::user} "You're a user!"))
  (GET "/users/is-admin" req
        (friend/authorize #{::admin} (resp/response {:admin true})))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn tapp []
  (-> #'app
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn users)
                            :allow-anon? true
                            :login-uri "/users/login"
                            :default-landing-uri "/"
                            :workflows [(workflows/interactive-form)]})
      (wrap-session {:store (jdbc-store @db/pgdb)
                     :cookie-attrs {:max-age (* 3 365 24 3600)}})
      wrap-keyword-params
      wrap-json-body wrap-json-params wrap-json-response ))

(defn -main [& [port]]
  (db/init-pg)
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site (tapp)) {:port port :join? false})))

