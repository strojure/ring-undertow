(defproject com.github.strojure/ring-undertow "1.0.100-SNAPSHOT"
  :description "Clojure ring adapter to Undertow web server."
  :url "https://github.com/strojure/ring-undertow"
  :license {:name "The Unlicense" :url "https://unlicense.org"}

  :dependencies [;; Undertow server API.
                 [com.github.strojure/undertow "1.3.0-116"]
                 ;; Lazy map for ring request.
                 [com.github.strojure/zmap "1.3.26"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.11.1"]]}
             :dev,,,,, {:dependencies [;; Competitor libraries
                                       [luminus/ring-undertow-adapter "1.3.0"]
                                       [org.immutant/web "2.1.10"]
                                       ;; Testing HTTP requests
                                       [java-http-clj "0.4.3"]]
                        :source-paths ["doc"]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo" :sign-releases false}]])
