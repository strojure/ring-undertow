(defproject com.github.strojure/undertow-ring "1.0.0-SNAPSHOT"
  :description "Clojure ring adapter to Undertow web server."
  :url "https://github.com/strojure/undertow-ring"
  :license {:name "The MIT License" :url "http://opensource.org/licenses/MIT"}

  :dependencies [;; Undertow server API.
                 [com.github.strojure/undertow "1.0.18-beta2"]
                 ;; Lazy map for ring request.
                 [com.github.strojure/zmap "1.0.2"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.11.1"]]}
             :dev,,,,, {:dependencies [;; Competitor libraries
                                       [luminus/ring-undertow-adapter "1.3.0"]
                                       [org.immutant/web "2.1.10"]]
                        :source-paths ["doc"]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo" :sign-releases false}]])
