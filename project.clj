(defproject overseer "1.0.0-SNAPSHOT"
  :description "Demo Clojure web app"
  :url "http://overseer.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [org.clojure/tools.nrepl "0.2.7"]
                 [org.clojure/data.json "0.2.5"]
                 [com.cemerick/friend "0.2.1" :exclusions [xerces/xercesImpl]]
                 [org.clojure/tools.trace "0.7.8"]
                 [com.google.api-client/google-api-client "1.20.0"]
                 [com.google.api-client/google-api-client-jackson2 "1.20.0"]
                 [environ "1.0.0"]
                 [ring/ring-json "0.3.1"]
                 [jdbc-ring-session "0.2"]
                 [sonian/carica "1.1.0" :exclusions [[cheshire]]]
                 [clj-time "0.8.0"]
                 [heroku-database-url-to-jdbc "0.2.2"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [prismatic/schema "0.4.2"]
                 [postgresql "9.1-901.jdbc4"]
                 [org.clojure/tools.trace "0.7.8"]
                 [com.ashafa/clutch "0.4.0"]]
  :min-lein-version "2.0.0"
  :plugins [[cider/cider-nrepl "0.10.0-SNAPSHOT"]
            [lein-ring "0.7.0"]
            [environ/environ.lein "0.2.1"]]
  :test-selectors {:default (or (complement :integration)
                                (complement :performance))
                   :integration :integration
                   :all (constantly true)}
  :hooks [environ.leiningen.hooks]
  ;; :main overseer.web
  :uberjar-name "overseer-standalone.jar"
  :profiles {:test {:dependencies [[clj-webdriver "0.6.1"]]}
  	     :debug { :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"]}
             :production {:env {:production true}}
             :uberjar {:main overseer.web :aot :all}})
