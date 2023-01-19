(defproject com.github.strojure/ring-undertow "1.0.53-SNAPSHOT"
  :description "Clojure ring adapter to Undertow web server."
  :url "https://github.com/strojure/ring-undertow"
  :license {:name "The MIT License" :url "http://opensource.org/licenses/MIT"}

  :dependencies [;; Undertow server API.
                 [com.github.strojure/undertow "1.0.72-rc2"]
                 ;; Lazy map for ring request.
                 [com.github.strojure/zmap "1.2.11"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.11.1"]]}
             :dev,,,,, {:dependencies [;; Competitor libraries
                                       [luminus/ring-undertow-adapter "1.3.0"]
                                       [org.immutant/web "2.1.10"]
                                       ;; Testing HTTP requests
                                       [clj-http "3.12.3"]]
                        :source-paths ["doc"]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo" :sign-releases false}]])
