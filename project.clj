(defproject text-ad "0.1.0-SNAPSHOT"
 g:description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2277"]
                 [org.clojure/google-closure-library "0.0-20130212-95c19e7f0f5f"]
                 [figwheel "0.1.3-SNAPSHOT"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [sablono "0.2.19"]
                 [om "0.6.5"]]
  
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-figwheel "0.1.3-SNAPSHOT"]]

  :cljsbuild {
              :builds [{:source-paths ["src"]
                        :compiler {:output-to "resources/public/js/compiled/text_ad.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map true
                                   :optimizations :none }}]}
  :figwheel {
             :http-server-root "public" ;; default and assumes "resources" 
             :server-port 3449 ;; default
             :css-dirs ["resources/public/css"] ;; watch and update CSS
             })
