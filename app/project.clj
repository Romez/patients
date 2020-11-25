(defproject app "0.1.0-SNAPSHOT"
  :description "patients app"
  :url "https://github.com/Romez/patients"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"]
                 [reagent "0.10.0"]
                 [ring "1.8.2"]
                 [compojure "1.6.2"]
                 [hiccup "1.0.5"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.20"]
            [lein-ring "0.12.5"]]

  :clean-targets ^{:protect false}

  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :resource-paths ["resources" "target/cljsbuild"]

  :ring {:handler server.core/run}

  :figwheel {:http-server-root "."
             :nrepl-port       7002
             :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
             :css-dirs         ["resources/public/css"]
             :ring-handler     server.core/run}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src" "env/dev/cljs"]
                :watch-paths ["src/app"]
                :compiler {:main          "app.dev"
                           :output-to     "resources/public/js/app.js"
                           :output-dir    "resources/public/js/out"
                           :asset-path    "js/out"
                           :source-map    true
                           :optimizations :none
                           :pretty-print  true}
                :figwheel {:on-jsload "app.core/mount-root"}}
               :release {:source-paths ["src" "env/prod/cljs"]
                         :compiler     {:output-to     "target/cljsbuild/public/js/app.js"
                                        :output-dir    "target/cljsbuild/public/js/out"
                                        :optimizations :advanced
                                        :infer-externs true
                                        :pretty-print  false}}}}

  :aliases {"package" ["do" "clean" ["cljsbuild" "once" "release"]]}

  :profiles {:dev {:source-paths ["src" "env/dev/clj"]
                   :dependencies [[binaryage/devtools "1.0.2"]
                                  [figwheel-sidecar "0.5.20"]
                                  [nrepl "0.7.0"]
                                  [cider/piggieback "0.5.0"]]}
             :uberjar {:source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "release"]]
                       :env {:production true}
                       :omit-source true
                       :aot :all}})
