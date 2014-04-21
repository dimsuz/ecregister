(defproject ecregister "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [http-kit "2.1.12"]
                 [seesaw "1.4.4"]
                 [enlive "1.1.5"]
                 [reagi "0.7.0"]
                 [cheshire "5.3.1"]
                 [clj-time "0.7.0"]]
  :main ^:skip-aot ecregister.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.0"]]
                   :plugins [[lein-midje "3.1.1"]]}})
